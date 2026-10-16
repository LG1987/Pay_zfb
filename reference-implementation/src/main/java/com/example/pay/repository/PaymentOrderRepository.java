package com.example.pay.repository;

import com.example.pay.domain.LocalOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 订单与入账流水的数据访问层。
 *
 * <p>修复评估报告：
 * <ul>
 *   <li><b>SEC-010</b> 主键由数据库自增生成，不再使用 {@code select max(id)+1}</li>
 *   <li><b>SEC-011</b> 全部使用 {@link JdbcTemplate} 的参数化占位符，无字符串拼接</li>
 *   <li><b>CODE-004</b> 连接由 Spring Boot 默认的 HikariCP 连接池管理</li>
 * </ul>
 */
@Repository
public class PaymentOrderRepository {

    private final JdbcTemplate jdbc;

    public PaymentOrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<LocalOrder> ORDER_MAPPER = (rs, rowNum) -> new LocalOrder(
            rs.getLong("id"),
            rs.getString("out_trade_no"),
            rs.getLong("user_id"),
            rs.getBigDecimal("total_amount"),
            rs.getString("status"));

    /**
     * 加行级锁读取订单，防止并发通知同时通过校验。
     *
     * <p>必须在事务内调用。{@code FOR UPDATE} 使同一订单的并发处理串行化。
     */
    public Optional<LocalOrder> findOrderForUpdate(String outTradeNo) {
        List<LocalOrder> rows = jdbc.query(
                "SELECT id, out_trade_no, user_id, total_amount, status "
                        + "FROM payment_order WHERE out_trade_no = ? FOR UPDATE",
                ORDER_MAPPER,
                outTradeNo);
        return rows.stream().findFirst();
    }

    public Optional<LocalOrder> findOrder(String outTradeNo) {
        List<LocalOrder> rows = jdbc.query(
                "SELECT id, out_trade_no, user_id, total_amount, status "
                        + "FROM payment_order WHERE out_trade_no = ?",
                ORDER_MAPPER,
                outTradeNo);
        return rows.stream().findFirst();
    }

    /**
     * 尝试写入入账流水 —— 幂等的最终防线。
     *
     * <p>依赖 {@code payment_credit_ledger.uk_ledger_out_trade_no} 唯一索引。
     * 使用 {@code ON DUPLICATE KEY UPDATE} 而非捕获异常，避免在事务中
     * 因语句失败污染连接状态。
     *
     * @return true 表示本次真正写入（首次入账）；false 表示该订单已入账过
     */
    public boolean tryInsertLedger(String outTradeNo, String tradeNo, long userId, BigDecimal amount) {
        int affected = jdbc.update(
                "INSERT INTO payment_credit_ledger (out_trade_no, trade_no, user_id, amount) "
                        + "VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE id = id",
                outTradeNo, tradeNo, userId, amount);
        return affected == 1;
    }

    /**
     * 将订单置为已支付。带状态前置条件，天然幂等。
     *
     * @return 受影响行数，0 表示订单已不在 PENDING 状态
     */
    public int markOrderPaid(long orderId, String tradeNo, LocalDateTime paidAt) {
        return jdbc.update(
                "UPDATE payment_order SET status = ?, trade_no = ?, paid_at = ?, version = version + 1 "
                        + "WHERE id = ? AND status = ?",
                LocalOrder.STATUS_PAID, tradeNo, paidAt, orderId, LocalOrder.STATUS_PENDING);
    }

    /** 增加用户额度。原子自增，无读-改-写竞态。 */
    public void increaseQuota(long userId, BigDecimal amount) {
        jdbc.update(
                "INSERT INTO user_quota (user_id, quota) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE quota = quota + VALUES(quota)",
                userId, amount);
    }

    public void closeOrder(long orderId) {
        jdbc.update(
                "UPDATE payment_order SET status = ?, version = version + 1 "
                        + "WHERE id = ? AND status = ?",
                LocalOrder.STATUS_CLOSED, orderId, LocalOrder.STATUS_PENDING);
    }

    /** 记录通知留痕。此方法不应在业务事务内调用，避免业务回滚导致审计丢失。 */
    public void insertNotifyLog(String outTradeNo, String tradeNo, String tradeStatus,
                                String totalAmount, boolean signVerified, String acceptResult,
                                String remoteIp, String rawBody) {
        jdbc.update(
                "INSERT INTO payment_notify_log "
                        + "(out_trade_no, trade_no, trade_status, total_amount, sign_verified, accept_result, remote_ip, raw_body) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                outTradeNo, tradeNo, tradeStatus, totalAmount,
                signVerified ? 1 : 0, acceptResult, remoteIp, rawBody);
    }

    /** 将参数表序列化为留痕字符串。 */
    public static String serialize(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(k).append('=').append(v);
        });
        return sb.toString();
    }
}
