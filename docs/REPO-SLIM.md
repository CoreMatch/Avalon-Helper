# REPO-SLIM: 以 Helper 编译/运行闭包为判据的仓库瘦身清单

日期: 2026-11-04 · 分支: `chore/repo-slim` · 基线: `master` @ `b0fbccfa`

## 判定方法（三路交叉 + 运行时实测）

1. **Gradle 模块图**: `settings.gradle.kts` include 与各模块 `project(...)` 依赖，从 `:Helper` 反向求闭包。
   `:Helper → :Server-Core → { :TimeTaskQuartz, :ASM-Framework }`；`:Server-All → :Server-Core`（消费者，闭包外）；`:Java-Doc` 独立（Dokka 输出桩）。
2. **源码引用**: Kotlin/Java import（含通配符）+ 全限定类名，类级不动点迭代（含同包类型名扫描，注释已剥离）。
3. **字符串/反射引用**: 全文扫描点号 `net.rwhps.server.X` 与斜杠 `net/rwhps/server/X` 两种形式（覆盖 `Class.forName`、ASM 重定向目标、装载链字符串类名）。
4. **运行时实测**: 用 JDK17 以 `-Xlog:class+load=info:file=...` 跑通一整局真实对局
   （hello→room.create→addai×2→room.start[Crossing Large (10p)]→removeplayer→game.result，引擎 v1.15 产出回放），
   收集实际加载的 `net.rwhps.server.*` 类：**230 个**（util 88 / game 36 / plugin 24 / net 21 / dependent 19 / io 11 / data 11 / struct 6 / func 5 / core 5 / helper 3 / Main 1；**command 0 个**）。

**保留集 = 运行时加载类的静态编译闭包**（Server-Core 必须整体可编译，被保留类静态引用的文件不可删）。
静态闭包分析脚本与运行时类载日志为本次临时产物（仓库外 `/tmp/closure_class.py`、`/tmp/helper-classload.log`）。

## 一、保留清单

| 对象 | 理由 |
| --- | --- |
| `Go-Server/`（全部） | 下游主体，一行不动 |
| `Helper/`（4 个源文件 + build） | 闭包根 |
| `Server-Core/` 中 404 个源文件 | 运行时类的静态编译闭包（见"待定"节对其中枢纽的说明） |
| `Server-Core/libs/*.jar`（game-lib 2.6M / slick 0.7M / wasm 1.0M） | 引擎与编译期依赖，随仓库入库 |
| `Server-Core/src/main/resources/`（libs.zip 6.4M、ip2location.7z 7.5M、HZK16、bundles、eula、maven 清单等） | `GameStartInit` 运行时从 libs.zip 装载引擎；ip2location.7z/HZK16 被保留类 `IPCountry`/`Font16` 引用 |
| `ASM-Framework/` | Server-Core 编译依赖（ASM 重定向框架） |
| `TimeTaskQuartz/` | `core/thread/Threads.kt`（运行时闭包内）import `net.rwhps.timetask.task.TimeTaskManage` —— 编译闭包内（见待定 D-2） |
| `data/gameData/` | helper 运行必需游戏数据 |
| `buildSrc/`、`gradle/`、`gradlew*` | 构建自身 |
| `docs/` | 文档（含本文件） |

## 二、删除清单（本次执行）

**模块/目录（3 个，均在闭包外）**

| 目录 | 体积 | 理由 |
| --- | --- | --- |
| `Server-All/` | 4K（仅 build 桩） | 上游独立发行入口模块，消费 :Server-Core；Go 形态不再从它出包 |
| `Java-Doc/` | 4K（仅 build 桩） | Dokka 输出目录模块，无源码 |
| `UPList/` | 8K（README + 白名单 json） | 上游插件残留资料；功能已由 Go-Server 内建（`internal/uplist`），且目录不在 settings.gradle |

**Server-Core 内 12 个零引用文件**（静态闭包外：无 import、无 FQN/字符串引用、无同包类型名引用；运行时类载日志中亦未出现）：

```
game/headless/README.kt                    net/api/WebGetRelayInfo.kt
net/debug/SafetyConnect.kt                 plugin/beta/banunit/BanData.kt
plugin/beta/banunit/BanUnitMain.kt         test/MapLoad.kt
test/Relay.kt                              util/annotations/ioc/Plugin.kt
util/annotations/ioc/ServiceProtocol.kt    util/internal/kotlin/SuppressProMax.kt
util/privately/BundlesCheck.kt             util/privately/FakeHessRes.kt
```

> 方法论备注：首轮按"可解析边"闭包删了 44 个文件，`:Server-Core:compileJava` 即失败
> （`ReliableSocket.java` 经通配 import 使用被删的 `rudp/impl/Timer.kt`——Java 分号
> 尾与通配 import 曾使静态扫描漏边），已整段回滚。随后改为**保守规则**：凡引用无法
> 精确解析到类（顶层函数/扩展函数/通配 import 等），其目标包整包保留；并按
> `HessClassPathProperties.headlessPath` 把 `plugin/internal/headless/inject/**`
> 全部划入装载链保留区（引擎按包前缀整体复制进游戏 classloader，`inject/ex/FFA_X.kt`
> 因此从删除集移入待定）。保守规则下的删除集即上述 12 个文件。

