package com.example.pay.domain;

import java.math.BigDecimal;

/**
 * 本地订单（金额与归属的唯一权威来源）。
 *
 * <p>修复评估报告 <b>SEC-006</b>：原先下单金额直接取自 URL 参数 {@code UnitPrice}，
 * 客户端可随意篡改。本记录代表服务端订单，任何金额与用户身份的判定
 * 都必须以本对象为准。
 */
public record LocalOrder(
        long id,
        String outTradeNo,
        long userId,
        BigDecimal totalAmount,
        String status) {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CLOSED = "CLOSED";
}
