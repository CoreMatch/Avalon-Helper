# REPO-SLIM: 以 Helper 编译/运行闭包为判据的仓库瘦身（最终版）

日期: 2026-11-04 ～ 2026-11-05 · 基线: `master` @ `b0fbccfa`（第一轮前）
四轮执行：第一轮 `89dc0d0a`（保守档），第二轮激进档 `0e82df79…a7cddd6c`，
第三轮 IP 定位整链 `2a096b08…007e1f10`，第四轮方案甲闭包续删 + netty 排除
`589ce2b6…350a00b2`（本次）。

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
| relay 类簇残余（`RelayRoom`/`GameVersionRelay`/`internal/relay` 等） | `NetStaticData` 是 Kotlin `object`，字段 `relayRoom = RelayRoom(...)` 在对象初始化时即构造；`ServerCommands`（inject 链，运行时加载）经 `NetStaticData.RwHps` 触达。类载日志显示 `GameVersionRelay` 等仍随对象初始化加载 | 删除需重写 `NetStaticData`（运行时对象手术），风险 ≫ 收益，保留。（第四轮已先删其外围零注册的 `Type*` 协议簇 5 文件，见下） |
| `net/` 其余（NetService/NetStaticData/netconnectprotocol.realize 等） | `CustomServerSocket`（引擎桥接）运行时经 `IRwHpsManage→RwHps→GameVersionPacket` 取用；`GameVersionServer` 用 `NetStaticData.RwHps.abstractNetPacket` | 真实运行时依赖，保留 |
| `core/Application.kt` + `core/NetServer.kt` | `Data.kt:123` god object 字段 `val core = Application()` 在对象初始化时构造；NetServer 被 Application 引用 | 被 `Data` 钉死，删除需重写 god object（枢纽手术），保留 |
| `game/`、`io/`、`util/`、`struct/`、`func/` 中未加载部分 | 被上述运行时链与 `Data.kt`（god object）静态引用；同包引用无法静态穷尽 | 保守保留；再删需逐类运行时验证。第四轮闭包后仅剩 6 文件（`FastIntValueClass`/`SyncByteArrayChannel`/`DynamicPrintStream`/`IntSeq`/`SeqSave`/`LongIntMap`），全部位于 ASM 重定向/io/struct 保守区 |
| `resources`（HZK16/ASC16 字体、libs.zip 等） | 被保留类 `Font16` 与引擎装载链静态引用 | 保留（ip2location.7z 7.5M 已于第三轮删除） |

## 最终体积对比

| 指标 | 原始 `b0fbccfa` | 第一轮后 `89dc0d0a` | 第三轮后 `ed92ff0f` | 最终（第四轮） | 变化（累计） |
| --- | --- | --- | --- | --- | --- |
| `git archive` 字节 | 33,546,240 | 33,495,040 | 25,344,000 | **25,169,920** | **−8,376,320 B（−25.0%）** |
| Kt/Java 行数 | 45,599 | 45,028 | 38,847 | **35,628** | **−9,971 行（−21.9%）** |
| Kt/Java 文件数 | 521 | 509 | 446 | **396** | **−125 个（−24.0%）** |
| Helper.jar 产物 | —（无此产物） | — | 28,665,640 | **25,337,804** | **−3,327,836 B（−11.6%）** |
| Gradle 模块 | 6 | 5 | 4 | **4**（Helper/Server-Core/ASM-Framework/buildSrc 工具） | −2（Server-All、Java-Doc、UPList、TimeTaskQuartz 除名） |

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
