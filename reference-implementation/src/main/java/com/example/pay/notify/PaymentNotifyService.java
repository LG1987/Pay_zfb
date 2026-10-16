package com.example.pay.notify;

import com.example.pay.config.AlipayProperties;
import com.example.pay.repository.PaymentOrderRepository;
import com.example.pay.security.AlipaySignatureVerifier;
import com.example.pay.service.PaymentCreditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 支付宝异步通知处理编排。
 *
 * <p>处理顺序严格固定，任一步失败立即拒绝并留痕：
 * <pre>
 *   1. 验签                        ← SEC-007
 *   2. 校验 app_id                  ← 防止其它应用的通知被投递过来
 *   3. 校验 seller_id               ← 防止跨商户重放
 *   4. 校验 trade_status            ← SEC-004
 *   5. 事务内：查订单 → 比对金额 → 幂等入账   ← SEC-003 / SEC-006 / CODE-012
 * </pre>
 *
 * <p>本类<b>不加</b> {@code @Transactional}：验签是纯计算，不应占用数据库连接；
 * 数据库操作全部委托给独立的 {@link PaymentCreditService} 事务。
 */
@Service
public class PaymentNotifyService {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotifyService.class);

    /**
     * 视为"已支付成功"的交易状态。
     *
     * <p>注意：绝不能用响应码 {@code code == 10000} 代替本判断 ——
     * {@code code=10000} 仅表示"接口调用成功"。这正是原代码
     * {@code zfb_cx} 的致命错误（评估报告 SEC-004）。
     */
    private static final Set<String> PAID_TRADE_STATUS = Set.of("TRADE_SUCCESS", "TRADE_FINISHED");

    private final AlipayProperties properties;
    private final AlipaySignatureVerifier verifier;
    private final PaymentCreditService creditService;
    private final PaymentOrderRepository repository;

    public PaymentNotifyService(AlipayProperties properties,
                                AlipaySignatureVerifier verifier,
                                PaymentCreditService creditService,
                                PaymentOrderRepository repository) {
        this.properties = properties;
        this.verifier = verifier;
        this.creditService = creditService;
        this.repository = repository;
    }

    public NotifyOutcome handle(Map<String, String> params, String remoteIp) {
        String outTradeNo = trim(params.get("out_trade_no"));
        String tradeNo = trim(params.get("trade_no"));
        String tradeStatus = trim(params.get("trade_status"));
        String rawAmount = trim(params.get("total_amount"));
        String rawBody = PaymentOrderRepository.serialize(params);

        // ---------- 1. 验签 ----------
        boolean signOk = verifier.verify(params);
        if (!signOk) {
            logNotify(outTradeNo, tradeNo, tradeStatus, rawAmount, false,
                    NotifyOutcome.REJECT_SIGN, remoteIp, rawBody);
            return NotifyOutcome.REJECT_SIGN;
        }

        // ---------- 2. app_id ----------
        if (!properties.getAppId().equals(trim(params.get("app_id")))) {
            log.warn("通知 app_id 不匹配，实际={}", params.get("app_id"));
            logNotify(outTradeNo, tradeNo, tradeStatus, rawAmount, true,
                    NotifyOutcome.REJECT_APP_ID, remoteIp, rawBody);
            return NotifyOutcome.REJECT_APP_ID;
        }

        // ---------- 3. seller_id ----------
        String sellerId = properties.getSellerId();
        if (sellerId != null && !sellerId.isBlank()
                && !sellerId.equals(trim(params.get("seller_id")))) {
            log.warn("通知 seller_id 不匹配，实际={}", params.get("seller_id"));
            logNotify(outTradeNo, tradeNo, tradeStatus, rawAmount, true,
                    NotifyOutcome.REJECT_SELLER_ID, remoteIp, rawBody);
            return NotifyOutcome.REJECT_SELLER_ID;
        }

        // ---------- 4. 交易状态 ----------
        if (tradeStatus == null || !PAID_TRADE_STATUS.contains(tradeStatus)) {
            log.info("交易状态非支付成功，不入账 outTradeNo={} tradeStatus={}", outTradeNo, tradeStatus);
            logNotify(outTradeNo, tradeNo, tradeStatus, rawAmount, true,
                    NotifyOutcome.REJECT_TRADE_STATUS, remoteIp, rawBody);
            return NotifyOutcome.REJECT_TRADE_STATUS;
        }

        // ---------- 5. 基础参数 ----------
        if (outTradeNo == null || tradeNo == null || rawAmount == null) {
            log.warn("通知缺少必要字段 outTradeNo={} tradeNo={} totalAmount={}",
                    outTradeNo, tradeNo, rawAmount);
            logNotify(outTradeNo, tradeNo, tradeStatus, rawAmount, true,
                    NotifyOutcome.REJECT_BAD_REQUEST, remoteIp, rawBody);
            return NotifyOutcome.REJECT_BAD_REQUEST;
        }

        BigDecimal paidAmount;
        try {
            paidAmount = new BigDecimal(rawAmount);
        } catch (NumberFormatException e) {
            log.warn("通知金额格式非法 value={}", rawAmount);
            logNotify(outTradeNo, tradeNo, tradeStatus, rawAmount, true,
                    NotifyOutcome.REJECT_AMOUNT_MISMATCH, remoteIp, rawBody);
            return NotifyOutcome.REJECT_AMOUNT_MISMATCH;
        }

        // ---------- 6. 事务内入账 ----------
        NotifyOutcome outcome;
        try {
            outcome = creditService.credit(outTradeNo, tradeNo, paidAmount);
        } catch (Exception e) {
            // 事务已由 Spring 回滚；此处只做留痕，返回 failure 让支付宝重推
            log.error("入账处理异常 outTradeNo={}", outTradeNo, e);
            logNotify(outTradeNo, tradeNo, tradeStatus, rawAmount, true,
                    NotifyOutcome.ERROR, remoteIp, rawBody);
            return NotifyOutcome.ERROR;
        }

        logNotify(outTradeNo, tradeNo, tradeStatus, rawAmount, true, outcome, remoteIp, rawBody);
        return outcome;
    }

    private void logNotify(String outTradeNo, String tradeNo, String tradeStatus, String totalAmount,
                           boolean signVerified, NotifyOutcome outcome, String remoteIp, String rawBody) {
        try {
            repository.insertNotifyLog(outTradeNo, tradeNo, tradeStatus, totalAmount,
                    signVerified, outcome.name(), remoteIp, rawBody);
        } catch (Exception e) {
            // 留痕失败不应影响主流程判定，但必须告警
            log.error("写入通知留痕失败 outTradeNo={}", outTradeNo, e);
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
