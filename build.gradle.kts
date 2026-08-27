/*
 * Copyright 2016-2018 G-CSC, Uni-Frankfurt.de. All rights reserved.
 * Copyright 2014-2018 Michael Hoffer <info@michaelhoffer.de>  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Michael Hoffer <info@michaelhoffer.de>.
 */

plugins {
    java
    application
}

group = "edu.gcsc.connectionviewer"
version = "3.4"

// The old build used 0.4.3.2.4, which was only ever published to jcenter and is
// therefore unresolvable since jcenter shut down. 0.4.4.0.0 is the newest
// release on Maven Central and satisfies the plugin's declared VRL range
// ("0.4.0" .. "0.4.x", see CustomPluginConfigurator).
val vrlVersion = "0.4.4.0.0"

// ---------------------------------------------------------------------------
// Two independent products from one source tree:
//
//   * the standalone app  - source set `main`, has NO VRL dependency at all
//   * the VRL plugin      - source set `vrl`, adds edu/gcsc/connectionviewer/vrl
//
// The app therefore neither compiles against nor ships VRL, and `./gradlew run`
// or `fatJar` work with no VRL artifact on the machine.
// ---------------------------------------------------------------------------
sourceSets {
    main {
        java.exclude("edu/gcsc/connectionviewer/vrl/**")
    }
}

val vrlSourceSet = sourceSets.create("vrl") {
    java.setSrcDirs(listOf("src/main/java"))
    java.include("edu/gcsc/connectionviewer/vrl/**")
    compileClasspath += sourceSets.main.get().output
}

// The plugin source set needs everything the app needs, plus VRL itself.
configurations["vrlImplementation"].extendsFrom(configurations.implementation.get())

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Was swing-layout-1.0.4.jar from the NetBeans IDE libraries; 1.0.3 is the
    // newest release on Maven Central and is API-compatible with the
    // GroupLayout code the NetBeans form editor generates.
    implementation("org.swinglabs:swing-layout:1.0.3")
    implementation("de.erichseifert.vectorgraphics2d:VectorGraphics2D:0.9.3")

    "vrlImplementation"("eu.mihosoft.vrl:vrl:$vrlVersion")

    testImplementation("junit:junit:4.13.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Code predates generics-everywhere; don't drown the build in warnings.
    options.compilerArgs.addAll(listOf("-Xlint:none", "-nowarn"))
}

application {
    mainClass = "edu.gcsc.connectionviewer.ConnectionViewer"
    applicationDefaultJvmArgs = listOf("-Xms64m", "-Xmx2048m", "-Xdock:name=ConnectionViewer")
}

// ---------------------------------------------------------------------------
// App
// ---------------------------------------------------------------------------

// Self-contained jar, no VRL:
//   java -jar build/libs/ConnectionViewer.jar [file.mat]
val fatJar = tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Builds a runnable jar of the standalone app (without VRL)."
    archiveFileName = "ConnectionViewer.jar"
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Title" to "ConnectionViewer",
            "Implementation-Version" to project.version,
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/MANIFEST.MF")
    }
}

// ---------------------------------------------------------------------------
// VRL plugin
// ---------------------------------------------------------------------------

// The plugin jar carries the app classes plus the VRL glue, but must NOT bundle
// VRL itself - the host provides it, and VRL rejects plugin jars whose name
// starts with "vrl-<digits>". Written to its own directory so it cannot be
// confused with the app jar of the same name.
val vrlPlugin = tasks.register<Jar>("vrlPlugin") {
    group = "build"
    description = "Builds the VRL plugin jar (app classes + VRL glue, VRL excluded)."
    archiveFileName = "ConnectionViewer.jar"
    destinationDirectory = layout.buildDirectory.dir("vrl-plugin")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output, vrlSourceSet.output)
    from(
        configurations["vrlRuntimeClasspath"]
            .filter { !it.name.startsWith("vrl-") }
            .map { if (it.isDirectory) it else zipTree(it) }
    ) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/MANIFEST.MF")
    }
}

// Destination for installVRLPlugin. Override with -PvrlDir=/some/path,
// otherwise ~/.vrl/<VRL major version>/default, matching the layout VRL's
// eu.mihosoft.vrl.system.Constants.VERSION_MAJOR produces (e.g. "0.4.4").
val vrlDir: String = (findProperty("vrlDir") as String?)?.takeIf { it.isNotBlank() }
    ?: "${System.getProperty("user.home")}/.vrl/${vrlVersion.split(".").take(3).joinToString(".")}/default"

