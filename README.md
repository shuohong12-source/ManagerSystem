# 智慧跨境物流供应链管理系统

数据库系统原理课程设计项目，使用 Java Spring Boot + MyBatis + MySQL 实现 B/S 架构的跨境物流供应链管理系统。

## 功能概览

- 基础数据维护：枢纽区域、城市、仓库、商品、供应商、库存的增删改查。
- 运单管理：创建运单、动态添加多个运单项、自动计算运费、多条件查询。
- 并发库存控制：创建运单时在事务内扣减库存，使用条件更新避免超卖。
- BI 看板：展示各枢纽区域货运量、Top 10 高价值商品、库存预警。
- 智能仓库推荐：按同城市、同枢纽区域、库存量规则推荐发货仓。
- 自动对账：按月份生成物流费用报表，并导出 Excel。

## 目录结构

```text
.
├── pom.xml
├── sql
│   ├── 01_schema.sql
│   ├── 02_seed.sql
│   ├── 03_views_procedures.sql
│   └── 04_test_data.sql
├── src/main
│   ├── java/com/example/logistics
│   │   ├── controller
│   │   ├── mapper
│   │   ├── model
│   │   └── service
│   └── resources
│       ├── mapper
│       ├── static
│       └── templates
└── docs
    ├── 数据库课程设计报告.md
    └── 汇报PPT大纲.md
```

## 运行步骤

1. 安装 MySQL 8.x，创建数据库并导入脚本：

```sql
SOURCE sql/01_schema.sql;
SOURCE sql/02_seed.sql;
SOURCE sql/03_views_procedures.sql;
-- 可选：导入 100 条运单测试数据
SOURCE sql/04_test_data.sql;
```

2. 修改 `src/main/resources/application.yml` 中的数据库用户名和密码。

3. 安装 Maven 后启动项目：

```powershell
mvn spring-boot:run
```

4. 浏览器访问：

```text
http://localhost:8080
```

## 默认数据库

- 数据库名：`smart_logistics`
- 默认用户名：`root`
- 默认密码：请在 `application.yml` 中改成自己的 MySQL 密码。

## 并发库存控制说明

系统创建运单时使用 `@Transactional(isolation = Isolation.READ_COMMITTED)` 包裹运单主表、明细表与库存扣减逻辑。库存扣减使用如下条件更新：

```sql
UPDATE inventory
SET quantity = quantity - ?, frozen_quantity = frozen_quantity + ?
WHERE whid = ? AND goodsid = ? AND quantity >= ?;
```

当并发请求同时扣减同一仓库商品时，只有满足库存条件的事务能更新成功，其他事务会失败并回滚，从而避免库存负数和超卖。

### 事务隔离级别与选型依据

项目库存扣减事务明确使用 `READ_COMMITTED` 隔离级别。该隔离级别可以避免脏读，同时相比 `REPEATABLE_READ`、`SERIALIZABLE` 持有更少的锁资源，更适合物流仓储系统中高频出库、发运场景。

库存扣减没有采用“先查询库存再更新”的两步式逻辑，而是通过数据库单条条件 `UPDATE` 完成库存判断与扣减。InnoDB 会在更新命中的库存行上加排他锁，多个并发事务同时扣减同一 `whid + goodsid` 时会串行化执行该行更新；后提交或后获得锁的事务会基于最新已提交库存重新判断 `quantity >= ?`，库存不足时更新行数为 0，业务层立即抛出异常并回滚整笔运单创建事务。

该方案的实际影响是：同一仓库同一商品的扣减会按行锁排队，保障强一致和不超卖；不同仓库或不同商品的库存行互不阻塞，可以并发处理。相比使用全局锁或最高隔离级别，它把锁范围控制在单条库存记录上，兼顾一致性与吞吐量。

### 数据库层面优化

- `inventory` 使用 `(whid, goodsid)` 作为主键，库存扣减条件直接命中唯一库存行，减少扫描范围和锁竞争。
- `inventory` 增加 `idx_inventory_goods_quantity (goodsid, quantity)`，支持按商品库存查询、推荐仓库和低库存筛选。
- `stock_txn` 增加 `idx_txn_goods_time (goodsid, create_time)` 与 `idx_txn_wh_goods (whid, goodsid)`，优化库存流水按商品、仓库维度的查询。
- 事务范围只覆盖运单主表、明细、库存冻结/释放、流水记录等必须保持一致的数据修改；运费计算等内存计算在事务内快速完成，不引入额外远程调用，缩短事务执行时间。
- 业务层根据 `UPDATE` 影响行数判断库存扣减结果，库存不足立即抛异常回滚，避免事务继续执行造成锁占用时间变长。
