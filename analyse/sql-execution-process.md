# 两条 SQL 详细执行过程

## 说明

本文描述的是两条 SQL 的**逻辑执行过程**，目的是帮助理解它们是如何一步一步得到结果的。

需要特别说明：

- 这里说的“执行步骤”是**语义层面的理解顺序**
- 不一定等同于数据库优化器最终采用的**物理执行计划**
- 实际数据库可能会重排连接顺序、改写子查询、选择不同索引

但从理解 SQL 逻辑的角度，这样拆解最清晰。

## 分析前提

第二条 SQL 按你确认后的修正版理解，即：

```sql
select vt.appvid as appvid, vt.appid as appid, ...
from store_app_version_t as vt
left join store_app_t as apt on apt.appid = vt.appid
where apt.aliasname = #{alias, jdbcType = VARCHAR}
  and vt.is_beta = if(#{isBeta, jdbcType=BOOLEAN}, '1', '0')
  and vt.version_code = (
      select max(version_code)
      from store_app_version_t
      where appid = apt.appid
        and os_target = vt.os_target
        and device_type = vt.device_type
        and is_beta = if(#{isBeta, jdbcType=BOOLEAN}, '1', '0')
  )
order by vt.os_target, vt.device_type desc
```

## 第一条 SQL 执行过程

第一条 SQL 的核心特点是：

- 先在子查询里按组求最大 `version_code`
- 再把最大值结果回连到主表，取完整版本记录

原始结构可以概括为：

```sql
select vt.appvid as appvid, vt.appid as appid, ...
from store_app_version_t vt
inner join store_app_t apt on apt.appid = vt.appid
inner join (
    select v.appid, v.os_target, v.device_type, max(v.version_code) as max_version_code
    from store_app_version_t v
    inner join store_app_t a on a.appid = v.appid
    where a.aliasname = #{alias}
      and v.is_beta = if(#{isBeta}, '1', '0')
    group by v.appid, v.os_target, v.device_type
) max_ver
    on vt.appid = max_ver.appid
   and vt.os_target = max_ver.os_target
   and vt.device_type = max_ver.device_type
   and vt.version_code = max_ver.max_version_code
where apt.aliasname = #{alias}
  and vt.is_beta = if(#{isBeta}, '1', '0')
order by vt.os_target, vt.device_type desc
```

### 步骤 1：执行子查询 `max_ver`

数据库首先处理这部分：

```sql
select v.appid, v.os_target, v.device_type, max(v.version_code) as max_version_code
from store_app_version_t v
inner join store_app_t a on a.appid = v.appid
where a.aliasname = #{alias}
  and v.is_beta = if(#{isBeta}, '1', '0')
group by v.appid, v.os_target, v.device_type
```

这一段的目标是：

- 先找出指定 `aliasname`
- 且 `is_beta` 符合条件的所有版本数据
- 然后按 `(appid, os_target, device_type)` 分组
- 每组取最大的 `version_code`

### 步骤 1.1：读取 `store_app_version_t v`

先把版本表作为基础数据源读取出来。

这时每一行通常包含：

- `appid`
- `os_target`
- `device_type`
- `version_code`
- `is_beta`
- 其他版本字段

### 步骤 1.2：与 `store_app_t a` 做内连接

连接条件是：

```sql
a.appid = v.appid
```

连接后，每条版本记录都能拿到对应应用表中的信息，例如：

- `aliasname`
- 应用名称
- 其他应用属性

因为是 `inner join`，所以：

- 没有关联应用信息的版本行会被过滤掉

### 步骤 1.3：应用 `where` 过滤

过滤条件是：

```sql
a.aliasname = #{alias}
and v.is_beta = if(#{isBeta}, '1', '0')
```

这一阶段会把无关数据去掉，只保留：

- 别名等于目标 `alias`
- beta 标识等于目标 `isBeta`

的版本记录。

