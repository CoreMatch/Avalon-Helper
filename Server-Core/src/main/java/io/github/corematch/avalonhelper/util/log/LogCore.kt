/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.log

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.game.event.global.ServerConsolePrintEvent
import io.github.corematch.avalonhelper.util.Time.getMilliFormat
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.file.plugin.PluginManage
import io.github.corematch.avalonhelper.util.inline.ifNullResult
import io.github.corematch.avalonhelper.util.log.ColorCodes.formatColors
import io.github.corematch.avalonhelper.util.log.Log.skipping
import io.github.corematch.avalonhelper.util.log.exp.ExceptionX
import io.github.corematch.avalonhelper.util.str.Parser
import java.io.FileOutputStream

/**
 * Log 的核心实现
 *
 * @date 2024/6/23 上午1:19
 * @author Dr (dr@der.kim)
 */
abstract class LogCore {
    /* Log level defaults to WARN */
    private var logGrade = Logg.WARN
    private val outLog: FileOutputStream

    init {
        val logLogFile = FileUtils.getFolder(Data.ServerLogPath).toFile("Log.txt")
        outLog = logLogFile.writeByteOutputStream(logLogFile.file.length() <= 512 * 1024)
    }

    fun set(log: String) {
        logGrade = Logg.from(log)
    }

    fun saveLog() {
        outLog.flush()
    }

    /**
     * WLog：
     * @param i Warning level -INT
     * @param tag Title / Default empty
     * @param eIn Info
     * i>=Set the level to write to the file
     */
    protected fun logs(i: Logg, tag: Any, eIn: Any? = null) {
        val errorFlag = (tag is Throwable || eIn is Throwable)
        if (logGrade.level > i.level && !errorFlag) {
            return
        }

        val lines = (if (tag is Throwable) {
            ExceptionX.resolveTrace(tag)
        } else if (eIn is Throwable) {
            ExceptionX.resolveTrace(eIn)
        } else {
            eIn.ifNullResult("") { it.toString() }
        }).split(Data.LINE_SEPARATOR).toTypedArray()

        val printData = StringBuilder()

        // [Time][Log-Leve]
        printData.append("[").append(getMilliFormat(1)).append("]").append(i.tag).append("&b[\$ThreadName]&fr")
        // [Time][Log-Leve][Tag]
        if (tag !is Throwable && tag != "") {
            printData.append(" ").append(tag)
        }
        // [Time] [Log-Leve] [Tag] :
        if (lines[0] != "") {
            printData.append(": ")
        }

        var flag = false
        // 避免换行
        if (lines.size > 1) {
            printData.append(Data.LINE_SEPARATOR)
            for (line in lines) {
                if (line.trim().replace("(?m)^\\s*$(\\n|\\r\\n)".toRegex(), "").isNotEmpty()) {
                    if (flag) {
                        printData.append(Data.LINE_SEPARATOR)
                    }
                    printData.append(line)
                    flag = true
                }
            }
        } else {
            printData.append(lines[0])
        }

        printData.append("&fr")

        print(errorFlag, formatColors(Parser.parseStr(printData.toString())))
    }

    private fun print(error: Boolean, text: String) {
        println(text)

        PluginManage.runGlobalEventManage(ServerConsolePrintEvent(text))

        if (error) {
            // Remove Color
            outLog.write("${text.replace("\\e\\[[\\d;]*[^\\d;]".toRegex(), "")}${Data.LINE_SEPARATOR}".toByteArray())
            //outLog.write("$textCache${Data.LINE_SEPARATOR}".toByteArray())
        }
    }

    protected enum class Logg(
        val level: Int,
        val tag: String,
    ) {
        /* ALL during development */
        OFF(99 ,""),
        CONSOLE(10 ,""),
        FATAL(7,"${ColorStructure(backGroundRgb = intArrayOf(255, 0, 0))}[FATAL]&fr"),
        ERROR(6,"&r[ERROR]&fr"),
        WARN(5,"&y[WARN]&fr"),
        INFO(4,"&g[INFO]&fr"),
        DEBUG(3,"${ColorStructure(foreGroundRgb = intArrayOf(128, 128, 128))}[DEBUG]&fr"),
        TRACK(2,"${ColorStructure(foreGroundRgb = intArrayOf(128, 128, 128))}[TRACK]&fr"),
        ALL(1,"[All]");

        companion object {
            fun from(type: String): Logg = entries.find { it.name == type || it.name.lowercase() == type.lowercase() }.ifNullResult({
                skipping("Log Level Set Error , In: $type")
                ALL
            }) { it }
        }
    }
}