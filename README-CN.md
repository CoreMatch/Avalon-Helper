# Avalon-Helper

[Avalon](https://github.com/CoreMatch/Avalon) 的服务端游戏引擎 helper：把闭源
Rusted Warfare 游戏引擎承载为独立 JVM 进程，经 stdin/stdout 线协议
（v2 / v2-ext）接受 Avalon 的驱动。

[English version / 英文介绍](README.md) **或** [Chinese version / 中文介绍](README-CN.md)

## 职责

本仓库是引擎壳，不是独立运行的游戏服务端：

| 模块 | 语言 | 职责 |
| --- | --- | --- |
| `Helper/` | Kotlin | 协议入口：`HelperMain` 协议主循环 + `GoBridge` 引擎桥接，v2 / v2-ext 全部操作 |
| `Server-Core/` | Java/Kotlin | 引擎装载链与桥接类（在 helper 的 JVM 内被加载） |
| `ASM-Framework/` | Kotlin | 字节码改写框架，装载期修补闭源引擎 |

运行时主体（网关 / 房间 / 协议 / 控制台 / HTTP）在 Avalon 仓库；Avalon 经
`gameHelperPath` 拉起本进程，本进程经线协议接受驱动并回传游戏事件与包。
不配置 helper 时 Avalon 仍可完整启动，只是无法真实开局。

## 线协议

契约在 Avalon 仓库：`docs/HELPER-PROTOCOL.md`（权威实现
`internal/game/external/protocol.go`）。本仓库只有 helper 侧实现，不存第二份。

## 构建

- **JDK 17**（与 CI 一致；Gradle 8.6 运行上限为 JDK 21，Kotlin 2.4.21 编译器本身兼容更新）

```bash
# 产物: Helper/build/libs/avalon-helper.jar (fat jar, 不入库)
JAVA_HOME=<jdk17> ./gradlew :Helper:jar
```

## 被 Avalon 拉起

Avalon 以 `gameHelperPath` 指向 `avalon-helper.jar`，并用
`-Davalonhelper.dataPath` 指定 helper 的数据目录（其下 `data/gameData` 为引擎
所需游戏数据，随本仓库提供）。运行配置、分发包与验收见 Avalon 仓库
`README.md` 与 `docs/DISTRIBUTION.md`。

## 许可证

GNU AGPLv3（见 [`LICENSE`](LICENSE)）。本程序源自上游 RW-HPS 项目；按 AGPL
要求，源文件头部保留上游版权声明。
