/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import dev.nytheria.compat.gradle.ClassTweakerUpdater
import dev.nytheria.compat.gradle.loom.NytheriaCompatLoomPlugin
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    kotlin("jvm")
    alias(libs.plugins.fabric.loom)
    id("com.gradleup.shadow") version "9.4.2"
    alias(libs.plugins.minivan)
    id("dev.nytheria.compat.gradle.nytheria-compat-plugin")
}

apply<NytheriaCompatLoomPlugin>()

version = "${createVersion()}${getVersionMetadata()}"
group = property("maven_group")!!

base {
    archivesName.set(property("archives_base_name")!! as String)
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId.set("nytheria_compat")
        eula = true
    }
}

sourceSets {
    getByName("main") {
        java.srcDir("src/main/java")
        java.srcDir("src/main/kotlin")
        java.srcDir("forge/src/main/java")
        java.srcDir("fml/loader/src/main/java")

        resources.srcDir("forge/src/generated/resources")
        resources.srcDir("forge/src/main/resources")
        resources.srcDir("fml/loader/src/main/resources")

        resources.exclude("META-INF/MANIFEST.MF") // god dammit neo
    }

    getByName("test") {
        java.srcDir("tools/trim")
    }

    getByName("gametest") {
        java.srcDirs("forge/src/test/java")
        resources.srcDir("forge/src/generated_test/resources")
        resources.srcDir("forge/src/test/resources")
    }
}

loom {
    accessWidenerPath.set(file("src/main/resources/nytheria_compat.classtweaker"))
    mixin {
//        useLegacyMixinAp = false
        showMessageTypes.set(true)

        messages.set(mutableMapOf(
            "ACCESSOR_TARGET_NOT_FOUND" to "disabled",

            // Make sure that we don't accidentally leave broken mixins. This happens a lot, I don't know why the hell these aren't error-level by default.
            "MIXIN_SOFT_TARGET_NOT_RESOLVED" to "error",
            "TARGET_ELEMENT_NOT_FOUND" to "error",
            //"NO_OBFDATA_FOR_METHOD" to "error",
            "MISSING_INJECTOR_DESC_SINGLETARGET" to "error"
        ))
    }
}

// Fabric-ASM with a neutral mod ID (see rebrandFabricAsm at the end of this file). Declared before allprojects, which uses it.
val nytheriaCompatFabricAsmJar: File by lazy { rebrandFabricAsm() }

allprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    val prodRuntimeDep by configurations.creating

    tasks {
        create("printConfigurations") {
            doLast {
                println("Project Name: ${project.name} configurations:")
                configurations.forEach { config ->
                    println("\t- ${config.name}")
                }
            }
        }
    }

    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://maven.fabricmc.net") {
            name = "FabricMC"
        }

        maven("https://mvn.devos.one/releases/") {
            name = "devOS Maven"
        }

        maven("https://maven.florianreuth.de/snapshots") {
            name = "AsmFabricLoader"
        }

        maven("https://mvn.devos.one/snapshots/") {
            name = "devOS Maven (Snapshots)"
        }

        maven("https://jitpack.io/") {
            name = "JitPack"
        }

        maven("https://maven.cafeteria.dev/releases/") {
            name = "Cafeteria Dev"
        }

        maven("https://maven.jamieswhiteshirt.com/libs-release") {
            name = "JamiesWhiteShirt Dev"
            content {
                includeGroup("com.jamieswhiteshirt")
            }
        }

        maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
            name = "Fuzs Mod Resources"
        }

        maven("https://maven.neoforged.net/releases") {
            name = "NeoForged Maven"
        }

        maven("https://maven.architectury.dev") {
            name = "Architectury"
        }

        maven("https://maven.parchmentmc.org") {
            name = "ParchmentMC"
        }

        flatDir {
            dir("libs")
        }

        // Testing mod sources
        maven("https://api.modrinth.com/maven") {
            name = "Modrinth"
            content {
                includeGroup("maven.modrinth")
            }
        }

        maven("https://cursemaven.com") {
            name = "CurseMaven"
            content {
                includeGroup("curse.maven")
            }
        }

        maven("https://maven.terraformersmc.com/releases") {
            name = "TerraformersMC"
        }

        maven("https://maven.su5ed.dev/releases") {
            name = "Su5ed"
        }

        maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
            name = "GeckoLib"
        }

        maven("https://thedarkcolour.github.io/KotlinForForge/") {
            name = "Kotlin for Forge"
        }

        maven("https://maven.blamejared.com") {
            name = "BlameJared"
        }
    }

    // Avoid making the compats submodule use Loom, otherwise we break stuff
    if (project.name == "compat")
        return@allprojects

    // Prevent other Knit Loader modules from going through Fabric Loom.
    if (project.name == "loader" || (project.parent?.name == "loader"))
        return@allprojects

    // Prevent the annotation processor from going through it too.
    if (project.name == "ap")
        return@allprojects

    apply(plugin = "fabric-loom")

    dependencies {
        // To change the versions see the gradle.properties file
        minecraft ("com.mojang:minecraft:${rootProject.property("minecraft_version")}")
        mappings (loom.layered {
            mappings(rootProject.file("workarounds/fix_yarn_mapping.tiny")) // for the cases where other mods are making the mistake of using Yarn and having conflicting names
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${rootProject.property("parchment_version")}:${rootProject.property("parchment_release")}@zip")
        })
        modImplementation ("net.fabricmc:fabric-loader:${rootProject.property("loader_version")}")

        // Just because I like Kotlin more than Java
        modImplementation ("prodRuntimeDep"("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_kotlin_version")}")!!)

        /*(implementation(annotationProcessor("io.github.llamalad7:mixinextras-fabric:${rootProject.property("mixinextras_version")}") {
            exclude("org.ow2.asm")
        })!!)*/

        implementation("com.moulberry:mixinconstraints:${rootProject.property("mixinconstraints_version")}") {
            exclude("org.spongepowered", "mixin")
            exclude("org.ow2.asm")
        }

        if (project.parent?.name != "loader") {
            // Fabric API. This is technically optional, but you probably want it anyway.
            modImplementation ("prodRuntimeDep"("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_version")}")!!)

            // Cursed Fabric/Mixin stuff
            implementation("com.github.FabricCompatibilityLayers.CursedMixinExtensions:CursedMixinExtensions:${rootProject.property("cursedmixinextensions_version")}") {
                exclude("org.ow2.asm")
            }
            // Fabric-ASM with a neutral mod ID, see rebrandFabricAsm at the end of this file
            modImplementation(files(nytheriaCompatFabricAsmJar))
            implementation(annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-fabric:${rootProject.property("mixin_squared_version")}") {
                exclude("org.ow2.asm")
            })
            modApi("de.florianreuth:asmfabricloader:${property("asmfabricloader_version")}") {
                exclude("org.ow2.asm")
            }
        }
    }
}

val cichlidDep by configurations.creating

val relocateCichlid = tasks.register<ShadowJar>("relocateCichlid") {
    description = "Relocates the shaded CichlidMC dependencies so we don't conflict with anyone."

    configurations = listOf(cichlidDep)
    archiveClassifier.set("cichlid-libs")

    relocate("fish.cichlidmc", "dev.nytheria.compat.shaded.cichlidmc")
}