### 步骤 1.4：按 `(appid, os_target, device_type)` 分组

执行：

```sql
group by v.appid, v.os_target, v.device_type
```

这里的含义是把过滤后的数据拆成很多组，每组代表：

- 某一个应用 `appid`
- 某一个系统 `os_target`
- 某一个设备类型 `device_type`

例如可能形成这样的组：

| appid | os_target | device_type | 组内 version_code |
|---|---|---|---|
| 1001 | android | phone | 1, 2, 3, 5 |
| 1001 | android | tablet | 2, 4 |
| 1001 | ios | phone | 7, 8 |

### 步骤 1.5：每组计算 `max(version_code)`

执行：

```sql
max(v.version_code)
```

得到子查询结果集 `max_ver`，形如：

| appid | os_target | device_type | max_version_code |
|---|---|---|---:|
| 1001 | android | phone | 5 |
| 1001 | android | tablet | 4 |
| 1001 | ios | phone | 8 |

这一步非常关键。

到这里为止，数据库只知道：

- 每一组最大的版本号是多少

但还没有拿到完整版本记录的全部字段。

## 步骤 2：执行外层主查询的基础连接

外层主查询的前半部分是：

```sql
from store_app_version_t vt
inner join store_app_t apt on apt.appid = vt.appid
```

这一步的作用是：

- 再次读取完整版本表 `vt`
- 再关联应用表 `apt`

为什么要再读一次版本表？

因为子查询 `max_ver` 只有：

- `appid`
- `os_target`
- `device_type`
- `max_version_code`

而最终 `select vt.appvid, vt.appid, ...` 需要的是完整版本记录。

所以必须“回表”到 `store_app_version_t vt` 才能取出所有列。

## 步骤 3：把外层版本记录与 `max_ver` 做匹配

连接条件是：

```sql
vt.appid = max_ver.appid
and vt.os_target = max_ver.os_target
and vt.device_type = max_ver.device_type
and vt.version_code = max_ver.max_version_code
```

这一阶段的含义是：

- 对于外层每条完整版本记录 `vt`
- 只有当它所在组的 `version_code` 正好等于该组最大值时
- 它才会被保留下来

也就是说，这一步完成了：

- “从完整记录里筛出每组最大版本号对应的行”

例如：

外层 `vt` 有这些数据：

| appid | os_target | device_type | version_code | appvid |
|---|---|---|---:|---:|
| 1001 | android | phone | 3 | 501 |
| 1001 | android | phone | 5 | 502 |
| 1001 | android | tablet | 4 | 503 |
| 1001 | ios | phone | 8 | 504 |

而 `max_ver` 是：

| appid | os_target | device_type | max_version_code |
|---|---|---|---:|
| 1001 | android | phone | 5 |
| 1001 | android | tablet | 4 |
| 1001 | ios | phone | 8 |

匹配后保留下来的就是：

| appid | os_target | device_type | version_code | appvid |
|---|---|---|---:|---:|
| 1001 | android | phone | 5 | 502 |
| 1001 | android | tablet | 4 | 503 |
| 1001 | ios | phone | 8 | 504 |

## 步骤 4：再次应用外层 `where` 条件

外层还有一层过滤：

```sql
where apt.aliasname = #{alias}
  and vt.is_beta = if(#{isBeta}, '1', '0')
```

这层过滤和子查询里的过滤条件是重复的。

它的作用是：

- 保证外层回表出来的完整记录仍然满足相同业务条件
- 避免连接过程中出现超出目标范围的数据

从结果正确性上说，这一层是有意义的。

从写法上说，它也体现了：

- 子查询负责“求最大版本号”
- 外层负责“取完整行并再次约束结果范围”

## 步骤 5：执行 `select`

此时结果集已经只剩下目标记录，数据库再输出：

- `vt.appvid`
- `vt.appid`
- 以及你在 `...` 中列出的其他字段

