<div align="center">
<h1>RW-HPS Project Unofficial</h1>

这是一个非官方仓库。由于原仓库已被删除，因此这是一个备份。

**RW-HPS** 是一个 **Rusted Warfare**（铁锈战争）游戏服务端（目标客户端 **1.15** / 协议号 **176**）

_**个人项目，仅供学习**_
</div>

[English version / 英文介绍](README.md) **或** [Chinese version / 中文介绍](README-CN.md)

## 当前形态

本仓库承载 **Java/Kotlin 引擎壳**：承载闭源游戏引擎，把它作为独立 Java 进程
（helper）提供给 Go 运行时主体
[Avalon](https://github.com/CoreMatch/Avalon)，经 stdin/stdout 线协议
（v2 / v2-ext）接受 Go 侧驱动。共 3 个模块（另有 `buildSrc` 为 Gradle 构建
工具，不算产品模块）：

| 模块 | 语言 | 职责 |
| --- | --- | --- |
| `Helper/` | Kotlin | 引擎壳：把上游 headless 内核封装为独立 Java 进程，经 stdin/stdout 线协议（v2 / v2-ext）接受 Go 侧驱动 |
| `Server-Core/` | Java/Kotlin | 上游服务端实体与引擎装载链（在 helper 的 JVM 内被加载） |
| `ASM-Framework/` | Kotlin | 字节码改写框架，装载期修补闭源引擎 |

线协议定义、运行形态与配置见 Avalon 仓库（`docs/HELPER-PROTOCOL.md` 与
`docs/DISTRIBUTION.md`）。`data/gameData` 为引擎所需游戏数据，随本仓库提供。

## 构建

- **JDK 17**（构建引擎壳；引擎与 Kotlin 运行时按 JDK 17 验证）

```bash
# 产物 Helper/build/libs/Helper.jar
JAVA_HOME=<jdk17> ./gradlew :Helper:jar
```

## 运行

helper 不是独立运行的服务端：它由 Avalon 的 Go 服务端经 `gameHelperPath`
配置拉起（配置接入与分发包见 Avalon 仓库 `README.md` 与
`docs/DISTRIBUTION.md`）。

## 文档

- [`docs/REPO-SLIM.md`](docs/REPO-SLIM.md) —— 仓库瘦身记录与"当前不可再删项"护栏
- 线协议合同：Avalon 仓库 `docs/HELPER-PROTOCOL.md`
- 分发与运行：Avalon 仓库 `docs/DISTRIBUTION.md`、`docs/BASELINE.md`

## 声明

- RW-HPS 是免费开源项目，仅供学习用途，请勿用于任何非法用途。
- 一切开发旨在学习，请勿用于非法用途。

## 许可证

[AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html)（见 [`LICENSE`](LICENSE)），
并沿用原项目 EULA（CC BY-NC-SA 4.0，非商业使用）。
