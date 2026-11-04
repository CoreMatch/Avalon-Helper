<div align="center">
<h1>RW-HPS Project Unofficial</h1>

这是一个非官方仓库。由于原仓库已被删除，因此这是一个备份。

**RW-HPS** 是一个 **Rusted Warfare**（铁锈战争）游戏服务端（目标客户端 **1.15** / 协议号 **176**）

_**个人项目，仅供学习**_
</div>

[English version / 英文介绍](README.md) **或** [Chinese version / 中文介绍](README-CN.md)

## 当前形态

运行时主体是 **Go**；Java/Kotlin 侧只承担"承载闭源游戏引擎的壳"。
共 4 个模块（另有 `buildSrc` 为 Gradle 构建工具，不算产品模块）：

| 模块 | 语言 | 职责 |
| --- | --- | --- |
| `Go-Server/` | Go | **运行时主体**：网关、房间、玩家与协议处理、控制台、HTTP API、Cascade 对接 |
| `Helper/` | Kotlin | 引擎壳：把上游 headless 内核封装为独立 Java 进程，经 stdin/stdout 线协议（v2 / v2-ext）接受 Go 侧驱动 |
| `Server-Core/` | Java/Kotlin | 上游服务端实体与引擎装载链（在 helper 的 JVM 内被加载） |
| `ASM-Framework/` | Kotlin | 字节码改写框架，装载期修补闭源引擎 |

Go 无法执行 Java 字节码，因此游戏模拟内核外置为 helper 进程；两者经
`Go-Server/docs/HELPER-PROTOCOL.md` 定义的线协议解耦。不配置 helper 时
Go 服务端仍可完整启动（控制台/HTTP/房间管理），只是无法真实开局。
细节见 [`Go-Server/README.md`](Go-Server/README.md)。

## 构建

- **Go 1.22+**（Go 主体）
- **JDK 17**（构建引擎壳；引擎与 Kotlin 运行时按 JDK 17 验证）

```bash
# 1. 引擎壳 (产物 Helper/build/libs/Helper.jar)
JAVA_HOME=<jdk17> ./gradlew :Helper:jar

# 2. Go 主体 (产物 Go-Server/rwhps)
cd Go-Server
go build -o rwhps ./cmd/rwhps
```

提交前校验（必须全绿）：

```bash
cd Go-Server
go build ./...
go vet ./...
go test ./... -count=1    # 含真实 helper 集成测试 (自动发现 Helper.jar 与 JDK 17, 缺失则跳过)
```

## 运行

推荐分发包形态（干净目录自足，不依赖仓库环境）：

```bash
./dist/build-dist.sh      # 产出 dist/out/rwhps-<hash>-linux-<arch>.tar.gz
```

包内容、启动步骤、配置键与升级方式见 [`docs/DISTRIBUTION.md`](docs/DISTRIBUTION.md)。

运行要求：Linux + **JRE/JDK 17** 的 `java` 在 `PATH` 中（或在配置里显式指定
`javaPath`）。首次启动有 EULA 确认，随后自动加载游戏内核并监听配置端口
（默认 5123）。

## 文档

- [`docs/DISTRIBUTION.md`](docs/DISTRIBUTION.md) —— 分发包内容与升级方式
- [`Go-Server/docs/HELPER-PROTOCOL.md`](Go-Server/docs/HELPER-PROTOCOL.md) —— Go ↔ helper 线协议（v2 / v2-ext）
- [`docs/REPO-SLIM.md`](docs/REPO-SLIM.md) —— 仓库瘦身记录与"当前不可再删项"护栏
- [`docs/BASELINE.md`](docs/BASELINE.md) —— 回归基线（E2E / 启动 / 内存 / 类数）

## 声明

- RW-HPS 是免费开源项目，仅供学习用途，请勿用于任何非法用途。
- 一切开发旨在学习，请勿用于非法用途。

## 许可证

[AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html)（见 [`LICENSE`](LICENSE)），
并沿用原项目 EULA（CC BY-NC-SA 4.0，非商业使用）。
