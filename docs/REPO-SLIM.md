# REPO-SLIM: 以 Helper 编译/运行闭包为判据的仓库瘦身（最终版）

日期: 2026-11-04 ～ 2026-11-05 · 基线: `master` @ `b0fbccfa`（第一轮前）
六轮执行：第一轮 `89dc0d0a`（保守档），第二轮激进档 `0e82df79…a7cddd6c`，
第三轮 IP 定位整链 `2a096b08…007e1f10`，第四轮方案甲闭包续删 + netty 排除
`589ce2b6…350a00b2`，枢纽手术试验 `8450dcda`/`a477d340`，
第五轮 relay 簇删除 + 枢纽收窄 `81bbc009…b8815069`，
第六轮阶段二收官（Initialization 精简 + 依赖处置 + 机械项）
`8f0e16ef…1f61920a`（本次）。

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
| ~~relay 类簇~~ | **第五轮已删除**（`81bbc009`/`9fcb4e21`）：试验二清障后纯静态手术，11 文件删 9（`RelayServerType.kt` 因 inject 活分发触达保留），类载交叉 0 加载 | 已清 |
| `net/` 其余（NetService/NetStaticData/netconnectprotocol.realize 等） | `CustomServerSocket`（引擎桥接）运行时直接构造 `RwHps` 并直赋 `NetStaticData.RwHps`（第五轮批3 后，`IRwHpsManage` 注册表已删）→ `GameVersionPacket` | 真实运行时依赖，保留 |
| `core/Application.kt` + `core/NetServer.kt` | `Data.kt:123` god object 字段 `val core = Application()`。枢纽试验三分析：20 处活读取面（admin/settings/serverHessUuid/http），惰性可行但推迟窗口≈0 且不解锁删除 | 保留；删除 = 重写 20 处读取点 + 替代 Administration/PluginData/HttpRequestManage，属大规模重写，建议阶段二最后做或不做 |
| `game/`、`io/`、`util/`、`struct/`、`func/` 中未加载部分 | 被上述运行时链与 `Data.kt`（god object）静态引用；同包引用无法静态穷尽 | 保守保留；再删需逐类运行时验证。第五轮闭包复扫（类载并集种子）后闭包外剩 8 文件，全部位于保守区：`FastIntValueClass`（ASM 重定向）、`SyncByteArrayChannel`/`DynamicPrintStream`（io）、`IntSeq`/`SeqSave`/`LongIntMap`/`IntMap`/`ObjectIntMap`（struct 容器，后两者为 relay 删除后新暴露的零引用零加载类，按区纪律保留并记录） |
| `resources`（HZK16/ASC16 字体、libs.zip 等） | 被保留类 `Font16` 与引擎装载链静态引用 | 保留（ip2location.7z 7.5M 已于第三轮删除） |

## 最终体积对比

| 指标 | 原始 `b0fbccfa` | 第一轮后 `89dc0d0a` | 第三轮后 `ed92ff0f` | 第四轮后 `5c660dd3` | 第五轮后 `baabca44` | 最终（第六轮） | 变化（累计） |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `git archive` 字节 | 33,546,240 | 33,495,040 | 25,344,000 | 25,180,160 | 25,077,760 | **25,036,800** | **−8,509,440 B（−25.4%）** |
| Kt/Java 行数 | 45,599 | 45,028 | 38,847 | 35,628 | 32,972 | **32,666** | **−12,933 行（−28.4%）** |
| Kt/Java 文件数 | 521 | 509 | 446 | 396 | 382 | **380** | **−141 个（−27.1%）** |
| Helper.jar 产物 | —（无此产物） | — | 28,665,640 | 25,338,175 | 25,263,098 | **23,307,355** | **−5,358,285 B（−18.7%）** |
| Gradle 模块 | 6 | 5 | 4 | 4 | 4 | **4**（Helper/Server-Core/ASM-Framework/buildSrc 工具） | −2（Server-All、Java-Doc、UPList、TimeTaskQuartz 除名） |

**剩余体积构成**（字节数变化小的原因）：`data/gameData` 9.7M（helper 运行必需）、
`Server-Core/libs` 4.2M（闭源引擎 jar）、`Server-Core/src/main/resources` ≈6.6M
（libs.zip 6.4M 引擎装载源 + 字体/语言包；ip2location.7z 7.5M 已删），以上全部在保留红线内；
Go-Server 2.2M 不动。可删的源码部分已从 45.6K 行降到 35.6K 行，上游"人不人鬼不鬼"的
发行面（Server-All 入口、插件生态、命令系统、relay 服务端、CI/文档）已全部拆除。