**配套清理**

- `settings.gradle.kts`: 移除 `include(":Java-Doc")`、`include(":Server-All")` 及注释掉的历史 include。
- 根 `build.gradle.kts`: Dokka 多模块输出路径 `Java-Doc/pages/snapshot` 改为标准 `build/dokka`。
- `.github/workflows/gradle.yml`: 删除。该 workflow 唯一产物是 `Server-All/build/libs/*.jar`，随 Server-All 删除而失效（且 `continue-on-error: true` + JDK21 与本项目 JDK17 要求不符，早已名存实亡）。

**明确不删（分析曾误报）**: `Server-Core/src/main/java/com/corrodinggames/rts/gameFramework/j/{CustomServerSocket,NetEnginePackaging}.kt` —— 位于引擎命名空间，被 `GoBridge`/`LinkGameNet`/`PlayerConnectX` 等闭包内文件引用，是引擎桥接补丁，必须保留。

## 三、待定清单（静态可达、运行时未载入；删除需先做小幅枢纽解引用，等作者决策）

运行时只加载 230 个类，但以下区域被少数**枢纽类**静态引用，"只删不改"会破坏 Server-Core 编译，故本次不删：

| 待定区域 | 钉住它的枢纽（静态引用点） | 运行时证据 | 删除所需改动 | 风险 |
| --- | --- | --- | --- | --- |
| `plugin/internal/headless/inject/ex/FFA_X.kt` | 无静态引用，但 `GameStartInit` 按 `headlessPath` 前缀把 `inject/**` 整体复制进游戏 classloader | 本局（非 FFA）未加载；FFA 局是否被引擎按名加载无法静态排除 | 需 FFA 模式真实对局验证后再定 | 中（装载链按包前缀复制，静态扫描覆盖不到） |
| `command/`（6 文件）+ `command/ex/Vote.kt` 链 | `Main.kt`（import 三个命令类）；`Data.kt`→`Vote`；`plugin/internal/headless/inject/command/ClientCommands.kt`→`Vote` | **0 个类被加载**（helper 自建 CommandHandler，只注册 startnetservice） | 删 `Main.kt` 需先改 `Initialization.kt` 的 `Main::class.java.getResourceAsStream`（资源锚点，换成任意保留类即可）；去 `Data`/`ClientCommands` 的 Vote 引用 | 低：运行时零加载；建议验证=四项验证全跑 |
| `plugin/` 生态（beta/center/GetVersion/PluginsLoad 等 ~20 文件，**不含 internal/headless**） | `util/file/plugin/PluginManage.kt`（HelperMain 反射写其 pluginData 字段，必留）→`PluginsLoad`；`Main.kt`→`PluginCenter`；`custom/LoadCoreCustomPlugin.kt`→UpListMain/PlayerBindMain | 加载的 24 个 plugin 类全部位于 `plugin.internal.headless.*`（内核注入链）与 PluginManage 基础类；**beta 生态 0 加载** | 剪 PluginManage 对 PluginsLoad 的引用、删 LoadCoreCustomPlugin（仅 Main.kt 引用） | 中：PluginManage 被 HelperMain 反射使用，改后需跑真实内核测试 |
| `net/` 大部分（~50 文件保留中运行时未载的部分） | `Initialization.loadService`（注册 TypeRelay/RwHps/FakeRwHps 等协议实现）、`NetStaticData`、`Main.kt`→NetService | 运行时仅载 21 个 net 类（ConnectionAgreement 补丁链 + 包序列化基础） | 剪 loadService 注册与 NetStaticData 字段 | 中高：协议注册表是反射取用（ServiceLoader），属"静态扫描覆盖不到"，保守待定 |
| `TimeTaskQuartz/` 模块 | `Threads.kt` import `TimeTaskManage` | helper 仅用 `Threads.newThreadCore`，未走定时任务路径 | 改 Threads.kt（去定时任务分支） | 中 |
| `game/`、`io/`、`util/`、`struct/`、`func/` 中保留但未加载的部分 | 由上述枢纽及 `Data.kt`（god object）扇出 | 运行时 game 36 / io 11 / util 88 类被加载，其余未载 | 需先解除枢纽 | — |

**建议验证方式（作者批准后）**: 每剪一个枢纽 → `./gradlew :Helper:jar`（JDK17）→ 真实内核 `TestRealHelperV2Ext` → 端到端 `TestE2ERealGameToSettlement`，失败即回滚该步。

## 四、本次瘦身体量

删除合计：3 个模块目录（16K）+ 12 个源文件（≈80K，2100+ 行）+ 失效 CI 与 4 份失效构建说明；仓库总体积 35M 中大头为 `data/gameData`（9.7M，必留）、`Server-Core/libs`（4.2M，必留）、`Server-Core/src/main/resources`（≈14M，其中 libs.zip/ip2location.7z 被保留类引用）。待定区（含首轮曾删、因保守规则恢复的 32 个文件）若获批枢纽解引用，可再减 ≈1.2M 源码。