dependencies {
    // Forge Reimplementations
    val portingLibs = listOf("base", "blocks", "brewing", "chunk_loading", "common", "config", "core", "data", "entity", "fluids", "gametest", "gui_utils", "item_abilities", "items", "level_events", "loot", "mixin_extensions", "model_loader", "models", "registry", "render_types", "resources", "tags", "transfer")
    portingLibs.forEach { lib ->
        modApi(include("io.github.fabricators_of_create.Porting-Lib:$lib:${property("porting_lib_version")}")!!)
    }

    // JiJ'd into main JAR alone
    //include("io.github.llamalad7:mixinextras-fabric:${property("mixinextras_version")}")
    include("com.github.FabricCompatibilityLayers.CursedMixinExtensions:CursedMixinExtensions:${property("cursedmixinextensions_version")}")
    // Fabric-ASM is nested by remapJar (nestedJars), see nytheriaCompatFabricAsmJar
    include("com.github.bawnorton.mixinsquared:mixinsquared-fabric:${rootProject.property("mixin_squared_version")}")
    include("de.florianreuth:asmfabricloader:${property("asmfabricloader_version")}")
    include("com.moulberry:mixinconstraints:${rootProject.property("mixinconstraints_version")}") {
        exclude("org.spongepowered", "mixin")
    }

    // Extra libraries that should be shaded
    cichlidDep("xyz.bluspring.fork:fishflakes:${property("fishflakes_version")}")
    cichlidDep("xyz.bluspring.fork:tiny-json:${property("tinyjson_version")}")
    cichlidDep("xyz.bluspring.fork:tiny-codecs:${property("tinycodecs_version")}")

    api(relocateCichlid.get().outputs.files)

    // Forge stuff
    api(include("net.neoforged:bus:${property("eventbus_version")}") {
        exclude("org.ow2.asm")
    })
    implementation(include("org.apache.maven:maven-artifact:3.8.5")!!)
    api(include("cpw.mods:securejarhandler:${property("securejarhandler_version")}") {
        exclude("org.ow2.asm")
    })
    implementation(include("net.jodah:typetools:0.6.3")!!)
    implementation(include("net.neoforged:mergetool:2.0.0") {
        exclude("org.ow2.asm")
    })
    implementation(include("org.jline:jline-reader:3.12.+")!!)
    implementation(include("net.minecrell:terminalconsoleappender:1.3.0")!!)

    // I don't know where the fuck these libraries come from, but some NeoForge mods depend on them annoyingly, but I can't
    // find them in any of Neo's buildscripts at all.

    // Remapping SRG to Intermediary
    implementation(include("xyz.bluspring:srgutils:${property("srgutils_version")}")!!)
    implementation(include("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")!!)


    // ForgeAutoRenamingTool, from a fork of Sinytra Connector's fork
    implementation(include("xyz.bluspring:AutoRenamingTool:${property("forgerenamer_version")}") {
        exclude("org.ow2.asm")
        exclude("net.sf.jopt-simple") // otherwise prod crashes
    })

    fun modOptional(dependencyNotation: String, shouldRunInRuntime: Boolean, configuration: Action<ExternalModuleDependency> = Action {}) {
        if (shouldRunInRuntime) {
            modImplementation(dependencyNotation, configuration)
        } else {
            modCompileOnly(dependencyNotation, configuration)
        }
    }

    val runSodium = true

    // Runtime mods for testing
    modImplementation ("com.terraformersmc:modmenu:11.0.3") {
        exclude("net.fabricmc", "fabric-loader")
    }
    modRuntimeOnly ("maven.modrinth:ferrite-core:7.0.2-hotfix-fabric") {
        exclude("net.fabricmc", "fabric-loader")
    }
    "prodRuntimeDep"("maven.modrinth:sodium:${property("sodium_version")}")
    modOptional ("maven.modrinth:sodium:${property("sodium_version")}", runSodium)
    modRuntimeOnly ("maven.modrinth:lithium:mc1.21.1-0.15.2-fabric") {
        exclude("net.fabricmc", "fabric-loader")
    }
    modOptional("maven.modrinth:iris:${property("iris_version")}", runSodium && false) // can't test Iris in dev atm

    // Need this for Iris
    modRuntimeOnly("io.github.douira:glsl-transformer:2.0.1")
    modRuntimeOnly("org.antlr:antlr4-runtime:4.13.1")
    modRuntimeOnly("org.anarres:jcpp:1.4.14")

    // apparently I need this for Nullable to exist
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation(include("commons-codec:commons-codec:1.15")!!)

    // Compatibility layers
    listOf("fabric-compats", "forge-config-api").forEach { layer ->
        runtimeOnly(project(":compat:$layer", configuration = "namedElements"))
    }

    // Knit Loader
    api(project(":loader"))
    runtimeOnly(project(":loader:fabric", configuration = "namedElements"))
    include(project(":loader:fabric")) {
        isTransitive = false
    }
    /*include(project(":loader:quilt")) {
        isTransitive = false
    }*/

    // Test libraries
    testImplementation("net.fabricmc:fabric-loader-junit:${property("loader_version")}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.+")
    testImplementation("org.opentest4j:opentest4j:1.2.0") // needed for junit 5
    testImplementation("org.hamcrest:hamcrest-all:1.3") // needs advanced matching for list order
}

// yoinked - https://github.com/devOS-Sanity-Edition/Stew/blob/1.21.9/main/build.gradle.kts#L70C10-L80C6
// FIXME: why does this not work.
//loom.runs {
//    afterEvaluate {
//        configureEach {
//            vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
//            vmArg("-XX:+IgnoreUnrecognizedVMOptions") // in the case the below doesnt work bc that JVM doesnt have it
//            vmArg("-XX:+AllowEnhancedClassRedefinition")
//            property("mixin.hotSwap", "true")
//            property("mixin.debug.export", "true")
//            property("classtransform.dumpClasses", "true")
//        }
//    }
//}

