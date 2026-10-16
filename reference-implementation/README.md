# 安全支付入账 · 参考实现

本目录是[《代码安全与可行性评估报告》](../docs/代码安全与可行性评估报告.md)的配套参考实现，
针对 `Pay_zfb` 模块的 P0 / P1 问题给出可直接对照的写法。

> ⚠️ **验证状态说明**
> 本实现是**参考代码**，用于演示正确的处理顺序与边界条件。
> 编写环境无法访问 Maven 中央仓库与 MySQL，因此**未经编译与运行验证**。
> 正式采用前请：补齐单元测试与集成测试 → 本地跑通 → 用支付宝**沙箱环境**做端到端联调。

---

## 一、修复对照表

每个源文件的类注释中都标注了它修复的问题编号，对应关系如下：

| 评估报告问题 | 严重级别 | 修复位置 |
| --- | --- | --- |
| SEC-001 私钥硬编码 | 🔴 P0 | `AlipayProperties` + `application.yml`：密钥全部改为环境变量注入 |
| SEC-003 入账零鉴权、入参指定用户 | 🔴 P0 | `PaymentCreditService.credit()`：用户 ID 一律取自本地订单 |
| SEC-004 入账未校验支付状态 | 🔴 P0 | `PaymentNotifyService`：`trade_status ∈ {TRADE_SUCCESS, TRADE_FINISHED}` |
| SEC-005 关单后错误入账 | 🔴 P0 | `PaymentCreditService.closeOrder()`：只关单，不动额度 |
| SEC-006 下单金额客户端可控 | 🔴 P0 | `LocalOrder`：金额以本地订单为权威并逐笔比对 |
| SEC-007 通知验签缺失 | 🔴 P0 | `AlipaySignatureVerifier` + `PaymentNotifyController` |
| SEC-008 明文 HTTP、return/notify 混用 | 🔴 P0 | `PaymentNotifyController`：独立端点，只返回纯文本 |
| SEC-009 支付宝公钥配错 | 🟠 P1 | `AlipaySignatureVerifier.verify()`：使用独立的 `alipayPublicKey` |
| SEC-010 主键 `max+1` 竞态 | 🟠 P1 | `V1__secure_payment_schema.sql`：改用自增主键 |
| SEC-011 SQL 拼接 | 🟠 P1 | `PaymentOrderRepository`：全部参数化 |
| SEC-012 异常吞没返回 null | 🟠 P1 | `NotifyOutcome` 枚举取代 `null` 返回值 |
| CODE-004 无连接池 | 🟡 P2 | `application.yml`：HikariCP |
| CODE-012 无幂等设计 | 🟡 P2 | `payment_credit_ledger` 唯一索引 + `ON DUPLICATE KEY UPDATE` |

---

## 二、处理流程

```
支付宝服务器
     │  POST /payment/alipay/notify  (form-urlencoded)
     ▼
┌─────────────────────────────────────────────────────────┐
│ PaymentNotifyController   恒返回 HTTP 200 + 纯文本       │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ PaymentNotifyService      （无事务，不占用数据库连接）    │
│   1. 验签            失败 → REJECT_SIGN                  │
│   2. 校验 app_id     不符 → REJECT_APP_ID                │
│   3. 校验 seller_id  不符 → REJECT_SELLER_ID             │
│   4. 校验 trade_status    → REJECT_TRADE_STATUS          │
│   5. 解析并校验金额格式                                   │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ PaymentCreditService      @Transactional                 │
│   6. SELECT ... FOR UPDATE  锁定订单行                   │
│   7. 订单存在性 / 状态校验                                │
│   8. 金额比对（以本地订单为准）                            │
│   9. INSERT 流水（唯一索引） 冲突 → ALREADY_CREDITED      │
│  10. UPDATE 订单状态（带状态前置条件）                     │
│  11. UPDATE 用户额度（原子自增）                           │
└────────────────────────┬────────────────────────────────┘
                         ▼
              成功 → "success"    失败 → "failure"
```

### 为什么把入账拆成独立的 Service

Spring 的 `@Transactional` 基于代理实现，**同一个 Bean 内部的自调用不会开启事务**。
如果把 `credit()` 直接写在 `PaymentNotifyService` 里由本类方法调用，
事务会静默失效 —— 这是生产事故的常见来源。因此拆成独立 Bean，由外部注入调用。

---

