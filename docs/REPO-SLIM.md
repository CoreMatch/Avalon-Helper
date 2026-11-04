# REPO-SLIM: 以 Helper 编译/运行闭包为判据的仓库瘦身（最终版）

日期: 2026-11-04 ～ 2026-11-05 · 基线: `master` @ `b0fbccfa`（第一轮前）
两轮执行：第一轮 `89dc0d0a`（保守档），第二轮激进档 `0e82df79…a7cddd6c`（本次）。

仓库现状：上游 Kotlin 只保留一个职责——**给 Go Server 提供游戏内核（Helper）**。
`Go-Server/` 不引用任何 Kotlin 源码（仅运行时经 `gameHelperPath` 拉起 Helper.jar），两轮瘦身均未改动 Go-Server 一行。

## 判定方法（三路交叉 + 运行时实测）

1. **Gradle 模块图**：`:Helper → :Server-Core → { :ASM-Framework, :TimeTaskQuartz(已除) }`。
2. **源码引用**：import（含 Java 分号尾/通配符）+ 全限定类名 + 同包类型名，类级不动点。
3. **字符串/反射引用**：全文扫描点号与斜杠两种类名形式；另按 `HessClassPathProperties`
   （`headlessPath=…plugin.internal.headless.inject`、`fastASMClassPath`、`GameHessPath=com.corrodinggames.rts`）
   把装载链按包前缀整体保留；`HelperMain` 反射点（`PluginManage.pluginData` 字段）单独核对。
4. **运行时实测**：`-Xlog:class+load` 跑真实对局收集实际加载类：第一轮 230 个，
   第二轮剪枝后复测 **224 个**（relay 协议类、Quartz 均确认不再加载）。

## 第一轮（保守档，merge `89dc0d0a`）

- 删：`Server-All/`、`Java-Doc/`、`UPList/` 3 个模块目录；Server-Core 内 12 个零引用文件；
  失效 CI（Server-All workflow）与 4 份失效构建说明（`8b5cceb4`/`8db946e5`/`99e0fef2`）。
- 失败记录：首次尝试删 44 文件，`compileJava` 失败（`ReliableSocket.java` 通配 import 依赖
  `rudp/impl/Timer.kt`，扫描漏边），整段回滚，改保守规则后重删。

## 第二轮（激进档，逐批提交 + 逐批四项验证）

四项验证 = 干净克隆 `./gradlew :Helper:jar`（JDK17）→ `go build/vet/test ./...`（49 包）
→ `TestRealHelperV2Ext` → `TestE2ERealGameToSettlement`（均用本批新构建 jar，`-count=1`）。

| 批 | 提交 | 内容 | 验证 |
| --- | --- | --- | --- |
| 1 | `0e82df79` | 枢纽解引用 5 处（`Initialization` 资源锚点 `Main::class`→`Initialization::class`；`Data.vote` 字段、`ClientCommands` /vote 注册、`GameVersionServer` 聊天投票分发、`ServerRoom` stopVote），删 `Main.kt` + `command/` 全链 6 文件 | 全绿（9.95s/13.00s） |
| 2 | `92b27bea` | 剪 `PluginManage→PluginsLoad`（init 置空表、删 addPluginClass 重载；`pluginData` 字段与 run* 方法保留——HelperMain 反射依赖），删 `custom/`、`plugin/beta/**` 17 文件、`plugin/center`、`PluginsLoad`、`JavaScriptPluginGlobalContext`、`GetVersion`、`moreheadless`、`Server-Core/src/test/**` 24 文件，共 48 删 | 全绿（9.29s/12.41s） |
| 3 | `ede1145c` | 剪 `Initialization.loadService` 的 4 项 relay 注册与 `initGetServerData` 统计信标（relay.der.kim）；保留 `RwHps`/`FakeRwHps`/`GameVersionPacket`/`PacketType` 注册（`CustomServerSocket.run()` 运行时经 `IRwHpsManage` 回退取 `RwHps`，类载日志证实）。**失败记录**：首次编辑误将文件尾部 `init{}` 块切掉，helper 启动 NPE（i18NBundleMap 未初始化），恢复 init 块后通过 | 全绿（9.44s/12.92s） |
| 4 | `78ecd98e` | 移除 `TimeTaskQuartz/` 模块与 org.quartz 依赖：新增 `core/thread/SimpleTimeTask.kt`（ScheduledExecutorService 等价替身，API/语义对齐 TimeTaskManage），`Threads.kt` 切换；连带删 `Initialization.loadLib`（唯一调用方 Main.kt 已删）、`LibraryManager.kt`、`LibraryManagerError.kt`、settings include 与 Dokka 条目。**失败记录**：漏删 `Server-Core/build.gradle.kts` 的 `project(":TimeTaskQuartz")`，配置期报错，补删后通过 | 全绿（10.62s/13.10s） |
| 5 | `4e7182f4` | 删 `inject/ex/FFA_X.kt`。三重验证：全仓零引用；game-lib.jar strings 扫描 0 处按名引用；真实对局类载 0 次加载（该类为上游 @DidNotFinish "ONLY Test" 实验，触发接线本就不存在） | 全绿（10.30s/13.05s） |
| 6 | `a7cddd6c` | 新 CI `.github/workflows/helper.yml`：JDK17 构建 `:Helper:jar` + 产物上传，不引入发布流水线 | 全绿（9.75s/12.86s） |

