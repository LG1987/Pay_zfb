package com.example.pay.service;

import com.example.pay.domain.LocalOrder;
import com.example.pay.notify.NotifyOutcome;
import com.example.pay.repository.PaymentOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 入账服务 —— 全部数据库相关的校验与写操作都在这一个事务内完成。
 *
 * <p>修复评估报告：
 * <ul>
 *   <li><b>SEC-003</b> 入账用户来自本地订单（{@code order.userId()}），
 *       不再接受任何来自请求参数的用户标识</li>
 *   <li><b>SEC-004</b> 交易状态校验由调用方完成，本服务只处理"已确认支付成功"的入账</li>
 *   <li><b>SEC-006</b> 金额以本地订单为权威，与通知金额比对，不一致即拒绝</li>
 *   <li><b>CODE-012</b> 幂等由唯一索引保证，重复通知返回 ALREADY_CREDITED 而非重复入账</li>
 * </ul>
 *
 * <p><b>设计说明</b>：本类被刻意拆成独立的 Bean，而不是放在
 * {@link PaymentNotifyService} 里。因为 Spring 的 {@code @Transactional}
 * 基于代理实现，同一个 Bean 内部的自调用不会开启事务 —— 这是极常见的静默失效陷阱。
 */
@Service
public class PaymentCreditService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCreditService.class);

    private final PaymentOrderRepository repository;

    public PaymentCreditService(PaymentOrderRepository repository) {
        this.repository = repository;
    }

    /**
     * 幂等入账。
     *
     * <p>事务隔离级别使用 READ_COMMITTED，配合 {@code SELECT ... FOR UPDATE}
     * 行级锁，同一订单的并发通知会被串行化处理。
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public NotifyOutcome credit(String outTradeNo, String tradeNo, BigDecimal paidAmount) {

        // 1) 加锁读取本地订单 —— 金额与用户身份的权威来源
        Optional<LocalOrder> found = repository.findOrderForUpdate(outTradeNo);
        if (found.isEmpty()) {
            log.warn("通知对应的本地订单不存在 outTradeNo={}", outTradeNo);
            return NotifyOutcome.REJECT_ORDER_NOT_FOUND;
        }
        LocalOrder order = found.get();

        // 2) 订单状态校验：已关闭的订单不得入账
        if (LocalOrder.STATUS_CLOSED.equals(order.status())) {
            log.warn("订单已关闭，拒绝入账 outTradeNo={}", outTradeNo);
            return NotifyOutcome.REJECT_ORDER_STATE;
        }

        // 3) 金额比对：以本地订单为准，compareTo 忽略标度差异（0.01 与 0.010 视为相等）
        if (order.totalAmount().compareTo(paidAmount) != 0) {
            log.error("通知金额与订单金额不一致，拒绝入账 outTradeNo={} 订单={} 通知={}",
                    outTradeNo, order.totalAmount(), paidAmount);
            return NotifyOutcome.REJECT_AMOUNT_MISMATCH;
        }

        // 4) 幂等写入流水；唯一索引冲突说明已入账过
        boolean firstTime = repository.tryInsertLedger(outTradeNo, tradeNo, order.userId(), paidAmount);
        if (!firstTime) {
            log.info("重复通知，订单已入账，直接返回成功 outTradeNo={}", outTradeNo);
            return NotifyOutcome.ALREADY_CREDITED;
        }

        // 5) 更新订单状态（带状态前置条件，天然幂等）
        int updated = repository.markOrderPaid(order.id(), tradeNo, LocalDateTime.now());
        if (updated == 0) {
            // 并发下另一事务已完成状态流转，此时流水已由唯一索引挡下，不应到达这里
            log.warn("订单状态更新影响 0 行，可能已被并发处理 outTradeNo={} 当前状态={}",
                    outTradeNo, order.status());
        }

        // 6) 增加额度 —— 用户ID 取自订单，不接受外部输入
        repository.increaseQuota(order.userId(), paidAmount);

        log.info("入账成功 outTradeNo={} tradeNo={} userId={} amount={}",
                outTradeNo, tradeNo, order.userId(), paidAmount);
        return NotifyOutcome.ACCEPTED;
    }

    /** 关单：仅关闭订单，<b>绝不变更额度</b>。修复评估报告 SEC-005。 */
    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(String outTradeNo) {
        repository.findOrderForUpdate(outTradeNo)
                .ifPresent(order -> repository.closeOrder(order.id()));
    }
}
