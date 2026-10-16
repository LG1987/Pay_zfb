# Changelog

本文件记录本仓库的所有重要变更。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [1.0.0] - 待发布

### Added

- **代码安全与可行性评估报告**：对 `Pay_zfb` 模块 6 个 Java 文件的全量审计，
  共 20 项问题（P0 严重 8 项、P1 高 4 项、P2 中 8 项），含复现路径与 20 条整改工单
- **密钥材料解析结论**：通过 DER 结构解析与模数指纹比对，确认
  硬编码的 `ss` 为 2048 位 RSA 私钥（PKCS#8，未加密），
  且 `zfb.java` 传入的"支付宝公钥"实为商户自己的公钥
- **安全支付入账参考实现**：验签、交易状态校验、金额比对、
  三层幂等保障、通知留痕
- **建表脚本**：`payment_order` / `payment_credit_ledger` /
  `payment_notify_log` / `user_quota`，含从旧表迁移的注意事项
- 报告 Word 版生成脚本 `tools/build_report_docx.py`

### Security

- 本仓库**不包含**任何原始密钥材料，报告中仅以模数指纹标识
- 提示事项：被评估的原始代码中硬编码了真实支付宝应用私钥与数据库口令，
  需立即执行整改工单 WO-01（重置应用密钥）

[Unreleased]: https://github.com/LG1987/Pay_zfb/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/LG1987/Pay_zfb/releases/tag/v1.0.0
