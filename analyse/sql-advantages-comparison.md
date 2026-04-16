# 两条 SQL 优劣分析

## 分析前提

本文件基于以下前提分析：

- 第二条 SQL 已修正为 `apt.aliasname`、`vt.is_beta`
- 两条 SQL 的业务目标相同：查询指定 `aliasname`、`is_beta` 下，每个 `(appid, os_target, device_type)` 的最大 `version_code` 对应版本记录

两条 SQL 的核心写法分别是：

### 写法一：派生表分组后回表

```sql
select vt.appvid as appvid, vt.appid as appid, ...
from store_app_version_t vt
inner join store_app_t apt on apt.appid = vt.appid
inner join (
    select v.appid, v.os_target, v.device_type, max(v.version_code) as max_version_code
    from store_app_version_t v
    inner join store_app_t a on a.appid = v.appid
    where a.aliasname = #{alias, jdbcType = VARCHAR}
      and v.is_beta = if(#{isBeta, jdbcType=BOOLEAN}, '1', '0')
    group by v.appid, v.os_target, v.device_type
) max_ver on vt.appid = max_ver.appid
       and vt.os_target = max_ver.os_target
       and vt.device_type = max_ver.device_type
       and vt.version_code = max_ver.max_version_code
where apt.aliasname = #{alias, jdbcType = VARCHAR}
  and vt.is_beta = if(#{isBeta, jdbcType=BOOLEAN}, '1', '0')
order by vt.os_target, vt.device_type desc
```

### 写法二：相关子查询取最大值

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

## 结论

如果只比较“业务语义是否正确”，两者都可以。

如果比较“工程上更推荐哪一个”，**更推荐第一条 SQL**，原因是：

1. 在大多数数据库场景下，第一条的执行计划通常更稳定。
2. 第一条更容易让人一眼看出“先分组求最大值，再回表取明细”的意图。
3. 第二条属于相关子查询写法，对优化器和索引更敏感，数据量上来后性能波动风险更大。

第二条 SQL 不是不能用，但更适合：

- 数据量不大
- 查询频率不高
- 更在意 SQL 书写短一些

## 分维度分析

### 1. 可读性

**第一条更偏工程化，可读性更强。**

原因：

- 第一条明确拆成两步：
  - 先求每组最大 `version_code`
  - 再根据最大值回表取完整记录
- 这种写法和“取每组最新一条记录”的经典写法一致，维护人员更容易理解

第二条的问题是：

- 逻辑藏在相关子查询里
- 需要读者先理解外层行，再理解子查询如何对当前行逐条比较
- 对不熟悉相关子查询的人来说，理解成本更高

这一点上：

- **第一条胜出**

### 2. 执行性能

**通常第一条更有优势。**

原因：

- 第一条先把候选结果聚合成较小结果集，再和主表做等值连接
- 第二条通常需要对外层每一行去判断一次“我是不是这组里的最大 version_code”
- 如果优化器不能很好地把相关子查询改写成半连接或物化结果，第二条更容易慢

简单理解：

- 第一条更像“先算清楚，再匹配”
- 第二条更像“边扫边问”

数据量小时差异可能不明显，但数据量大、并发高时，第一条一般更稳。

这一点上：

- **第一条胜出**

### 3. 对优化器的依赖

**第二条更依赖数据库优化器能力。**

第一条的执行思路比较直接：

- 聚合
- 连接
- 过滤

第二条则更依赖数据库是否能把相关子查询优化好。

如果数据库版本较老、统计信息不准、索引不理想，第二条更容易出现：

- 重复执行子查询代价高
- 走错索引
- 扫描行数偏大

这一点上：

- **第一条更稳**

### 4. 索引利用

两条 SQL 都受索引影响，但**第二条更吃索引质量**。

对这类查询，比较关键的索引通常是 `store_app_version_t` 上能覆盖这些列的复合索引：

```sql
(appid, os_target, device_type, is_beta, version_code)
```

或者按数据库实际过滤顺序做等价调整。

第一条即使索引一般，很多时候也还能靠聚合加连接得到相对可接受的计划。

第二条如果缺少合适索引，相关子查询的代价容易快速放大。

这一点上：

- **第一条容错更高**

### 5. 结果确定性

这一点两者基本相同，都有同一个特点：

- 如果同一组 `(appid, os_target, device_type, is_beta)` 下存在多条记录，并且它们的 `version_code` 同为最大值
- 那么两条 SQL 都会返回多行

也就是说，两条 SQL 都只是表达：

- “取最大 `version_code` 的记录”

而不是：

- “强制只取唯一一条最新记录”

如果业务要求严格唯一，还需要额外排序条件或额外唯一字段参与比较，例如：

- `appvid`
- `create_time`
- `publish_time`

在这一点上：

- **两者打平**

### 6. 可维护性

**第一条通常更适合长期维护。**

原因：

- 逻辑分层清晰
- 更容易扩展
- 如果后续要增加额外分组维度或最大值规则，第一条通常更容易改

例如以后如果要改成：

- 先按 `aliasname`
- 再按 `(appid, os_target, device_type, channel)`
- 取最大 `version_code`
- 若并列再取最大 `publish_time`

第一条更容易逐层扩展。

第二条会更快变成很长的相关子查询，维护成本上升明显。

这一点上：

- **第一条胜出**

### 7. SQL 简洁性

**第二条更短，看起来更直接。**

它的优点主要是：

- SQL 行数更少
- 不需要显式写一个派生表
- 对小表、小项目、低频查询来说，写起来更快

如果当前场景是：

- 表不大
- 访问量低
- 临时查询
- 开发更在意快速表达

第二条有一定优势。

这一点上：

- **第二条胜出**

## 综合对比表

| 维度 | 第一条 SQL | 第二条 SQL |
|---|---|---|
| 业务语义 | 正确 | 正确 |
| 可读性 | 更清晰 | 中等 |
| 性能稳定性 | 更好 | 更依赖优化器 |
| 大数据量表现 | 通常更优 | 更容易波动 |
| 索引容错 | 更高 | 更低 |
| SQL 简洁性 | 较长 | 更短 |
| 可维护性 | 更好 | 一般 |
| 临时查询便利性 | 一般 | 更方便 |

## 推荐意见

### 推荐第一条 SQL 的场景

- 线上正式业务 SQL
- 数据量较大
- 查询频率较高
- 团队多人维护
- 需要更稳定执行计划

### 可以接受第二条 SQL 的场景

- 数据量不大
- 一次性查询或后台低频查询
- 对性能要求不高
- 更希望 SQL 简洁

## 最终结论

在你这个场景里，如果这是正式业务查询，**优先推荐第一条 SQL**。

它的主要优势不是“结果更正确”，而是：

- 更稳
- 更容易维护
- 更适合大表和正式环境

第二条 SQL 的主要优点只有一个：**写法更短**。  
但在正式项目里，这个优点通常不足以抵消它在性能稳定性和维护性上的劣势。