## 第三轮（2026-11-05）：彻底删除 IP 定位功能（作者决定）

作者判定："Go Server 用不上的直接删掉就行了"——不做空实现占位，整链移除。

**为什么安全**：Go+helper 形态下所有连接经 GoBridge 以回环地址 `127.0.0.1` 建立
（HelperMain.openConn），国家查询输出恒为空值；回放档案侧全仓 grep 确认 `IPCount`
只写无读（`Event.kt` 仅把 `infoObject` 整体序列化进档案/回放素材，无反序列化必填
依赖），删除不影响回放生成与读取。Go 侧该链自移植起即空占位（`internal/util/ip`
注释 "Q7=C 暂不实现"），唯一生产落点 `player.InfoObject` 的 `IPCount` 亦无读取方，
`GetAllRelayIpCountry` 仅测试调用。

**删除清单**（提交 `2a096b08` Kotlin 侧 / `bee634c7` Go 侧）：
- `util/IPCountry.kt`、`resources/ip2location.7z`（7.5M，IP2Location LITE DB5）
- `net.renfei:ip2location` 依赖（build.gradle.kts + maven/Server-Core/implementation.txt）
- 两处 `ConnectionAgreement`（Server-Core + Helper 补丁）的 `ipCountry/ipCountryAll`
  字段与全部构造器赋值；`AbstractNetConnect` 透传属性
- `PlayerHess.infoObject` 与 Go `player.InfoObject` 的 `"IPCount"` 字段
- `RelayRoom.getAllRelayIpCountry`（零调用）与 Go `room.GetAllRelayIpCountry`（仅测试）
  及 `RelayConnect`/player 接口中的 `IPCountry()`、7 处测试 mock
- `Initialization.loadIpBin`（上游早注释的空壳）与 `BeanCoreConfig.ipCheckMultiLanguageSupport`
- Go `internal/util/ip/ipcountry.go`、`agreement.go` 字段与赋值、`connect.go` 方法

**验证**（当批新 jar，`-count=1`）：干净克隆 `:Helper:jar` 成功（产物 37.5M→**28.7M**，
−8.8M）；`go build/vet/test` 49 包全绿；`TestRealHelperV2Ext` PASS 8.63s；
`TestE2ERealGameToSettlement` PASS 11.72s——**无 IP 库正常开局与结算**：
两客户端+两 AI 真实入房，`结算: 获胜=[AI - Hard] 地图="Crossing Large (10p)"
回放="…[v1.15] (5 Nov 2026 01.19.31).replay" 时长=8s`，回放文件正常生成。

归零确认：`grep -rn "IPCountry|ip2location|ip2region|net.renfei|ipCountry|IPCount"`
于 Server-Core/Helper/Go-Server 源码均为空。

## 第四轮（2026-11-05）：方案甲执行——类级闭包续删 + netty 打包排除

决策报告 `RW-HPS-JAVA-SHELL-DECISION.md` 批准"方案甲第一步"。基线 `ed92ff0f`。

**判定方法（类级闭包，三路交叉 + 运行时种子）**：
- 种子 = Helper 4 文件 + 运行时类载日志 224 类（第三轮后实测）+ `inject/**`
  保守区 22 文件（其静态依赖一并保留）；
- 边 = import（含通配/成员）+ 全文 FQN（点号/斜杠两种形式，覆盖字符串/反射）
  + 同包类型名；注释剥离；
- 保守规则：未解析包级边整包保留（`func`/`util.inline`/`util.file.plugin.value`/
  `rudp.impl`/`game.headless.core`/`netconnectprotocol.internal.server`）；
  `com/corrodinggames/**` 引擎桥接恒定保留；
- 结果：闭包内 351 / 闭包外 56 文件。56 文件逐一人工核对（引用者枚举、
  运行时清单交叉、资源与构建脚本 grep、Go 侧字符串 grep、ServiceLoader 注册点、
  活监听器事件覆盖）后：**50 文件可删，6 文件属保守保留区记跳过**。

**批次执行**（每批四项硬验证：干净克隆 JDK17 `:Helper:jar` → `go build/vet/test`
49 包 → `TestRealHelperV2Ext` → `TestE2ERealGameToSettlement`，当批新 jar、`-count=1`）：