configurations.all {
    exclude("cpw.mods", "modlauncher")
}

// why isn't this default?
sourceSets.getByName("gametest").compileClasspath += sourceSets.getByName("test").compileClasspath
sourceSets.getByName("gametest").runtimeClasspath += sourceSets.getByName("test").runtimeClasspath

val targetJavaVersion = 21

kotlin {
    jvmToolchain(targetJavaVersion)
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }

    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion

    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

tasks {
    test {
        useJUnitPlatform()

        // src/test holds plain unit tests only. fabric-loader-junit (see the test libraries above) registers a JUnit
        // session listener that starts Fabric Loader inside the test JVM, and with it Nytheria Compat's own mod loading and
        // remapping, which needs the game set-up of a Loom run and otherwise ends in Fabric Loader's error window.
        // So it stays off this task's class path, and the test JVM is headless in any case.
        classpath = classpath.filter { !it.name.startsWith("fabric-loader-junit-") }
        systemProperty("java.awt.headless", "true")
        inputs.file("tools/trim/hook-rules-allowed.txt")
    }

    remapJar {
        nestedJars.from(nytheriaCompatFabricAsmJar)
    }

    processResources {
        val properties = mutableMapOf(
            "version" to project.version,
            "loader_version" to project.property("loader_min_version"),
            "fabric_version" to project.property("fabric_version"),
            "minecraft_version" to project.property("minecraft_version"),
            "fabric_kotlin_version" to project.property("fabric_kotlin_version"),
            "fabric_asm_version" to project.property("fabric_asm_version")
        )

        for ((key, value) in properties) {
            inputs.property(key, value)
        }

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            // Use this instead of expand, as otherwise Gradle hard-errors when finding unknown $ names, and treats them as properties.
            this.filter {
                if (it.contains("\${")) {
                    var newString = it

                    for ((name, property) in properties) {
                        newString = newString.replace("\${$name}", property.toString())
                    }

                    return@filter newString
                }

                it
            }
        }

        // Rename NeoForge's mods.toml, so launchers like Prism don't end up detecting it over Nytheria Compat.
        filesMatching("META-INF/neoforge.mods.toml") {
            this.name = "nytheria_compat_neoforge.mods.toml"
        }

        exclude("log4j2.xml")
    }

    processTestResources {
        filesMatching("META-INF/neoforge.mods.toml") {
            this.name = "nytheria_compat_neoforge.mods.toml"
        }
    }

    compileKotlin {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xexplicit-backing-fields"))
            jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
        }
    }

    compileTestKotlin {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${archiveBaseName.get()}" }
        }

        from("NOTICE") {
            rename { "${it}_${archiveBaseName.get()}" }
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(zipTree(relocateCichlid.get().archiveFile))
    }

    named<Jar>("sourcesJar") {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    register("setupDevEnvironment") {
        group = "nytheria_compat"

        doLast {
            val configDir = File("$projectDir/run/config")
            if (!configDir.exists())
                configDir.mkdirs()

            val loaderDepsFile = File(configDir, "fabric_loader_dependencies.json")

            if (!loaderDepsFile.exists())
                loaderDepsFile.createNewFile()

            loaderDepsFile.writeText(File("$projectDir/gradle/loader_dep_overrides.json").readText())
        }
    }

    register("updateTweakers") {
        group = "nytheria_compat"

        doLast {
            ClassTweakerUpdater.updateTweakers(rootProject)
        }
    }

    register("runProdClient", ClientProductionRunTask::class) {
        mods.from(configurations.getByName("prodRuntimeDep"))

        jvmArgs.add("-Dnytheriacompat.forceRemap=true")
        jvmArgs.add("-Dnytheriacompat.forceProductionRemap=true")
        jvmArgs.add("-XX:+AllowEnhancedClassRedefinition")
        jvmArgs.add("-Dmixin.debug.export=true")

        runDir = file("run")
    }
}

fun isRelease(): Boolean {
    return rootProject.findProperty("build.release")?.toString()?.equals("true", ignoreCase = true) == true
}

