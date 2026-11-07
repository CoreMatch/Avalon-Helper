# 谱系比对：本仓对上游 RW-HPS 的 copycat 审计

审计日期：2026-11-07 · 只读分析（不改代码、不动许可证）· 可复现（见 §6 与 `scripts/lineage-report.sh`）

本文回答一个问题：**本仓（Avalon-Helper，原 RW-HPS fork）现在还有多少代码在谱系上来自上游 RW-HPS（原作者 deng-rui，个人仓库已删除）。** 判定落到文件级与行级，结论先行：**本仓是上游的衍生作品，且以"原样上游代码 + 局部修改"为主**；自有部分集中在 `Helper/` 模块与少量重写文件。

---

## 1. 比对基准

| 项 | 值 |
|---|---|
| 上游基准 | `upstream/master` = RW-HPS 组织仓（`https://github.com/RW-HPS/RW-HPS.git`）master @ **`239a7773`**（"Chinese"） |
| 拓扑事实 | `git merge-base master upstream/master` = `239a7773` = 上游 HEAD——**fork 点即上游 HEAD**，fork 时 Kotlin 树与上游逐字节一致 |
| deng-rui 个人仓 | 已由作者删除；组织仓基准即权威基准（fork 的 Kotlin 部分与组织仓 master 完全一致由上述拓扑事实保证） |
| 本仓 HEAD（审计时） | `107778fe`（改名手术后；包名 `net.rwhps.server.*` → `io.github.corematch.avalonhelper.*` 的机械改名不影响谱系判定，blame 按内容追溯） |

## 2. 判定方法

1. **文件级**：当前树每个 `.kt/.java` 文件按包名逆映射回上游路径（`io/github/corematch/avalonhelper` → `net/rwhps/server`、`.../asm` → `net/rwhps/asm`），比对 blob 哈希：相同 / 有对应但被修改 / 无上游对应。
2. **行级**：`git blame --line-porcelain` 按 `author-mail` 归属。上游身份集：`dr@der.kim`、`deng-rui@*`、`493505110@qq.com`、`zerodegress@outlook.com`、`*@users.noreply.github.com`（上游贡献者）；本人身份：`me@samuelcheston.com`。
3. **结构性证据**：文件级哈希与行级归属双重覆盖，不依赖主观判断。

## 3. 总体数字（2026-11-07 实跑）

- 追踪的 `.kt/.java` 文件：383 个（上游树 517 个；仓库瘦身删除约 134 个上游文件，新增 8 个自有文件）
- 总行数：**33,122**；上游作者行 **30,192（91.2%）**；SamuelCheston 行 **2,926（8.8%）**
- 按文件上游占比分档：**≥90%：297 个**；50–90%：79 个；1–50%：0 个；**0%：7 个**（分布双峰：要么基本原样，要么纯自有）
- 与上游 blob 逐字节相同：**3 个**（`buildSrc/src/main/kotlin/{Data,Utils,Versions}.kt`）

## 4. 逐目录分级

| 目录 | 分级 | 支撑证据 |
|---|---|---|
| `Server-Core/src`（335 文件） | **原样上游（经修改）** | 94.0% 行归属上游；其中 262 文件上游占比 ≥90%。修改以仓库瘦身删减、枢纽手术接线与注释为主 |
| `ASM-Framework/src`（37 文件） | **原样上游（经修改）** | 95.2% 上游行 |
| `buildSrc`（3 文件） | **原样上游（逐字节）** | 与上游 blob 完全相同 |
| `Helper/`（4 文件，1,025 行） | **重写/自有** | 上游行 0%：`HelperMain.kt`（411 行，协议主循环）、`GoBridge.kt`（368 行，引擎桥接）、`ConnectionAgreement.kt`（226 行，虚拟连接补丁类）、`HelperEvents.kt`（20 行） |
| `Server-Core` 个别文件 | **重写/自有** | `SimpleTimeTask.kt`（113 行，替代上游 TimeTaskQuartz 的重写）、`NetStaticData.kt`（44 行，枢纽手术重写）、`LwjglClassProperties.kt`（22 行） |
| 50–90% 档（79 文件） | **原样上游（局部改动）** | 如 `BeanCoreConfig.kt`（79%）、`Application.kt`（83%）、`Initialization.kt`（66%）、`CustomServerSocket.kt`（82%）——改动为删减/注释/少量新接线 |
| `data/gameData` | 上游资产 | 游戏数据文件（非代码），随上游提供 |
| `LICENSE` | 上游原文 | AGPL-3.0 原文保留 |

> 注：`IProtocolProvider.kt` / `ProtocolProvider.kt` / `FakeProtocolProvider.kt` 由上游 `IRwHps.kt` / `RwHps.kt` / `FakeRwHps.kt` 改名而来，文件级映射因文件名变化落入"无上游对应"，但行级 blame 仍追溯为上游行——以行级归属为准。

## 5. 自有部分清单（每一行可追到 SamuelCheston 提交）

| 文件 | 行数 | 性质 |
|---|---|---|
| `Helper/src/main/kotlin/.../helper/HelperMain.kt` | 411 | helper 协议主循环（protocol v2/v2-ext helper 侧），上游无对应物 |
| `Helper/src/main/kotlin/.../inject/go/GoBridge.kt` | 368 | 引擎桥接（反射句柄 + v2-ext 全部操作），上游无对应物 |
| `Helper/src/main/kotlin/.../net/core/ConnectionAgreement.kt` | 226 | 虚拟连接补丁类（构建期替换上游同名类），为 helper 形态新写 |
| `Helper/src/main/kotlin/.../helper/api/HelperEvents.kt` | 20 | helper 事件接口 |
| `Server-Core/.../core/thread/SimpleTimeTask.kt` | 113 | 上游 TimeTaskQuartz 的等价重写 |
| `Server-Core/.../data/global/NetStaticData.kt` | 44 | 枢纽手术重写（直赋协议实例） |
| `Server-Core/.../redirections/lwjgl/LwjglClassProperties.kt` | 22 | 属性键常量重写 |

## 6. 复现

```bash
# 本仓根目录 (需 upstream 远端: https://github.com/RW-HPS/RW-HPS.git)
bash scripts/lineage-report.sh
# 输出: 文件级三分类 + 行归属汇总 + 模块级占比 + 0% 上游文件清单
# 明细: /tmp/lineage-blame.csv (file,total,upstream,samuel,other)
```

单条命令等价物：

```bash
git merge-base master upstream/master          # = 239a7773 = upstream HEAD
git blame --line-porcelain master -- <file> | grep '^author-mail' | sort | uniq -c
```

## 7. 结论：构成衍生作品 ≠ 风险

- **判定**：本仓无疑问构成上游 RW-HPS 的衍生作品——91.2% 的行直接归属上游作者，297 个文件 ≥90% 原样。自有 8.8%（Helper 模块等）与上游代码结合为同一程序，不改变整体定性。
- **法律姿态**：AGPL-3.0 **允许**演绎（修改与再分发），义务是许可传染 + 保留版权与许可声明 + 提供源码。本仓现状：`LICENSE` 为 AGPL 原文、386 个源文件保留上游版权声明头、仓库公开——**恰好是 AGPL 对衍生作品要求的合规姿态**。
- **风险只在**：否认谱系、剥离许可或版权声明、闭源分发。当前姿态与之相反，故"构成衍生"本身不构成本仓的法律风险。
- 改名（包名/仓库名/产物名）是身份变更，不改变谱系；本文档即谱系的明示记录。
