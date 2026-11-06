rootProject.name = "Avalon-Helper"

include(":ASM-Framework")
include(":Server-Core")

// Go 服务端的外部游戏内核 helper (DECISIONS D-9/D-14 后续, 2026-11-04 作者批准).
// 构建配置条目; 不改动上游 Kotlin 源码.
include(":Helper")
