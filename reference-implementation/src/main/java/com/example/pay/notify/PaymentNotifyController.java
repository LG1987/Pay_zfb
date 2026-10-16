package com.example.pay.notify;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付宝异步通知端点。
 *
 * <p>契约（不可违背）：
 * <ul>
 *   <li>只接受 POST，Content-Type 为 form-urlencoded</li>
 *   <li>处理成功返回 <b>纯文本</b> {@code success}；否则返回 {@code failure} 触发支付宝重推</li>
 *   <li>HTTP 状态码恒为 200 —— 用响应体而非状态码表达处理结果</li>
 *   <li><b>绝不在本端点返回 HTML</b>。原实现把 notify_url 与 return_url 指向同一个 JSP
 *       （评估报告 SEC-008），导致支付宝收到 HTML 判定为失败并反复重推</li>
 * </ul>
 *
 * <p>本端点<b>不需要登录态</b>：调用方是支付宝服务器，身份由签名保证。
 * 这与原代码的问题不同 —— 原代码的错误不是"该端点没鉴权"，
 * 而是把<b>面向用户的查询/关单接口</b>当成了入账入口且毫无校验（SEC-003）。
 */
@RestController
public class PaymentNotifyController {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotifyController.class);

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";

    private final PaymentNotifyService notifyService;

    public PaymentNotifyController(PaymentNotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @PostMapping(
            path = "/payment/alipay/notify",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> notify(@RequestParam Map<String, String> params,
                                         HttpServletRequest request) {
        String remoteIp = resolveClientIp(request);
        log.info("收到支付宝异步通知 outTradeNo={} tradeStatus={} from={}",
                params.get("out_trade_no"), params.get("trade_status"), remoteIp);

        NotifyOutcome outcome;
        try {
            outcome = notifyService.handle(params, remoteIp);
        } catch (Exception e) {
            // 兜底：任何未预期异常都不得导致向支付宝返回非预期响应
            log.error("处理支付宝通知时发生未预期异常", e);
            return ResponseEntity.ok(FAILURE);
        }

        log.info("支付宝通知处理结果 outTradeNo={} outcome={}", params.get("out_trade_no"), outcome);
        return ResponseEntity.ok(outcome.isSuccess() ? SUCCESS : FAILURE);
    }

    /**
     * 解析来源 IP。
     *
     * <p>仅在明确部署于可信反向代理之后时才信任 {@code X-Forwarded-For}，
     * 否则该头部可被伪造。生产环境应通过 Spring Boot 的
     * {@code server.forward-headers-strategy} 统一处理。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