## 步骤 6：执行排序

最后执行：

```sql
order by vt.os_target, vt.device_type desc
```

结果会按：

1. `os_target` 升序
2. 同一 `os_target` 下 `device_type` 降序

输出最终结果。

## 第一条 SQL 的过程总结

可以把第一条 SQL 简化理解成：

1. 先筛出目标应用、目标 beta 状态的版本数据
2. 按 `(appid, os_target, device_type)` 分组
3. 计算每组最大 `version_code`
4. 再回到版本表里找到这些最大版本号对应的完整记录
5. 输出并排序

一句话概括就是：

**先算“每组最大版本号”，再取“最大版本号对应的完整记录”。**

---

## 第二条 SQL 执行过程

第二条 SQL 的核心特点是：

- 不先单独生成一个最大值结果集
- 而是对外层每一条记录，动态判断它是不是本组最大版本号

其结构是：

```sql
select vt.appvid as appvid, vt.appid as appid, ...
from store_app_version_t as vt
left join store_app_t as apt on apt.appid = vt.appid
where apt.aliasname = #{alias}
  and vt.is_beta = if(#{isBeta}, '1', '0')
  and vt.version_code = (
      select max(version_code)
      from store_app_version_t
      where appid = apt.appid
        and os_target = vt.os_target
        and device_type = vt.device_type
        and is_beta = if(#{isBeta}, '1', '0')
  )
order by vt.os_target, vt.device_type desc
```

## 步骤 1：读取外层基础表 `store_app_version_t vt`

数据库先从版本表 `vt` 读取候选记录。

这时每一行都可能成为最终结果，但还没有筛选。

## 步骤 2：与 `store_app_t apt` 做连接

连接条件：

```sql
apt.appid = vt.appid
```

虽然这里写的是：

```sql
left join
```

但由于后面有：

```sql
where apt.aliasname = #{alias}
```

所以逻辑效果上等同于 `inner join`。

原因是：

- 如果某条 `vt` 在 `apt` 中没有匹配到行
- 那么 `apt.aliasname` 会是 `null`
- `where apt.aliasname = #{alias}` 不成立
- 该行会被过滤掉

因此这一步的实际效果是：

- 只保留能关联到应用表的版本记录

## 步骤 3：先做外层普通过滤

外层先应用两个直接条件：

```sql
apt.aliasname = #{alias}
and vt.is_beta = if(#{isBeta}, '1', '0')
```

这一步之后，外层候选记录会缩小到：

- 指定应用别名
- 指定 beta 状态

的版本记录。

例如外层候选集可能变成：

| appid | os_target | device_type | version_code | is_beta | appvid |
|---|---|---|---:|---|---:|
| 1001 | android | phone | 3 | 0 | 501 |
| 1001 | android | phone | 5 | 0 | 502 |
| 1001 | android | tablet | 4 | 0 | 503 |
| 1001 | ios | phone | 8 | 0 | 504 |

## 步骤 4：对外层每一行执行相关子查询

这是第二条 SQL 最关键的部分：

```sql
vt.version_code = (
    select max(version_code)
    from store_app_version_t
    where appid = apt.appid
      and os_target = vt.os_target
      and device_type = vt.device_type
      and is_beta = if(#{isBeta}, '1', '0')
)
```

这里的子查询引用了外层的值：

- `apt.appid`
- `vt.os_target`
- `vt.device_type`

所以它是**相关子查询**。

它不是只执行一次，而是逻辑上会随着外层当前行变化而变化。

### 步骤 4.1：以当前外层行作为参数

假设当前外层行是：

| appid | os_target | device_type | version_code | appvid |
|---|---|---|---:|---:|
| 1001 | android | phone | 3 | 501 |

那么子查询会变成逻辑上的：

```sql
select max(version_code)
from store_app_version_t
where appid = 1001
  and os_target = 'android'
  and device_type = 'phone'
  and is_beta = '0'
```