// Versioning format:
//   <mod_version>+mc<minecraft_version>[-local.<shortsha>]
// The "-local.<shortsha>" suffix is dropped when building with -Pbuild.release=true.
// Git tags are intentionally never parsed, so arbitrary tag names cannot break the build.
fun createVersion(): String {
    val modVersion = rootProject.property("mod_version") as String
    val mcVersion = rootProject.property("minecraft_version") as String

    return "$modVersion+mc$mcVersion"
}

fun getVersionMetadata(): String {
    if (isRelease())
        return ""

    return "-local.${shortCommitHash()}"
}

fun shortCommitHash(): String {
    // CI provides the full SHA, locally we ask git. Fall back to "unknown" if neither is available.
    System.getenv("GITHUB_SHA")?.takeIf { it.length >= 7 }?.let { return it.substring(0, 7) }

    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() == 0 && output.matches(Regex("[0-9a-f]{4,40}"))) output else "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}

// Fabric-ASM (Manningham Mills), used for early risers and enum extension, comes from a fork whose own
// fabric.mod.json uses the mod ID "mm_kilt" and a name that would show up in Mod Menu. Its code never looks up its
// own mod ID: it only reads the entrypoint/custom key "mm_kilt:early_risers" (which therefore stays in Nytheria Compat's
// fabric.mod.json) and loads its mixin config by the file name given in its fabric.mod.json. So a copy with a neutral
// mod ID, name and file names is used everywhere instead (compile classpath, dev runs and the nested jar).
// The classes, authors, licence and version are unchanged.

fun rebrandFabricAsm(): File {
    val version = rootProject.property("fabric_asm_version") as String
    val oldId = "mm_kilt"
    val newId = "mm_nytheria_compat"
    val newName = "Manningham Mills (Nytheria Compat build)"
    val revision = 2 // bump when the rewrite below changes

    val source = rootProject.configurations.detachedConfiguration(
        rootProject.dependencies.create("xyz.bluspring.fork:Fabric-ASM:$version")
    ).apply { isTransitive = false }.singleFile

    // Outside build/ so that "clean" in the same Gradle invocation cannot delete it after configuration.
    val out = rootProject.file(".gradle/nytheria_compat/fabric-asm/Fabric-ASM-$version.jar")
    val stampFile = File(out.parentFile, "${out.name}.stamp")
    val stamp = "${source.name}:${source.length()}:$revision"
    if (out.isFile && stampFile.isFile && stampFile.readText() == stamp)
        return out

    out.parentFile.mkdirs()
    ZipFile(source).use { zip ->
        ZipOutputStream(out.outputStream().buffered()).use { zos ->
            for (entry in zip.entries().asSequence()) {
                var name = entry.name
                var bytes = if (entry.isDirectory) ByteArray(0) else zip.getInputStream(entry).use { it.readBytes() }

                when (name) {
                    "fabric.mod.json" -> {
                        @Suppress("UNCHECKED_CAST")
                        val json = groovy.json.JsonSlurper().parseText(bytes.toString(Charsets.UTF_8)) as MutableMap<String, Any?>
                        check(json["id"] == oldId) { "Unexpected Fabric-ASM mod ID ${json["id"]}, update rebrandFabricAsm()" }
                        json["id"] = newId
                        json["name"] = newName
                        json.remove("contact")
                        (json["icon"] as? String)?.let { json["icon"] = it.replace("assets/$oldId/", "assets/$newId/") }
                        json["mixins"] = (json["mixins"] as List<*>).map { mixin ->
                            when (mixin) {
                                is String -> mixin.replace(oldId, newId)
                                is Map<*, *> -> mixin.mapValues { (k, v) -> if (k == "config") (v as String).replace(oldId, newId) else v }
                                else -> mixin
                            }
                        }
                        bytes = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json)).toByteArray(Charsets.UTF_8)
                    }
                    "mixins.$oldId.json" -> {
                        name = "mixins.$newId.json"
                        bytes = bytes.toString(Charsets.UTF_8).replace("mixins.$oldId.refmap.json", "mixins.$newId.refmap.json").toByteArray(Charsets.UTF_8)
                    }
                    "mixins.$oldId.refmap.json" -> name = "mixins.$newId.refmap.json"
                }
                name = name.replace("assets/$oldId/", "assets/$newId/")

                zos.putNextEntry(ZipEntry(name).apply { time = entry.time })
                zos.write(bytes)
                zos.closeEntry()
            }
        }
    }
    stampFile.writeText(stamp)
    return out
}