| 批 | 提交 | 内容 | 验证（V2Ext / E2E） |
| --- | --- | --- | --- |
| A | `589ce2b6` | `Helper/build.gradle.kts` fat-jar 打包排除 `io/netty/**`（构建级，零源码改动）。netty 随 Server-Core `api` 5 模块传递进包：2,162 条目 / 6,849,149 B 未压缩；运行时类载 0 命中。jar 28,665,640 → 25,471,228（**−3,194,412 B**），`unzip -l` netty 条目 0 | 8.96s / 11.48s 全绿 |
| B | `bd632f12` | net 协议簇 5 文件 / 450 行：`TypeHttp`/`TypeRelay`/`TypeRelayRebroadcast`/`TypeRemoteControl`/`GameVersionRelayRebroadcast`。仅死岛互引；ServiceLoader 协议注册全显式（`Initialization.kt:146-151`、`inject/GameEngine.kt:118-119`），五者零注册；relay 能力已由 Go 侧自实现接管 | 8.91s / 12.19s 全绿 |
| C | `3730d349` | game/core/data/dependent 15 文件 / 925 行：`Event.kt`+`EventGlobal.kt`（旧独立服控制台遗留，全树零实例化）、5 个死事件类（活监听器不处理）、`GameExternalUnits`/`GameFactoryBuildUnits`/`TeamManage`/`TimeTaskData`/`BeanGithubReleasesApi`/`RCN`（零引用，RCN 仅字符串前缀）、`HotLoadClass`/`DynamicByteClassLoader` | 9.61s / 12.21s 全绿 |
| D | `350a00b2` | util 30 文件 / 1,844 行 + `Sensitive_Thesaurus.txt`（2,156 行，BadWord.java 数据）：algorithms 4、compression 5、console 2、classload/file/plugin/game/log/alone/annotations 等，全部零引用或死岛互引 | 9.04s / 12.00s 全绿 |

四批 E2E 结算证据一致：`结算: 获胜=[AI - Hard] 地图="Crossing Large (10p)" … 时长=8s`，
回放文件正常生成；jar 内 `net/rwhps/server` 条目 762 → 695，死类全部归零。

**本轮数字**：删除 50 文件 / 3,219 行 + 资源 22,424 B；`git archive`
25,344,000 → 25,169,920（−174,080 B）；Helper.jar 28,665,640 → 25,337,804
（−3,327,836 B，其中 netty 排除 −3,194,412 B、死类 −133,424 B）。

**跳过清单（闭包外但保守保留，供枢纽手术阶段参考）**：

| 文件 | 保留原因 |
| --- | --- |
| `dependent/redirections/fast/FastIntValueClass.kt` | ASM 重定向保留区（兄弟类经 `LwjglRedirections` 显式注册，本类零注册零加载，理论上可删，按红线不动） |
| `io/input/SyncByteArrayChannel.kt`、`io/output/DynamicPrintStream.kt` | io 序列化保留区（零引用零加载，按红线不动） |
| `struct/list/IntSeq.kt`、`struct/list/SeqSave.kt`、`struct/map/LongIntMap.kt` | struct 容器保留区（零引用零加载，按红线不动） |
| `core/Application.kt`、`core/NetServer.kt` | 闭包内——`Data.kt:123` god object 字段初始化钉住，属枢纽边界 |

**删到枢纽即停**：`Data.kt` god object、`NetStaticData` relay 簇（`RelayRoom`/
`GameVersionRelay`/`internal/relay`）、`CustomServerSocket→IRwHpsManage→RwHps→
GameVersionPacket` 运行时链、Helper/inject/ASM/libs.zip/io/game 核心/data/gameData
全部未动。闭包复跑终态：闭包外仅剩上述 6 个保守区文件。

## 枢纽手术可执行性试验（2026-11-05，决策报告 §C.2 阶段二预研）

三个试验、两条独立分支，纪律：四项硬验证全绿才合并。结果：**两试两过两合并，
无回滚**；枢纽手术可行，但收益形态与预想不同（解链容易，删文件仍受静态引用面约束）。

### 试验一（已合并）：`CustomServerSocket→IRwHpsManage→RwHps` 回退链 —— **可解开**

