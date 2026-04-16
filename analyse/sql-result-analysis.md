# 两条 SQL 执行结果分析

## 结论

不是“始终一致”。

先说最直接的结论：

1. 按你当前贴出来的第二条 SQL 原文，它**不能正常执行**，因为 `WHERE` 里用了未定义别名：
   - `a.aliasname`
   - `v.is_beta`
2. 如果把第二条 SQL 修正为你大概率本来想写的：
   - `apt.aliasname`
   - `vt.is_beta`
   
   那么它和第一条 SQL 在**结果集语义上通常是一致的**，都是取：
   - 指定 `aliasname`
   - 指定 `is_beta`
   - 按 `(appid, os_target, device_type)` 分组
   - 取 `version_code` 最大的版本记录

所以准确说法是：

- **按当前原文：不一致，第二条会报错。**
- **按修正后的意图：大多数正常数据场景下，结果集等价。**

## 原因分析

### 1. 第二条 SQL 原文有别名错误

第二条 SQL 写的是：

```sql
... from store_app_version_t as vt
left join store_app_t as apt on apt.appid=vt.appid
where a.aliasname = #{alias, jdbcType = VARCHAR}
  and v.is_beta = IF(#{isBeta, jdbcType=BOOLEAN}, '1', '0')
  ...
```

但外层实际定义的别名只有：

- `vt`
- `apt`

并没有定义：

- `a`
- `v`

因此这条 SQL 如果原样执行，通常会报 `Unknown column` 或等价错误。

### 2. 修正别名后，两条 SQL 的目标逻辑基本相同

如果把第二条 SQL 理解为：

```sql
select vt.appvid as appvid, vt.appid as appid, ...
from store_app_version_t as vt
left join store_app_t as apt on apt.appid = vt.appid
where apt.aliasname = #{alias, jdbcType = VARCHAR}
  and vt.is_beta = IF(#{isBeta, jdbcType=BOOLEAN}, '1', '0')
  and vt.version_code = (
      select max(version_code)
      from store_app_version_t
      where appid = apt.appid
        and os_target = vt.os_target
        and device_type = vt.device_type
        and is_beta = IF(#{isBeta, jdbcType=BOOLEAN}, '1', '0')
  )
order by vt.os_target, vt.device_type desc
```

那么它与第一条 SQL 的核心语义一致：

- 第一条：先算出每组最大 `version_code`，再回表取整行
- 第二条：对每一行用相关子查询判断自己是否等于该组最大 `version_code`

这两种写法是常见的等价写法。

### 3. `LEFT JOIN` 在第二条 SQL 里实际上退化成了 `INNER JOIN`

第二条用了：

```sql
left join store_app_t as apt on apt.appid = vt.appid
where apt.aliasname = #{alias ...}
```

因为 `WHERE` 里又要求 `apt.aliasname = ...`，所以凡是 `apt` 连接不上、为 `NULL` 的行，都会被过滤掉。

因此这个 `LEFT JOIN` 的实际效果等同于：

```sql
inner join store_app_t as apt on apt.appid = vt.appid
```

这点与第一条 SQL 的 `INNER JOIN` 一致。

### 4. 两条 SQL 在“最大版本号重复”时都会返回多行

如果同一个 `(appid, os_target, device_type, is_beta)` 下存在多条记录，它们的 `version_code` 都等于最大值，例如：

| appid | os_target | device_type | is_beta | version_code | appvid |
|---|---|---|---|---:|---|
| 1001 | android | phone | 0 | 9 | 501 |
| 1001 | android | phone | 0 | 9 | 502 |

那么：

- 第一条 SQL 会把这两条都连出来
- 第二条 SQL 也会因为两条都满足 `vt.version_code = max(version_code)` 而返回两条

所以在这个场景下，两条 SQL 仍然一致，且都**不会自动去重**。

### 5. 真正的差异主要不在结果，而在“能否执行”和“执行代价”

结果语义上，修正后通常一致；但实现方式不同：

- 第一条 SQL：`GROUP BY + MAX + 回表 JOIN`
- 第二条 SQL：`相关子查询 + MAX`

在数据量较大时，第一条通常更容易被优化器稳定地执行成较高效的计划；第二条相关子查询更依赖数据库优化器能力。

所以更准确的说法是：

- **结果通常可等价**
- **执行计划和性能不一定相同**
- **你当前第二条 SQL 原文甚至无法执行**

## 对比总结

| 对比项 | 第一条 SQL | 第二条 SQL 原文 | 第二条 SQL 修正后 |
|---|---|---|---|
| 能否正常执行 | 可以 | 不可以，别名错误 | 可以 |
| 是否按 `aliasname + is_beta` 过滤 | 是 | 原文写错 | 是 |
| 是否取每组最大 `version_code` | 是 | 原文无法执行 | 是 |
| `LEFT JOIN`/`INNER JOIN` 实际效果 | `INNER JOIN` | 原文无法执行 | 实际等同 `INNER JOIN` |
| 结果是否总一致 | - | 否 | 通常一致 |
| 性能是否一定一致 | 否 | 否 | 否 |

## 最终判断

你的问题“这两个 SQL 执行结果一直吗”，答案是：

**不一直。**

原因分两层：

1. **按当前你贴出来的 SQL 原文，不一致**，因为第二条 SQL 有别名错误，无法正常执行。
2. **如果把第二条 SQL 改成正确别名后再比较**，那么两者在常规数据场景下，查询结果通常是一致的，但执行计划和性能不一定一致。
