package com.example.pay.notify;

/**
 * 通知处理结果。
 *
 * <p>只有 {@link #ACCEPTED} 与 {@link #ALREADY_CREDITED} 视为处理成功，
 * 其余一律返回 {@code failure} 让支付宝按策略重推。
 * 明确的结果枚举取代了原代码"异常后返回 null 再被 equals 调用打爆"的做法
 * （评估报告 SEC-012）。
 */
public enum NotifyOutcome {

    /** 验签通过并完成入账 */
    ACCEPTED,

    /** 该订单已入账，本次为重复通知，属正常情况 */
    ALREADY_CREDITED,

    /** 验签失败 */
    REJECT_SIGN,

    /** app_id 与本应用不符，可能是其它应用的通知被投递过来 */
    REJECT_APP_ID,

    /** seller_id 与配置不符，可能是伪造或跨商户重放 */
    REJECT_SELLER_ID,

    /** 交易状态不是支付成功 */
    REJECT_TRADE_STATUS,

    /** 本地不存在该订单号 */
    REJECT_ORDER_NOT_FOUND,

    /** 通知金额与本地订单金额不一致 */
    REJECT_AMOUNT_MISMATCH,

    /** 订单当前状态不允许入账（已关闭等） */
    REJECT_ORDER_STATE,

    /** 请求参数缺失或格式错误 */
    REJECT_BAD_REQUEST,

    /** 系统内部错误 */
    ERROR;

    /** 是否应向支付宝返回 success */
    public boolean isSuccess() {
        return this == ACCEPTED || this == ALREADY_CREDITED;
    }
}