- 分支 `exp/hub-trial-1-direct-rwhps`：`8576c0bf` → merge `8450dcda`。
- 原链路：`CustomServerSocket.run()` → `IRwHpsManage.addIRwHps` → ServiceLoader
  查 `"ServerProtocol"`（**从无注册**，恒抛 `ImplementedException`）→ 回退 `"IRwHps"`
  → 反射构造 `RwHps`。异常驱动的确定性回退。
- 改法：调用点直接构造 `RwHps(classLoader, ServerProtocol)`，经新增
  `IRwHpsManage.registerIRwHps` 显式注册（同 key），`firstServer`/
  `NetStaticData.RwHps` 读取面不变，语义等价。
- 验证：干净克隆 `:Helper:jar` 13.7s（javap 确认新方法入 jar）；go build/vet/test
  49 包 22.2s 无 FAIL；V2Ext 9.79s；E2E 12.46s `结算: 获胜=[AI - Hard]`。
- **死文件：0**。`IRwHpsManage` 未失去调用方——`NetStaticData.firstServer`
  （lazy RwHps 提供者）与 `NetService` mix 分支（`mixProtocolEnable` 默认 false，
  运行期死、静态活）仍引用。`"IRwHps"→RwHps` 注册同理保留（回退路径静态引用）。
- 结论：回退链可解开且零风险，但**解链本身不产生可删文件**——收益是为后续
  手术把"实现类在哪决定"显式化。

### 试验二（已合并）：`NetStaticData` relay 簇 —— **可解，relay 簇实证纯占位**

- 分支 `exp/hub-trial-2-lazy-relayroom`：`7fb71ac2` → merge `a477d340`。
- 改法：`relayRoom` 从 object 字段即时构造改 `by lazy`（SYNCHRONIZED），保留
  "每次访问重置 `closeRoom=false`"原语义。代价 = 1 处属性。
- 验证：干净克隆 `:Helper:jar` 15.2s；go build/vet/test 49 包 20.5s 无 FAIL；
  V2Ext 8.40s；E2E 12.81s `结算: 获胜=[AI - Hard]`。
- **运行时证据**（新 jar + `JAVA_TOOL_OPTIONS=-Xlog:class+load` 复跑 E2E，
  `/tmp/exp2-classlog.log` 5,525 行）：`NetStaticData` **0 加载**（连对象本身都
  从未初始化）、`RelayRoom`/`GameVersionRelay`/`GroupNet` 0 加载；对照
  `CustomServerSocket` 4.027s、`RwHps` 0.49s 正常加载。
- 关键问题回答：**helper 形态下 relay 簇是纯占位**——唯一触达点
  `GameVersionRelay:306/:432`（relay 协议连接路径），而 relay 已由 Go 侧自实现
  接管（第四轮批B 删 `Type*` 时证实，`Go-Server/internal/net/netconnectprotocol/
  typerelay_test.go` 为 Go 自己的实现）。
- 结论：删 relay 簇**不再需要重写运行时对象**（object 初始化钉已解除）；剩余是
  静态引用面手术——移除 `relayRoom` 属性与 `GameVersionRelay` 触达点。按纪律
  本步不删簇。

### 试验三（仅分析，未动代码）：`Data.kt:123 val core = Application()` 惰性化

- 读取面：20 处 / 7 文件——`HelperMain`（settings×4）、`Initialization`
  （settings/http/exit/save）、`NetPacketRedirections:39`（`serverHessUuid`，
  ASM 重定向活路径）、inject `ServerCommands`（admin×7）、`GameVersionServer`
  （`admin.filterMessage`）、`LinkGameNet`（`serverConnectUuid`）、`PlayerManage`
  （admin×2）。Java 消费方 0（去 `@JvmField` 无碍）。
- 结论：**惰性技术可行**（1 行：`val core: Application by lazy { Application() }`），
  但①首次访问发生在启动期（HelperMain/Initialization 读 settings），推迟窗口≈0；
  ②不解锁删除——`Application` 成员（`admin`/`settings`/`serverHessUuid`/`http`）
  全是活运行面，删 `Application`/`NetServer` 需重写 20 处读取点并替代
  `Administration`/`PluginData`/`HttpRequestManage`，属大规模重写而非手术。
  `NetServer` 唯一触达点为 `Application.exit():84`。

### 阶段二输入（三试汇总）

