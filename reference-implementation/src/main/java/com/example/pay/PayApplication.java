package com.example.pay;

import com.example.pay.config.AlipayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 安全支付入账参考实现。
 *
 * <p>本项目是《代码安全与可行性评估报告》的配套参考实现，
 * 针对 Pay_zfb 模块中的 P0/P1 问题给出可直接对照的写法。
 * 每个类与方法上的 javadoc 均标注了它修复的问题编号。
 */
@SpringBootApplication
@EnableConfigurationProperties(AlipayProperties.class)
public class PayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class, args);
    }
}
