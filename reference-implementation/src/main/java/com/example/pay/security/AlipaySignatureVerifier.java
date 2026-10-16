package com.example.pay.security;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.example.pay.config.AlipayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付宝通知验签器。
 *
 * <p>修复评估报告 <b>SEC-007</b>（异步通知验签缺失）与 <b>SEC-009</b>（公钥配置错误）。
 *
 * <p>原代码把商户自己的公钥当成支付宝公钥传入 SDK（模数指纹与商户私钥完全一致），
 * 导致验签逻辑形同虚设。本类强制使用配置中独立的 {@code alipayPublicKey}，
 * 并在密钥缺失时直接拒绝，绝不"验签失败也放行"。
 */
@Component
public class AlipaySignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(AlipaySignatureVerifier.class);

    private final AlipayProperties properties;

    public AlipaySignatureVerifier(AlipayProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验支付宝异步通知的签名。
     *
     * <p>使用支付宝官方 SDK 的 {@code rsaCheckV1}，它会剔除 {@code sign} 与
     * {@code sign_type} 后按参数名排序拼接，再用支付宝公钥验签。
     *
     * @param params 通知的全部请求参数（原样，不要做任何过滤或改写）
     * @return 验签通过返回 true；任何异常或不通过均返回 false
     */
    public boolean verify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            log.warn("支付宝通知参数为空，拒绝");
            return false;
        }
        if (!properties.isConfigured()) {
            log.error("支付宝密钥未配置，拒绝所有通知。请检查 ALIPAY_APP_ID / ALIPAY_MERCHANT_PRIVATE_KEY / ALIPAY_PUBLIC_KEY");
            return false;
        }
        String sign = params.get("sign");
        if (sign == null || sign.isBlank()) {
            log.warn("支付宝通知缺少 sign 字段，拒绝 outTradeNo={}", params.get("out_trade_no"));
            return false;
        }
        try {
            boolean ok = AlipaySignature.rsaCheckV1(
                    params,
                    properties.getAlipayPublicKey(),
                    properties.getCharset(),
                    properties.getSignType());
            if (!ok) {
                log.warn("支付宝通知验签不通过 outTradeNo={} tradeNo={}",
                        params.get("out_trade_no"), params.get("trade_no"));
            }
            return ok;
        } catch (AlipayApiException e) {
            // 验签异常一律按失败处理，绝不降级放行
            log.error("支付宝通知验签异常 outTradeNo={}", params.get("out_trade_no"), e);
            return false;
        }
    }
}
