/*
 * Helper: Go 服务端的外部游戏内核进程 (protocol v2 / v2-ext 的 helper 侧实现).
 *
 * 有意修正（作者已批准，2026-11-04）: 上游内核留在 JVM 侧, Go 经
 * Avalon 仓库 (CoreMatch/Avalon) docs/HELPER-PROTOCOL.md 定义的线协议驱动.
 * 本模块是薄封装: 复用 Server-Core 的 headless 内核装载链与引擎桥接类,
 * 只把"客户端网络层"换成 stdin/stdout 协议适配器.
 *
 * 构建: ./gradlew :Helper:jar   (需 JDK 17, 见 Avalon 仓库 README.md)
 * 产物: Helper/build/libs/Helper.jar (fat jar, 不入库)
 */

import java.text.SimpleDateFormat
import java.util.Date

dependencies {
    implementation(project(":Server-Core"))
    // 上游以 compileOnly 声明、运行时经 LibraryManager 联网拉取的依赖,
    // 在 helper 里直接打进 fat jar (构建可复现, 不依赖运行时下载).
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-tree:9.7")
    // 引擎混淆类仅供编译期引用; 运行时在游戏 classloader 内 (同 Server-Core 的做法)
    compileOnly(fileTree(mapOf("dir" to "../Server-Core/libs", "include" to "game-lib.jar")))
    compileOnly(fileTree(mapOf("dir" to "../Server-Core/libs", "include" to "slick.jar")))
}

// Server-Core 的大量桥接成员是 Kotlin internal (GameEngine/PluginManage 等).
// 以 friend 模块身份编译 (等价于同模块可见), 避免反射碎片化; 不改动上游源码.
// friend 路径必须与编译类路径上的实际条目一致 (这里是 Server-Core.jar).
tasks.compileKotlin {
    val serverCoreJar = project(":Server-Core").layout.buildDirectory
        .file("libs/Server-Core.jar").get().asFile.absolutePath
    val serverCoreClasses = project(":Server-Core").layout.buildDirectory
        .dir("classes/kotlin/main").get().asFile.absolutePath
    compilerOptions {
        freeCompilerArgs.add("-Xfriend-paths=$serverCoreJar,$serverCoreClasses")
    }
}

tasks.jar {
    archiveBaseName.set("Helper")

    // 与 Server-All 相同的清单裁剪
    exclude("META-INF/version/**")
    exclude("**/module-info.class")
    exclude("META-INF/maven/**")
    exclude("META-INF/AL2.0")
    exclude("META-INF/LGPL2.1")
    exclude("META-INF/native-image/**")
    exclude("META-INF/proguard/**")
    exclude("META-INF/**.kotlin_module")
    exclude("META-INF/**/LICENSE.txt")
    exclude("META-INF/LICENSE")
    exclude("META-INF/NOTICE.txt")
    exclude("META-INF/NOTICE")
    exclude("META-INF/INDEX.LIST")
    exclude("META-INF/**.properties")
    exclude("META-INF/**.xml")
    exclude("META-INF/DEPENDENCIES")
    exclude("META-INF/sisu/javax.inject.Named")
    exclude("about.html")
    exclude("META-INF/LWJGL.*")
    exclude("META-INF/BC2048KE.SF")
    exclude("META-INF/BC2048KE.DSA")
    exclude("META-INF/BC1024KE.SF")
    exclude("META-INF/BC1024KE.DSA")

    manifest {
        // helper 自己的入口
        attributes(mapOf("Main-Class" to "net.rwhps.server.helper.HelperMainKt"))
        // ASM agent 自附着 (与 Server-All 相同机制): java -jar 时先于 main 拿到 Instrumentation
        attributes(mapOf("Launcher-Agent-Class" to "net.rwhps.server.dependent.AgentAttachData"))
        attributes(mapOf("Can-Redefine-Classes" to "true"))
        attributes(mapOf("Implementation-Title" to "RW-HPS-Helper"))
        attributes(mapOf("Build-Jar-Time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date().time)))
    }

    // 运行时依赖全部打进 fat jar; 上游 ConnectionAgreement 被本模块的同名补丁类
    // 替换 (构建期注入, 不改上游源文件): 虚拟连接构造器用于截流引擎出包.
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    }) {
        exclude("net/rwhps/server/net/core/ConnectionAgreement.class")
        exclude("net/rwhps/server/net/core/ConnectionAgreement\$*.class")
        // Go+helper 形态不承载任何 netty 网络栈 (运行时类载日志 0 命中):
        // 上游的 netty 依赖随 Server-Core api 传递进来, 打包期整体排除.
        exclude("io/netty/**")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