| 枢纽 | 试验结论 | 下一步手术的实际障碍 |
| --- | --- | --- |
| `CustomServerSocket→IRwHpsManage→RwHps` | 已解开（`8450dcda`） | 删 `IRwHpsManage` 需先剪 `NetStaticData.firstServer` 与 `NetService` mix 分支静态引用 |
| `NetStaticData` relay 簇 | 惰性化通过（`a477d340`），簇纯占位 | 剩静态面：`relayRoom` 属性 + `GameVersionRelay` 触达点 + 簇内互引 |
| `Data.core`（Application/NetServer） | 惰性可行但收益≈0 | 20 处活读取面，删除=大规模重写，建议最后做或不做 |

## 第五轮（2026-11-05）：relay 簇删除 + 枢纽收窄（试验结论落地）

试验二清障后执行。四批全部通过四项硬验证 + 每批 `-Xlog:class+load` 交叉确认，
**零回滚**。边界纪律：`Data.kt`/`Application`/`NetServer` 一律未越界。

| 批 | 提交 | 内容 | 验证（V2Ext / E2E） | 类载交叉 |
| --- | --- | --- | --- | --- |
| 1 | `81bbc009` | relay 协议/房间簇：删 `GameVersionRelay`(1062 行)/`RelayRoom`(404)/`AbstractNetConnectRelay`/`PlayerRelay`/`RelayPacketCache`/`internal/relay` 2 件，共 9 文件 −1,921 行；剪引用：`NetStaticData.relayRoom`、`UniversalAnalysis.getPacketTeamData`、`PlayerHess.playerJumpsToAnotherServer`（零调用）、`DataPermissionStatus.RelayStatus`、inject 未用 import | 9.18s / 11.52s | 删类 0 加载 |
| 2 | `9fcb4e21` | `GroupNet` 广播池链：删 `net/GroupNet.kt`(112)；`AbstractNetConnect.close(groupNet?)`→`close()`；`ConnectionAgreement`×2（Server-Core+Helper 补丁）删 add/remove/close 的 groupNet 参数；`inject/GameVersionServer`、`inject/HessSocket` 调用点同步 | 8.63s / 11.45s | GroupNet 0 加载 |
| 3 | `b2faf0c9` | 枢纽收窄：`NetStaticData.RwHps` lazy{firstServer}→`@Volatile var` 直赋（`CustomServerSocket` 构造后直接赋值）；删零调用的 `checkServerStartNet`；`NetService` 删 `mixProtocolEnable` 分支；连带删 `StartMixProtocol.kt`、`Cache.iRwHpsCache`、`Initialization` 三处 `ServiceType.IRwHps` 注册；**删 `IRwHpsManage.kt`（失去全部调用方）** | 9.00s / 11.43s | IRwHpsManage 0 加载，直连链正常 |
| 4 | `b8815069` | 闭包复扫（类载并集种子 228 类）：删 `UniversalAnalysisOfGamePackages`(零引用, Go 已自实现)/`ParseException`/`GameOtherUtils`/`Rand`；`CallTimeTask` 删 12 死枚举项；`maven/*/{implementation,compileOnly}.txt` 4 件删除+gitignore（无读取方，构建自动重写残留）；过期注释修正 | 8.70s / 11.46s | 命中全为同构类（gson/JDK/引擎） |

**批1 失败修复记录**：首次提交漏查 `internal/relay` 包的顶层函数引用
（`PlayerHess:234`、`inject/GameVersionServer:332/346` 经包 import 使用
`fromRelayJumpsToAnotherServerInternalPacket`/`relayServerTypeInternal` 等），
干净克隆构建失败。修复：`playerJumpsToAnotherServer` 零调用→删方法；
`relayServerType*` 被 `HeadlessTypeConnect.processConnect`（RELAY_118_117_RETURN
活分发）触达→按纪律保守保留 `RelayServerType.kt`。amend 后全绿。

**保守保留记录**（本轮触达但按纪律不删）：
- `internal/relay/RelayServerType.kt`：inject/GameVersionServer 的
  `sendRelayServerType*` 覆写依赖其包构建器，`sendRelayServerTypeReply` 被
  HeadlessTypeConnect 活分发触达（类载 0 加载，但静态可达）；
