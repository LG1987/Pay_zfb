package com.example.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付宝接入配置。
 *
 * <p>安全要求：本类中的所有敏感字段（商户私钥、支付宝公钥）必须来自环境变量或
 * 密钥管理系统，<b>严禁</b>硬编码在代码或提交到仓库。
 * 参见评估报告 SEC-001 / SEC-009。
 */
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** 支付宝网关。沙箱环境为 https://openapi.alipaydev.com/gateway.do */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    /** 应用 AppId */
    private String appId;

    /** 商户应用私钥（PKCS#8，单行无换行）。对应评估报告中泄露的 {@code ss} */
    private String merchantPrivateKey;

    /** 支付宝公钥。对应评估报告中配置错误的 {@code gs} */
    private String alipayPublicKey;

    /** 签名算法，固定 RSA2 */
    private String signType = "RSA2";

    /** 字符集，固定 UTF-8 */
    private String charset = "UTF-8";

    /** 卖家支付宝账号 ID（PID），用于校验通知归属，防止跨商户通知攻击 */
    private String sellerId;

    /** 异步通知地址，必须为 HTTPS 独立端点，且只返回纯文本 success */
    private String notifyUrl;

    /** 同步跳转地址，仅用于展示，不得在其中入账 */
    private String returnUrl;

    public boolean isConfigured() {
        return notBlank(appId) && notBlank(merchantPrivateKey) && notBlank(alipayPublicKey);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getMerchantPrivateKey() {
        return merchantPrivateKey;
    }

    public void setMerchantPrivateKey(String merchantPrivateKey) {
        this.merchantPrivateKey = merchantPrivateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}
