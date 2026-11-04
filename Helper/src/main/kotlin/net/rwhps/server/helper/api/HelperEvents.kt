/*
 * Helper: bridge -> 协议层 的回调接口.
 *
 * 有意修正（作者已批准，2026-11-04）: 本接口由 app classloader 加载,
 * 游戏 classloader 内的桥接类经父委托可见, 是两条类加载器之间唯一的
 * 直接类型通道 (其余跨层调用走反射, 与上游 GameMainRedirections 同款手法).
 */

package net.rwhps.server.helper.api

interface HelperEvents {
    /** 引擎要发给某客户端的游戏包 (对应协议 type:"packet") */
    fun onPacket(conn: String, ptype: Int, data: ByteArray)

    /** 房间/玩家事件 (对应协议 type:"event"); dataJson 为负载 JSON 或空 */
    fun onEvent(name: String, conn: String, dataJson: String)

    /** 致命错误 (对应协议 type:"fatal") */
    fun onFatal(error: String)
}