假设算出来结果是：

```sql
5
```

接着数据库比较：

```sql
当前行的 vt.version_code = 3
是否等于 5
```

结果是不等于，所以这条外层行被过滤掉。

### 步骤 4.2：继续处理下一条外层行

下一条外层行如果是：

| appid | os_target | device_type | version_code | appvid |
|---|---|---|---:|---:|
| 1001 | android | phone | 5 | 502 |

则子查询逻辑上仍然是：

```sql
select max(version_code)
from store_app_version_t
where appid = 1001
  and os_target = 'android'
  and device_type = 'phone'
  and is_beta = '0'
```

结果还是 `5`。

这次比较变成：

```sql
当前行的 vt.version_code = 5
是否等于 5
```

成立，因此这条行被保留。

### 步骤 4.3：对每一条外层候选记录重复这一判断

最终保留下来的就是：

- 那些 `version_code` 正好等于本组最大 `version_code` 的行

也就是说，第二条 SQL 是通过“逐行判定”的方式，得到与第一条 SQL 类似的结果。

## 步骤 5：执行 `select`

当某一行通过了上述相关子查询条件后，数据库就输出该行的：

- `vt.appvid`
- `vt.appid`
- 其他版本字段

## 步骤 6：执行排序

最后执行：

```sql
order by vt.os_target, vt.device_type desc
```

输出排序后的结果。

## 第二条 SQL 的过程总结

可以把第二条 SQL 理解成：

1. 先取出目标应用、目标 beta 状态下的候选版本记录
2. 对每一条候选记录
3. 再去查一次“它所在组的最大 `version_code` 是多少”
4. 如果当前行的 `version_code` 等于这个最大值，就保留
5. 最后输出并排序

一句话概括就是：

**边扫描候选记录，边判断“当前行是不是本组最大版本”。**

---

## 两条 SQL 执行过程的核心差异

### 第一条 SQL

核心路径是：

```text
先求每组最大值 -> 再回表取完整记录
```

它是“先汇总，再定位明细”的思路。

### 第二条 SQL

核心路径是：

```text
先拿候选行 -> 再逐行判断是否等于本组最大值
```

它是“先看当前行，再判断是否满足最大值条件”的思路。

## 用一句更直观的话区分

第一条像这样：

- 我先列出每个分组的最高分
- 再回去找是谁拿到了这个最高分

第二条像这样：

- 我看每一个人
- 然后问一句：你的分数是不是你这一组最高分

## 为什么两条 SQL 结果通常一致

因为它们判断“入选”的标准本质相同：

- 当前记录所属分组相同
- 当前记录的 `version_code` 等于该组最大 `version_code`

只是实现路径不同。

## 为什么执行体验可能不同

虽然语义一致，但数据库实际执行时，第二条更容易受以下因素影响：

- 是否有合适复合索引
- 优化器是否能改写相关子查询
- 数据量是否足够大
- 某些分组是否存在大量历史版本

因此从执行过程理解上：

- 第一条更偏“批量先算好”
- 第二条更偏“逐条验证”

这也就是前面优劣分析里，第一条通常更稳的根本原因。

## 最终总结

两条 SQL 的详细执行过程可以分别概括为：

### 第一条

1. 过滤出目标应用和目标 beta 数据
2. 按 `(appid, os_target, device_type)` 分组
3. 计算每组最大 `version_code`
4. 回到原表匹配这些最大值对应的完整记录
5. 输出结果并排序

### 第二条

1. 过滤出目标应用和目标 beta 数据
2. 遍历每条候选记录
3. 对当前记录执行相关子查询，计算其所在组最大 `version_code`
4. 比较当前记录是否等于该最大值
5. 保留满足条件的记录
6. 输出结果并排序

所以两者最本质的区别只有一句话：

**第一条是“先分组求最大，再回表”；第二条是“逐行判断自己是不是最大”。**