- `BeanRelayConfig`：`HelperMain:165` 启动初始化 + `Administration:70` 保存，活路径；
- `Data.RELAY_COMMAND`、`Data.configRelay` 字段：`Data.kt` god object 边界；
- `BeanCoreConfig.mixProtocolEnable`：json 反序列化兼容，保留配置字段；
- `ServiceLoader.ServiceType.IRwHps` 枚举项：活类内部死项，无害；
- `struct/{IntMap,ObjectIntMap}`：relay 删除后新暴露的零引用零加载类，
  按 struct 容器保守区纪律保留（同 `IntSeq`/`SeqSave`/`LongIntMap`）。

**本轮数字**：删 17 文件；JVM 行数 35,628 → **32,972**（−2,656）；文件数
396 → 382（−14）；`git archive` 25,180,160 → **25,067,520**（−112,640 B）；
Helper.jar 25,338,175 → **25,263,097**（−75,078 B）。

**阶段二进度**：§C.2 ①②已完成（含删除落地）；剩余 ③`Initialization` 精简、
⑤kotlinx-coroutines/okhttp3 处置、④`Data` 裁剪（建议最后或不做）。

## 第六轮（2026-11-05）：阶段二收官——Initialization 精简 + 依赖处置 + 机械项

### 批1 `8f0e16ef`：③ Initialization 精简 + JNA 依赖移除（−575 行，jar −1.95M）

逐 init 步骤核对，只删类载/静态证明不执行的：

| 步骤 | 处置 | 证据 |
| --- | --- | --- |
| `checkEnvironment`（JNA 架构警告） | 删 | 全树唯一 JNA 触达点；删后类载日志 `com.sun.jna` 0 加载（原 44 条） |
| `initRsa`（调用点上游即注释） | 删，级联删 `util/algorithms/Rsa.kt`（135 行） | 零调用 |
| `startInit`/`initServerLanguage`/`eula` | 删，级联删 `PrintEx.kt`（52 行）与 `/eula/Main-*.md`×2 | helper 由 `HelperMain:167-173` 直接落位 settings（serverCountry=EN/eulaVersion/initStart=false），三方法全树零调用 |
| 9 个失效 import | 清理 | CallTimeTask/Threads/HeadlessModuleManage/NetService 等 |
| `loadLang`/`loadService`/shutdown hook | **保留** | 启动顺序依赖（i18n → 协议注册）与退出保存 |

构建：移除 `net.java.dev.jna:jna` 与 `org.jline:jline-terminal-jna` 依赖
（jar 内 jna 181 条目 / 压缩 1.84M 归零）。`jline-reader`/`jline-terminal`
保留：`Data.kt:164 privateReader: LineReader` 编译期钉住（god object 边界；
运行期 org.jline 0 加载）。**jar 25,263,098 → 23,309,069（−1,954,028 B）**。
验证：49 包 21.0s 无 FAIL；V2Ext 8.60s；E2E 11.76s 结算；类载交叉
com.sun.jna/Rsa/PrintEx 全 0，Initialization 正常加载。

### ⑤ kotlinx-coroutines / okhttp3 处置：**两库均活，保留**

`-Xlog:class+load` 实测（E2E 全程）：okhttp3 **69** 次加载、
kotlinx.coroutines **104** 次加载。具体路径：

| 库 | 加载源 | 结论 |
| --- | --- | --- |
| okhttp3（0.140s 起） | `Application.http = HttpRequestManage()` 与 `rwHttp`（`Data.core` 字段，启动即构造）；`DownloadManage`（`GameStartInit:60` 引擎资源下载）；`MyOkHttpRetryInterceptor` | 活路径，保留（`Server-Core/build.gradle.kts:43`） |
| kotlinx-coroutines（0.412s 起） | `AbstractEventManage.eventData = Events()`——事件核心，`Events.eventMonitorScope = CoroutineScope(SupervisorJob()+Dispatchers.Default)`；`AbstractFuture`/`EventFuture` 的 `.await()` 链（`runGlobalEventManage(...).await()`，连接关闭事件等） | 活路径，保留（`Server-Core/build.gradle.kts:15` api） |

`implementation.txt` 已于第五轮删除（无读取方），无需同步。

### ④ Data 裁剪：**已评估、本阶段不做**

试验三判定：`Data.core` 惰性化技术可行但收益≈0（20 处活读取面，首次访问
在启动期）。删除 `Application`/`NetServer` 需重写 20 处读取点并替换
`Administration`/`PluginData`/`HttpRequestManage` 三套活组件——属大规模重写
而非手术，若将来要做以前述三项替代为先决条件。