## 保留红线（未动）

`Go-Server/`（一行未动）、`Helper/` 源码、`Server-Core` headless 装载链
（`GameModularReusableLoadClass`/`GameStartInit`/`HeadlessProxyClass`/`HessMain` 链）、
`dependent/redirections/**`、`ASM-Framework/`、`Server-Core/libs/*.jar`（含闭源 game-lib.jar）、
`data/gameData/`、`plugin/internal/headless/**`（内核注入链，装载链按包前缀复制）、
`com/corrodinggames/**` 引擎桥接补丁（被 GoBridge/LinkGameNet 等引用）。

## 剩余待定 / 雷区（不再继续删的原因）

| 区域 | 钉住它的运行时事实 | 结论 |
| --- | --- | --- |
| relay 类簇（`TypeRelay`/`GameVersionRelay`/`RelayRoom`/`internal/relay` 等 ~10 文件） | `NetStaticData` 是 Kotlin `object`，字段 `relayRoom = RelayRoom(...)` 在对象初始化时即构造；`ServerCommands`（inject 链，运行时加载）经 `NetStaticData.RwHps` 触达。类载日志显示 `GameVersionRelay` 等仍随对象初始化加载 | 删除需重写 `NetStaticData`（运行时对象手术），风险 ≫ 收益（~80K），保留 |
| `net/` 其余（NetService/NetStaticData/netconnectprotocol.realize 等） | `CustomServerSocket`（引擎桥接）运行时经 `IRwHpsManage→RwHps→GameVersionPacket` 取用；`GameVersionServer` 用 `NetStaticData.RwHps.abstractNetPacket` | 真实运行时依赖，保留 |
| `game/`、`io/`、`util/`、`struct/`、`func/` 中未加载部分 | 被上述运行时链与 `Data.kt`（god object）静态引用；同包引用无法静态穷尽 | 保守保留；再删需逐类运行时验证 |
| `resources`（ip2location.7z 7.5M、HZK16 等） | 被保留类 `IPCountry`/`Font16` 静态引用（虽然 helper 路径少用） | 保留；若作者接受"保留类但删其冷资源"可另议 |

## 最终体积对比

| 指标 | 原始 `b0fbccfa` | 第一轮后 `89dc0d0a` | 最终 | 变化（累计） |
| --- | --- | --- | --- | --- |
| `git archive` 字节 | 33,546,240 | 33,495,040 | **33,198,080** | **−348,160 B（−1.04%）** |
| Kt/Java 行数 | 45,599 | 45,028 | **39,120** | **−6,479 行（−14.2%）** |
| Kt/Java 文件数 | 521 | 509 | **450** | **−71 个（−13.6%）** |
| Gradle 模块 | 6 | 5 | **4**（Helper/Server-Core/ASM-Framework/buildSrc 工具） | −2（Server-All、Java-Doc、UPList、TimeTaskQuartz 除名） |

**剩余体积构成**（字节数变化小的原因）：`data/gameData` 9.7M（helper 运行必需）、
`Server-Core/libs` 4.2M（闭源引擎 jar）、`Server-Core/src/main/resources` ≈14M
（libs.zip 6.4M 引擎装载源 + ip2location.7z 7.5M + 字体/语言包），以上 ≈28M 全部在保留红线内；
Go-Server 2.2M 不动。可删的源码部分已从 45.6K 行降到 39.1K 行，上游"人不人鬼不鬼"的
发行面（Server-All 入口、插件生态、命令系统、relay 服务端、CI/文档）已全部拆除。
