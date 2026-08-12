# zhush-boot4-parent 项目分析报告

> 分析时间：2026-08-11 ｜ 范围：全仓库（含 BOM、framework starters、业务模块、配置、文档）
> 分析时已执行全模块编译验证：**BUILD SUCCESS**（zhush-server / union / pay / ai 全量编译通过）

## 快速结论

| 维度 | 评级 | 说明 |
|---|---|---|
| 架构分层 | ★★★★☆ | BOM + starter 化框架层 + 业务模块 + 单体聚合，分层清晰 |
| 依赖治理 | ★★★★☆ | 版本全部集中 BOM + flatten 统一 revision，本次修复 1 处缺失 |
| 功能完成度 | ★★★☆☆ | 16 个 starter 中 5 个仍是空壳；union 大模块未接入 |
| 安全性 | ★★☆☆☆ | union 44 个管理端接口无细粒度权限；prod 配置有暴露风险 |
| 可运维性 | ★★★☆☆ | 监控/追踪已具备；多环境配置未同步 |

## 报告索引

| 文件 | 内容 |
|---|---|
| [01-总体架构与模块分析.md](./01-总体架构与模块分析.md) | 模块结构、代码规模、分层关系、启动装配链路 |
| [02-依赖与技术栈分析.md](./02-依赖与技术栈分析.md) | 技术栈版本清单、BOM 治理方式、Boot 4 兼容要点 |
| [03-功能完成度盘点.md](./03-功能完成度盘点.md) | 各 starter / 业务模块功能落地矩阵 |
| [04-风险与改进建议.md](./04-风险与改进建议.md) | 问题清单（P0/P1/P2）+ 优先级路线图 |

## 本次分析中顺手修复的问题

1. **BOM 缺失 x-file-storage 版本管理**：`starter-file` 引用了 `x-file-storage-spring` 但 BOM 无属性、无 dependencyManagement 条目，导致 reactor 构建直接失败。已补 `<x-file-storage.version>2.3.0</x-file-storage.version>` 及托管条目，全模块恢复编译通过。

## 核心数字

- 模块总数：**23**（1 BOM + 16 framework 模块（含 common）+ 5 业务模块 + 1 聚合启动模块）
- Java 文件：约 **500** 个，约 **28,800** 行（其中 union 占 309 个 / 16,700 行，framework 层约 7,200 行）
- 技术栈基线：Java 21 + Spring Boot **4.1.0** + MyBatis-Flex 1.11.8 + Sa-Token 1.45.0
