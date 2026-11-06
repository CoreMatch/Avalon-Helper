/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.game.GameMaps.MapFileType.*
import io.github.corematch.avalonhelper.util.compression.CompressionDecoderUtils
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.file.FileUtils.Companion.getFolder
import io.github.corematch.avalonhelper.util.log.Log.error

/**
 * 游戏的地图数据
 * @author Dr (dr@der.kim)
 */
class GameMaps {
    /** 地图类型  */
    var mapType = MapType.DefaultMap

    /** 地图数据  */
    var mapData: MapData? = null

    /** 地图名  */
    var mapName = "Crossing Large (10p)"

    /** 地图人数  */
    var mapPlayer = "[z;p10]"

    /**
     * 地图的类型
     * @property fileType 地图文件后缀
     * @constructor
     */
    enum class MapType(val fileType: String) {
        /**
         * DefaultMap : 官方地图
         * customMap  : 自定义的地图
         * SavedGames : 游戏里保存的进度
         */
        DefaultMap(".tmx"),
        CustomMap(".tmx"),
        SavedGames(".rwsave");
    }

    enum class MapFileType {
        /**
         * File         : 以文件的形式提供
         * Zip          : 以ZIP提供(文件在ZIP内)
         * WebDownLoad  : 以WebFile的形式提供(需要下载)
         */
        File,
        Zip,
        WebDownLoad;
    }

    class MapData {
        val mapType: MapType
        val mapFileName: String

        private val mapFileType: MapFileType
        private val zipFileName: String?
        private var mapClean = false
        private var mapFile: FileUtils? = null


        constructor(mapType: MapType, mapFileType: MapFileType, mapFileName: String) {
            this.mapType = mapType
            this.mapFileType = mapFileType
            this.mapFileName = mapFileName
            zipFileName = null
        }

        constructor(mapType: MapType, mapFileType: MapFileType, mapFileName: String, zipFileName: String) {
            this.mapType = mapType
            this.mapFileType = mapFileType
            this.mapFileName = mapFileName
            this.zipFileName = zipFileName
        }

        /**
         * 懒加载
         * 读取地图到内部bytes
         */
        fun readMap() {
            val fileUtil = if (mapType == MapType.SavedGames) {
                getFolder(Data.ServerSavePath, true)
            } else {
                getFolder(Data.ServerMapsPath, true)
            }
            if (fileUtil.toFile(mapFileName+mapType.fileType).exists()) {
                return
            }
            var bytesMap: ByteArray? = null
            when (mapFileType) {
                File -> {
                    // 忽略, 因为File不需要再次加载
                }
                Zip -> try {
                    bytesMap = CompressionDecoderUtils.zip(fileUtil.toFile(zipFileName!!).file)
                        .use { it.getTheFileBytesOfTheSpecifiedSuffixInTheZip(this) }
                } catch (e: Exception) {
                    error("Read Map Bytes Error", e)
                }
                WebDownLoad -> {
                }
            }

            mapClean = true

            mapFile = fileUtil.toFile(mapFileName+mapType.fileType)
            mapFile!!.writeFileByte(bytesMap!!)
        }

        /**
         * 清理服务器使用的地图数据
         */
        fun clean() {
            if (mapClean) {
                mapFile!!.delete()
                mapFile = null
            }
        }
    }
}