### 批2 `9c577701`：阶段三-2 配置面复盘（−10 死字段）

逐字段双侧审计（**Kotlin 0 解引用 且 Go-Server 0 使用才删**——Java `save()`
会重写 Config.json/ConfigServer.json，Go 仍读的键不能丢）：

- `BeanCoreConfig` 删 8：`noteEnglish`/`noteChina`/`cmdTitle`/
  `followBetaVersion`/`singleUserRelay`/`singleUserRelayMod`/`rconPort`/
  `runPid`（只写无读，连带删 `save()` 覆写）；
- `BeanServerConfig` 删 2：`maxUnit`/`defIncome`（唯一读取方 `Event.kt`
  已随 relay 前批删除，Go 侧 0 使用）；
- 保留：Go 侧仍读的键（`serverName`/`subtitle`/`defStartCommand`/
  `autoUpList`/`mixProtocolEnable`/`webToken` 等，虽 Kotlin 不读）；
- `BeanRelayConfig`：5 字段全死，但 Bean 本体被 `Data.configRelay`
  （god object 边界）+ `HelperMain:165` 初始化 + `Administration:70` 保存
  钉住，整体保留。

验证：49 包 17.1s 无 FAIL；V2Ext 8.80s；E2E 12.01s 结算。

### 批3 `1f61920a`：阶段三-3 异常路径确认 + 补测试

新增 `TestRealHelperRobustness`（直接拉起真实 Helper.jar 注入原始行）：
畸形帧（非 JSON 垃圾/截断 JSON/空行）全吞不崩；未知 op 返回
`ok:false unsupported`；缺字段帧缺省处理；**异常注入后 hello/room.status
仍正常**（JVM 未崩、主循环未死）；突然关闭 stdin（模拟 Go 断连）helper
感知 EOF 干净退出（`exitProcess(0)`），30s 时限内无挂死。PASS 3.22s。
防护机制：`HelperMain` dispatch try/catch（`HelperMain.kt:240-245`）+
`readLine() ?: break` EOF 退出。Go 侧异常面已有测试覆盖
（`backend_test.go`: HandshakeFailure/UnmatchedResponseIDIsIgnored/
UnknownEventIsIgnored 等）。验证：49 包 17.9s 无 FAIL；V2Ext 9.69s；
E2E 11.54s 结算。

### 阶段三-1：启动时间/内存/类数基线对照（各 3 次均值）

测量法：独立进程拉起真实 jar（`-Xmx512m` + `-Xlog:class+load`），
`boot_done` = 进程启动 → `[Helper] boot done`（协议循环就绪）；
`engine_socket` = → `CustomServerSocket` 类载（引擎网络就绪，约等于
可接收 `room.addai` 的时刻）；RSS 为就绪后 3s 常驻。

| 指标 | 优化前（`baabca44`, jar 25.3M） | 优化后（`8f0e16ef`, jar 23.3M） | 变化 |
| --- | --- | --- | --- |
| boot_done | 862 ms（828/827/932） | 1,165 ms（1627/827/1041，首跑离群） | ≈持平（噪声内） |
| engine_socket | 3,810 ms | 3,926 ms | ≈持平 |
| 常驻 RSS | ~475 MB | ~455 MB | **−20 MB（−4%）** |
| 加载类总数 | 5,423 | 5,344 | **−79**（jna 44 + Rsa/PrintEx/eula 链） |
| rwhps 类 | 479 | 479 | 持平 |

**结论**：启动时间由闭源引擎装载主导（引擎载入 ~3-4s），jar 瘦身不改变
启动速度；收益在分发体积（−1.95M）、常驻内存（−20MB）与加载类数。
无进一步启动优化的必要证据。

### 本轮数字

删 2 文件 + 10 死字段 + 2 依赖；JVM 行数 32,972 → **32,666**（−306）；
文件 382 → 380；`git archive` 25,077,760 → **25,036,800**（−40,960 B）；
Helper.jar 25,263,098 → **23,307,355**（−1,955,743 B，JNA 移除为主）。

**阶段二至此全部收官**（①②③⑤ 完成，④ 评估后不做）。剩余体积为
gameData 9.7M + 闭源引擎 4.2M + libs.zip 6.4M + 必要依赖，均在保留红线内。

## 当前不可再删项与理由（护栏，2026-11-05 稳定壳收尾）

