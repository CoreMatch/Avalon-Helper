<div align="center">
  <h1>Avalon-Helper</h1>
  <p><strong>Avalon 真正对局背后的隔离 JVM。</strong></p>
  <p><a href="./README.md">English</a></p>
</div>

Avalon-Helper 是 Avalon 与闭源 Rusted Warfare 引擎之间的轻量 stdin/stdout JSON 桥接器。它面向需要真实模拟与清晰进程边界的服务端运维者和集成者。

## 为什么选择 Avalon-Helper

- **保持通信输出纯净。** 协议 stdout 只承载 JSON，引擎输出另行隔离。
- **遵循当前契约。** 严格的 v4 握手会在模拟启动前拒绝过时协议版本。
- **复用现有引擎。** 既有引擎装载与 ASM 集成保留在 helper JVM 内。
- **隔离崩溃。** 引擎故障不会蔓延到 Avalon 的 Go 进程，也不会拖垮宿主进程。
- **无需重写即可集成。** Avalon 通过狭窄的桥接层发送模拟工作并接收游戏事件。

## 进一步了解

- [Avalon](https://github.com/CoreMatch/Avalon) — 驱动 helper 的服务端运行时
- [Avalon 运维文档](https://github.com/CoreMatch/Avalon/blob/main/docs/OPERATIONS.md) — 面向运维者的集成指南
- [HA 契约](https://github.com/CoreMatch/HA-Contract) — 生态权威契约
- [谱系记录](docs/LINEAGE-COPYCAT.md) — 衍生来源与上游对照

## 使用边界

Avalon-Helper 不是独立服务端，不会开源游戏引擎，也不支持任意引擎版本。

## 许可证与来源

[AGPL-3.0](LICENSE) · 衍生自 [RW-HPS](https://github.com/RW-HPS/RW-HPS)，保留上游版权声明。
