-- =============================================================================
-- 安全支付入账：表结构
-- 修复问题：SEC-003 / SEC-004 / SEC-010 / SEC-011 / CODE-012
--
-- 设计要点
--   1. payment_order        本地订单，金额与归属以本表为唯一权威来源（修复 SEC-006）
--   2. payment_credit_ledger 入账流水，out_trade_no 唯一索引 → 数据库层面保证幂等
--   3. payment_notify_log   通知原始报文留痕，用于对账与取证
--   4. 主键使用自增/序列，不再用 max(id)+1（修复 SEC-010）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 本地订单表
-- -----------------------------------------------------------------------------
CREATE TABLE payment_order (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    out_trade_no    VARCHAR(64)  NOT NULL                COMMENT '商户订单号（对外唯一）',
    user_id         BIGINT       NOT NULL                COMMENT '下单用户ID（服务端写入，不接受外部传入）',
    subject         VARCHAR(256) NOT NULL                COMMENT '商品标题',
    total_amount    DECIMAL(12,2) NOT NULL               COMMENT '订单金额，以本字段为权威，禁止信任客户端',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING'
                    COMMENT '订单状态：PENDING / PAID / CLOSED',
    paid_at         DATETIME     NULL                    COMMENT '支付完成时间',
    trade_no        VARCHAR(64)  NULL                    COMMENT '支付宝交易号',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_out_trade_no (out_trade_no),
    KEY idx_order_user_status (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '本地订单';

-- -----------------------------------------------------------------------------
-- 入账流水表：幂等的最终防线
--   out_trade_no 唯一索引使重复通知在数据库层被拒绝，
--   无需依赖任何应用层判断，天然抵御并发与重复推送。
-- -----------------------------------------------------------------------------
CREATE TABLE payment_credit_ledger (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    out_trade_no    VARCHAR(64)   NOT NULL                COMMENT '商户订单号（唯一，幂等键）',
    trade_no        VARCHAR(64)   NOT NULL                COMMENT '支付宝交易号',
    user_id         BIGINT        NOT NULL                COMMENT '入账用户（服务端解析，非外部传入）',
    amount          DECIMAL(12,2) NOT NULL                COMMENT '入账金额',
    credited_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入账时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ledger_out_trade_no (out_trade_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '入账流水（幂等键）';

-- -----------------------------------------------------------------------------
-- 通知原始报文留痕
--   支付宝通知可能重推，同一 out_trade_no 会有多条记录，属正常。
-- -----------------------------------------------------------------------------
CREATE TABLE payment_notify_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    out_trade_no    VARCHAR(64)  NULL     COMMENT '商户订单号（验签失败时可能为空）',
    trade_no        VARCHAR(64)  NULL     COMMENT '支付宝交易号',
    trade_status    VARCHAR(32)  NULL     COMMENT '交易状态',
    total_amount    VARCHAR(16)  NULL     COMMENT '通知中的金额（原样留存）',
    sign_verified   TINYINT      NOT NULL DEFAULT 0 COMMENT '验签是否通过',
    accept_result   VARCHAR(32)  NOT NULL COMMENT '处理结果：ACCEPTED / REJECT_* 原因码',
    remote_ip       VARCHAR(64)  NULL     COMMENT '来源IP',
    raw_body        TEXT         NULL     COMMENT '原始报文（用于取证与对账）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notify_out_trade_no (out_trade_no),
    KEY idx_notify_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付宝异步通知留痕';

-- -----------------------------------------------------------------------------
-- 用户额度表（示例：对应原 TblGetAdvance 的业务语义）
--   使用行级锁 + 原子自增，避免读-改-写竞态。
-- -----------------------------------------------------------------------------
CREATE TABLE user_quota (
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    quota           DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可用额度',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户额度';

-- =============================================================================
-- 迁移说明（从旧表 TblGetAdvance 迁移时的注意事项）
-- =============================================================================
-- 1. 旧代码用 `select max(DID)+1` 生成主键，历史数据可能已存在重复或空洞，
--    迁移前必须执行去重检查：
--       SELECT memory, COUNT(*) FROM TblGetAdvance GROUP BY memory HAVING COUNT(*) > 1;
-- 2. `memory` 字段承载商户订单号语义，迁移到 payment_order.out_trade_no 后，
--    必须补建唯一索引，否则幂等保证不成立。
-- 3. 历史数据中的 user_id（原 ClientID）需与新的用户主键做映射校验，
--    存在无法映射的记录必须人工确认，不可默认归到某个账号。
