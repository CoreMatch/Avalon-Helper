/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.manage

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.game.GameMaps
import io.github.corematch.avalonhelper.struct.map.ObjectMap
import io.github.corematch.avalonhelper.struct.map.OrderedMap
import io.github.corematch.avalonhelper.util.algorithms.Base64
import io.github.corematch.avalonhelper.util.compression.CompressionDecoderUtils
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.log.Log
import java.io.File

object MapManage {

    /** 地图数据  */
    val maps = GameMaps()
    val mapsData = OrderedMap<String, GameMaps.MapData>(8)
    val gameMaps = ObjectMap<String, String>()

    init {
        with(gameMaps) {
            put("Beachlanding(2p)[byhxyy]", "Beach landing (2p) [by hxyy]@[p2]")
            put("BigIsland(2p)", "Big Island (2p)@[p2]")
            put("DireStraight(2p)[byuber]", "Dire_Straight (2p) [by uber]@[p2]")
            put("FireBridge(2p)[byuber]", "Fire Bridge (2p) [by uber]@[p2]")
            put("Hills(2p)[ByTstis&KPSS]", "Hills_(2p)_[By Tstis & KPSS]@[p2]")
            put("IceIsland(2p)", "Ice Island (2p)@[p2]")
            put("Lake(2p)", "Lake (2p)@[p2]")
            put("SmallIsland(2p)", "Small_Island (2p)@[p2]")
            put("Twocoldsides(2p)", "Two_cold_sides (2p)@[p2]")
            put("Hercules(2vs1p)[byuber]", "Hercules_(2vs1p) [by_uber]@[p3]")
            put("KingoftheMiddle(3p)", "King of the Middle (3p)@[p3]")
            put("Depthcharges(4p)[byhxyy]", "Depth charges (4p) [by hxyy]@[p4]")
            put("Desert(4p)", "Desert (4p)@[p4]")
            put("IceLake(4p)[byhxyy]", "Ice Lake (4p) [by hxyy]@[p4]")
            put("Islandfreeze(4p)[byhxyy]", "Island freeze (4p) [by hxyy]@[p4]")
            put("Islands(4p)", "Islands (4p)@[p4]")
            put("LavaMaze(4p)", "Lava Maze (4p)@[p4]")
            put("LavaVortex(4p)", "Lava Vortex (4p)@[p4]")
            put("MagmaIsland(4p)", "Magma Island (4p)@[p4]")
            put("Manipulation(4p)[ByTstis]", "Manipulation_(4p)_[By Tstis]@[p4]")
            put("Nuclearwar(4p)[byhxyy]", "Nuclear war (4p) [by hxyy]@[p4]")
            put("Crossing(6p)", "Crossing (6p)@[p6]")
            put("ShoretoShore(6p)", "Shore to Shore (6p)@[p6]")
            put("ValleyPass(6p)", "Valley Pass (6p)@[p6]")
            put("BridgesOverLava(8p)", "Bridges Over Lava (8p)@[p8]")
            put("Coastline(8p)[byhxyy]", "Coastline (8p) [by hxyy]@[p8]")
            put("HugeSubdivide(8p)", "Huge Subdivide (8p)@[p8]")
            put("Interlocked(8p)", "Interlocked (8p)@[p8]")
            put("InterlockedLarge(8p)", "Interlocked Large (8p)@[p8]")
            put("IsleRing(8p)", "Isle Ring (8p)@[p8]")
            put("LargeIceOutcrop(8p)", "Large Ice Outcrop (8p)@[p8]")
            put("LavaBiogrid(8p)", "Lava Bio-grid(8p)@[p8]")
            put("LavaDivide(8p)", "Lava Divide(8p)@[p8]")
            put("ManyIslands(8p)", "Many Islands (8p)@[p8]")
            put("RandomIslands(8p)", "Random Islands (8p)@[p8]")
            put("Tornadoeye(8p)[byhxyy]", "Tornado eye (8p) [by hxyy]@[p8]")
            put("TwoSides(8p)", "Two Sides (8p)@[p8]")
            put("Volcano(8p)", "Volcano (8p)@[p8]")
            put("VolcanoCrater(8p)", "Volcano Crater(8p)@[p8]")
            put("TwoSidesRemake(10p)", "Two Sides Remake (10p)@[z;p10]")
            put("ValleyArena(10p)[byuber]", "Valley Arena (10p) [by_uber]@[z;p10]")
            put("ManyIslandsLarge(10p)", "Many Islands Large (10p)@[z;p10]")
            put("CrossingLarge(10p)", "Crossing Large (10p)@[z;p10]")
            put("Kingdoms(10p)[byVulkan]", "Kingdoms (10p) [by Vulkan]@[z;p10]")
            put("LargeLavaDivide(10p)", "Large Lava Divide (10p)@[z;p10]")
            put("EnclosedIsland(10p)", "Enclosed Island (10p)@[z;p10]")
            put("TwoLargeIslands(10p)", "Two_Large_Islands_(10p)@[z;p10]")
            put("Wetlands(10p)", "Wetlands (10p)@[z;p10]")
        }
    }


    fun readMapAndSave() {
        readList(FileUtils.getFolder(Data.ServerMapsPath)) { file, postpone, name ->
            when (postpone) {
                ".tmx" -> try {
                    mapsData[name] = GameMaps.MapData(GameMaps.MapType.CustomMap, GameMaps.MapFileType.File, name)
                } catch (exception: Exception) {
                    Log.error("read tmx Maps", exception)
                }
                ".zip" -> try {
                    CompressionDecoderUtils.zip(file).use {
                        val zipTmx = it.getTheFileNameOfTheSpecifiedSuffixInTheZip("tmx")
                        zipTmx.eachAll { zipMapName: String ->
                            mapsData[zipMapName] = GameMaps.MapData(
                                    GameMaps.MapType.CustomMap, GameMaps.MapFileType.Zip, zipMapName, name
                            )
                        }
                    }
                } catch (exception: Exception) {
                    Log.error("ZIP READ", exception)
                }
            }
        }
        readList(FileUtils.getFolder(Data.ServerSavePath)) { file, postpone, name ->
            when (postpone) {
                ".rwsave" -> try {
                    mapsData[name] = GameMaps.MapData(GameMaps.MapType.SavedGames, GameMaps.MapFileType.File, name)
                } catch (exception: Exception) {
                    Log.error("read save Maps", exception)
                }
                ".zip" -> try {
                    CompressionDecoderUtils.zip(file).use {
                        val zipSave = it.getTheFileNameOfTheSpecifiedSuffixInTheZip("save")
                        zipSave.eachAll { zipSaveName: String ->
                            mapsData[zipSaveName] = GameMaps.MapData(
                                    GameMaps.MapType.SavedGames, GameMaps.MapFileType.Zip, zipSaveName, name
                            )
                        }
                    }
                } catch (exception: Exception) {
                    Log.error("ZIP READ", exception)
                }
            }
        }
    }

    private fun readList(fileUtils: FileUtils, process: (File, String, String)->Unit) {
        val list = fileUtils.fileListNotNullSizeSort
        list.eachAll { e: File ->
            val original = if (Base64.isBase64(e.name)) Base64.decodeString(e.name) else e.name
            val postpone = original.substring(original.lastIndexOf("."))
            val name = original.substring(0, original.length - postpone.length)
            process(e, postpone, name)
        }
    }
}