## 三、幂等的实现层次

原代码仅靠 `select * from TblGetAdvance where Memory = ?` 做查重，
没有唯一索引兜底，并发下仍会重复入账。本实现用三层防护：

| 层次 | 手段 | 抵御场景 |
| --- | --- | --- |
| 1 | `SELECT ... FOR UPDATE` 行级锁 | 同一订单的并发通知 |
| 2 | `payment_credit_ledger.out_trade_no` **唯一索引** + `ON DUPLICATE KEY UPDATE` | 通知重推、并发穿透锁 |
| 3 | 订单状态前置条件 `WHERE status = 'PENDING'` | 状态机被绕过 |

第 2 层是**根本保障**：即使前面所有应用层判断都被绕过，
数据库也不会允许同一订单写入两条入账流水。

> 使用 `ON DUPLICATE KEY UPDATE id = id` 而非捕获 `DuplicateKeyException`：
> 后者在事务中失败会让部分连接池/JPA 实现把事务标记为 rollback-only，
> 导致"明明处理成功却整体回滚"的隐蔽故障。

---

## 四、目录结构

```
reference-implementation/
├── README.md                      本文件
├── pom.xml
├── sql/
│   └── V1__secure_payment_schema.sql    建表脚本（含迁移注意事项）
└── src/main/
    ├── java/com/example/pay/
    │   ├── PayApplication.java
    │   ├── config/AlipayProperties.java
    │   ├── domain/LocalOrder.java
    │   ├── security/AlipaySignatureVerifier.java
    │   ├── notify/
    │   │   ├── PaymentNotifyController.java
    │   │   ├── PaymentNotifyService.java
    │   │   └── NotifyOutcome.java
    │   ├── repository/PaymentOrderRepository.java
    │   └── service/PaymentCreditService.java
    └── resources/application.yml
```

---

## 五、本地运行

```bash
# 1. 建库
mysql -u root -p -e "CREATE DATABASE pay DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p pay < sql/V1__secure_payment_schema.sql

# 2. 注入密钥（绝不要写进 application.yml）
export DB_USERNAME=pay_app
export DB_PASSWORD='<强随机口令>'
export ALIPAY_APP_ID='<你的AppId>'
export ALIPAY_MERCHANT_PRIVATE_KEY='<PKCS#8 单行私钥>'
export ALIPAY_PUBLIC_KEY='<支付宝公钥>'
export ALIPAY_SELLER_ID='<卖家PID>'

# 3. 启动
mvn spring-boot:run
```

冒烟测试（验签必然失败的用例，验证端点行为正确）：

```bash
curl -i -X POST http://localhost:8080/payment/alipay/notify \
  -d "out_trade_no=TEST001&trade_status=TRADE_SUCCESS&total_amount=0.01&sign=invalid"
# 预期：HTTP 200，响应体 failure；数据库 payment_notify_log 新增一条 sign_verified=0
```

---

## 六、上线前必须补齐

本参考实现覆盖了**入账链路**，以下内容尚未包含，需要在实际接入时补全：

| 项 | 说明 |
| --- | --- |
| 下单接口 | 服务端生成订单、金额取自本地订单、`out_trade_no` 服务端生成 |
| 关单接口 | 调用支付宝关单 API，**不触碰额度** |
| 主动对账任务 | 定时用 `AlipayTradeQueryRequest` 补偿丢失的通知；**查询结果同样要校验 `trade_status`** |
| 退款流程 | 独立的退款状态机与幂等表 |
| 公钥证书模式 | 若使用证书模式，验签改为 `AlipaySignature.rsaCertCheckV1` |
| 单元测试 | 建议覆盖：验签失败、金额不符、重复通知、交易状态非成功、订单不存在 |
| 集成测试 | Testcontainers + MySQL，验证并发通知只入账一次 |
| 密钥扫描 | CI 中接入 gitleaks，防止再次硬编码 |

---

## 七、再次提醒

参考实现解决的是**代码怎么写**。但本次评估中最紧急的事项是：

1. **立即在支付宝开放平台重置应用密钥**（SEC-001）—— 代码改得再好，泄露的旧私钥依然有效。
2. **审计 `TrReZfbPay.jsp`**（SEC-007）—— 真正的线上回调入口在仓库之外，其验签实现未知。

详见报告的[第 7 章 整改工单](../docs/代码安全与可行性评估报告.md)。
