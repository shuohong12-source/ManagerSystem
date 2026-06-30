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