tasks.register<Copy>("installVRLPlugin") {
    group = "distribution"
    description = "Builds the VRL plugin and installs it into VRL's plugin-updates folder."
    from(vrlPlugin)
    into("$vrlDir/plugin-updates")
    doFirst { logger.lifecycle(">> copying vrl plugin to: $vrlDir/plugin-updates") }
}

// ---------------------------------------------------------------------------
// macOS bundle
// ---------------------------------------------------------------------------

// Minimal Java runtime for the .app bundle. Without this, jpackage embeds a
// full JDK (~160 MB); jlinking only the modules the app actually uses cuts the
// bundle to ~45 MB. Module list comes from:
//   jdeps --print-module-deps --ignore-missing-deps build/libs/ConnectionViewer.jar
// Re-run that if the dependencies ever change.
val jlinkRuntime = tasks.register<Exec>("jlinkRuntime") {
    group = "distribution"
    description = "Builds a stripped-down Java runtime for the app bundle."

    val jlink = javaToolchains.launcherFor(java.toolchain).get()
        .metadata.installationPath.file("bin/jlink").asFile
    val runtimeDir = layout.buildDirectory.dir("runtime")
    outputs.dir(runtimeDir)

    // jlink refuses to write into an existing directory.
    doFirst { delete(runtimeDir) }
    commandLine(
        jlink.absolutePath,
        "--add-modules", "java.base,java.desktop,java.logging",
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages",
        "--compress", "zip-6",
        "--output", runtimeDir.get().asFile.absolutePath,
    )
}

// Native macOS packaging via jpackage, replacing resources/AppTemplate.app
// (whose JavaApplicationStub relied on a system JVM that macOS no longer ships).
//
// `macApp` produces the plain .app directory, `macDmg` a compressed disk image
// for distribution - the .app is ~45 MB on disk, the .dmg roughly a third less,
// because almost all of it is the embedded Java runtime.
val jpackageIcon = file("resources/AppTemplate.app/Contents/Resources/GenericApp.icns")

// Keeps the CFBundleDocumentTypes resources/AppTemplate.app declared, so
// double-clicking a .mat in Finder reaches MacOSXHelper's open-file handler.
val jpackageFileAssoc = layout.buildDirectory.file("tmp/jpackage-file-associations.properties")

fun writeFileAssociations() {
    jpackageFileAssoc.get().asFile.apply {
        parentFile.mkdirs()
        writeText(
            """
            extension=mat pmat vec pvec tarmat
            mime-type=application/x-connectionviewer-matrix
            description=ConnectionViewer matrix
            """.trimIndent()
        )
    }
}

fun jpackageArgs(type: String, destDir: Directory): List<String> = listOf(
    javaToolchains.launcherFor(java.toolchain).get()
        .metadata.installationPath.file("bin/jpackage").asFile.absolutePath,
    "--type", type,
    "--name", "ConnectionViewer",
    "--app-version", project.version.toString(),
    "--input", layout.buildDirectory.dir("libs").get().asFile.absolutePath,
    "--main-jar", "ConnectionViewer.jar",
    "--main-class", application.mainClass.get(),
    "--java-options", "-Xms64m",
    "--java-options", "-Xmx2048m",
    "--icon", jpackageIcon.absolutePath,
    "--file-associations", jpackageFileAssoc.get().asFile.absolutePath,
    "--runtime-image", layout.buildDirectory.dir("runtime").get().asFile.absolutePath,
    "--dest", destDir.asFile.absolutePath,
)

tasks.register<Exec>("macApp") {
    group = "distribution"
    description = "Builds ConnectionViewer.app using jpackage (macOS only)."
    dependsOn(fatJar, jlinkRuntime)

    val destDir = layout.buildDirectory.dir("jpackage").get()
    doFirst {
        delete(destDir.dir("ConnectionViewer.app"))
        writeFileAssociations()
    }
    commandLine(jpackageArgs("app-image", destDir))
    doLast { logger.lifecycle("Created ${destDir.dir("ConnectionViewer.app").asFile}") }
}

tasks.register<Exec>("macDmg") {
    group = "distribution"
    description = "Builds ConnectionViewer-<version>.dmg using jpackage (macOS only)."
    dependsOn(fatJar, jlinkRuntime)

    val destDir = layout.buildDirectory.dir("distributions").get()
    // jpackage refuses to overwrite an existing image, and names it
    // ConnectionViewer-<app-version>.dmg.
    val dmg = destDir.file("ConnectionViewer-${project.version}.dmg")
    doFirst {
        delete(dmg)
        writeFileAssociations()
    }
    commandLine(jpackageArgs("dmg", destDir))
    doLast { logger.lifecycle("Created ${dmg.asFile}") }
}

tasks.named("assemble") { dependsOn(fatJar) }