本节是**结论性护栏**：以下各项都已被本轮"稳定壳收尾"的真实运行链路
（干净目录分发实跑、E2E 矩阵、真实 helper 集成测试）钉死。后人若想再删，
先回答"删后这条链路由谁承载"，否则不要动手——前六轮已反复验证过这些边界。

### 运行时实体（删了直接跑不起来）

| 项 | 钉死它的运行事实 |
| --- | --- |
| `data/gameData/`（≈12M，含 7z 资源） | 引擎开局必需；分发自足性测试（`TestDistributionSelfContained`）与干净目录实跑都以它为数据根，缺失则引擎无法装载地图/资源 |
| `Server-Core/libs/*.jar`（4.2M，含闭源 game-lib.jar） | 闭源引擎本体，无源码、不可重建，helper 的唯一"货物" |
| `Server-Core/src/main/resources`（libs.zip 6.4M + HZK16/ASC16 字体 + bundles） | libs.zip 是引擎装载源（`GameModularReusableLoadClass` 按包前缀复制装载）；字体被保留类 `Font16` 静态引用 |
| `dependent/redirections/**` + `plugin/internal/headless/**` | ASM 注入/重定向链。实例：`GameMainRedirections` 的 "Recording replay to:" 钩子是 `room.replayFileName` 的**唯一**写入点（本轮 E2E 竞态定位实证：结算的 `replayName` 就靠它异步落定） |
| `com/corrodinggames/**` 引擎桥接补丁 | 被 `GoBridge`/`LinkGameNet` 等直接引用 |
| `net/` 残余（NetService / NetStaticData / netconnectprotocol.realize 等） | `CustomServerSocket`（引擎桥接）运行时直接构造 `RwHps` 并直赋 `NetStaticData.RwHps` |
| `core/Application.kt` + `core/NetServer.kt` | `Data.kt:123` god object 字段 `val core = Application()`，20 处活读取面；删除 = 大规模重写（枢纽试验三结论） |
| kotlinx-coroutines / okhttp3 | 第六轮 ⑤ 逐一查证：两库均活，保留 |

### 闭包外保守区（8 文件，别再零敲碎打）

`FastIntValueClass`（ASM 重定向目标）、`SyncByteArrayChannel` /
`DynamicPrintStream`（io）、`IntSeq` / `SeqSave` / `LongIntMap` / `IntMap` /
`ObjectIntMap`（struct 容器）。它们位于保留类的静态引用面或同包引用面内，
静态分析无法穷尽；再删**必须逐类做运行时类载验证**，单靠闭包扫描不够
（第五轮复扫已确认闭包外只剩这 8 个，全部按区纪律保留）。

### 本轮新增的钉死项（稳定壳收尾）

| 项 | 钉死它的运行事实 |
| --- | --- |
| `Go-Server/internal/resource/assets/bundles/HPS_*.properties`（4 语言包） | 控制台命令注册逐键依赖；`TestI18NBundleLoad` 钉死键数基线（当前 **205**，含本轮新增 `serverCommands.addai`） |
| 控制台 `addai` 命令（Go 侧 `servercommands.go`） | 无头分发形态的唯一 AI 驱动入口；分发自足性测试与干净目录实跑都靠它开局 |
| `bootstrap.go` 的 CloseServerHook 配置回写 | helper 退出时 Java 侧按 Bean 字段集整文件重写配置，Go 不最后写回就丢 `gameHelperPath`（本轮修复并经 dist 测试断言） |
| `dist/`（build-dist.sh / start.sh / 默认 Config*.json） | 分发形态入口；`docs/DISTRIBUTION.md` 是配套合同 |
| `docs/DISTRIBUTION.md` / `docs/BASELINE.md` | 分发合同与回归基线本身 |

### 方法论护栏

任何后续删除提案，**先过四项硬验证再谈合并**：

1. 干净克隆（`git clone` 本仓库）构建：`JAVA_HOME=<jdk17> ./gradlew :Helper:jar`；
2. `go build ./... && go vet ./...`；
3. `go test ./... -count=1` 全绿（含真实 helper 集成与分发自足性测试）；
4. 分发包干净目录实跑：启动 → 开局 → 结算 → 优雅退出。

"整体重跑一次就绿了"不算通过——失败必须定位到具体环节（本轮
`ReplayName` 竞态即按此纪律定位到引擎回放保存的异步钩子并修复测试）。
