Building ConnectionViewer
=========================

Requirements
------------

A JDK 21 or newer. Nothing else — the Gradle wrapper (`./gradlew`) downloads
Gradle, and `settings.gradle.kts` enables toolchain auto-provisioning, so Gradle
downloads a JDK 21 for compiling if the one on your `PATH` is a different
version.

On macOS with Homebrew:

    brew install openjdk@21
    mkdir -p ~/Library/Java/JavaVirtualMachines
    ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
            ~/Library/Java/JavaVirtualMachines/openjdk-21.jdk

(The symlink makes the keg-only Homebrew JDK visible to the macOS `java`
wrapper, so `./gradlew` finds it. No `sudo` required.)

Two products, one source tree
-----------------------------

| | source set | VRL dependency | output |
|---|---|---|---|
| standalone app | `main` (excludes `edu/gcsc/connectionviewer/vrl/**`) | **none** | `build/libs/ConnectionViewer.jar` |
| VRL plugin | `vrl` (only `edu/gcsc/connectionviewer/vrl/**`) | `eu.mihosoft.vrl:vrl` | `build/vrl-plugin/ConnectionViewer.jar` |

The app neither compiles against nor ships VRL, so `run`, `fatJar` and `macApp`
work on a machine that has never seen VRL. The plugin jar contains the app
classes plus the VRL glue but deliberately **excludes** the VRL library itself —
the host supplies it, and VRL rejects plugin jars whose name starts with
`vrl-<digits>`.

Tasks
-----

    ./gradlew fatJar             # app: build/libs/ConnectionViewer.jar (no VRL)
    ./gradlew run                # compile and launch the GUI
    ./gradlew macApp             # build/jpackage/ConnectionViewer.app (macOS only)
    ./gradlew macDmg             # build/distributions/ConnectionViewer-<version>.dmg

    ./gradlew vrlPlugin          # plugin: build/vrl-plugin/ConnectionViewer.jar
    ./gradlew installVRLPlugin   # ...and copy it into VRL's plugin-updates folder

    ./gradlew clean

`installVRLPlugin` installs to `~/.vrl/<VRL major version>/default/plugin-updates`
(currently `~/.vrl/0.4.4/default/plugin-updates`). Override the base folder with:

    ./gradlew installVRLPlugin -PvrlDir=/path/to/vrl/config

This replaces the `vrldir` entry in the old `build.properties`.

Running
-------

    java -jar build/libs/ConnectionViewer.jar
    java -jar build/libs/ConnectionViewer.jar resources/examples/bodensee.mat

Command-line options (full list in the header comment of
`ConnectionViewer.java`):

    java -jar build/libs/ConnectionViewer.jar resources/examples/bodensee.mat \
        -scaleZoom 0.99 -height 700 -width 950 -drawConnections 1 \
        -exportPDF out.pdf -quit

Via Gradle, pass arguments with `--args`:

    ./gradlew run --args="resources/examples/Hedgehog.mat -exportPDF out.pdf -quit"

Dependencies
------------

* `org.swinglabs:swing-layout:1.0.3` — replaces the `swing-layout-1.0.4.jar` that
  used to come from the NetBeans IDE libraries. API-compatible with the
  `GroupLayout` code the NetBeans form editor generates.
* `de.erichseifert.vectorgraphics2d:VectorGraphics2D:0.9.3` — the last release
  where `PDFGraphics2D` is still a `Graphics2D` subclass. 0.10+ moved to a
  processor-based API and would require rewriting `Export.java`.
* `eu.mihosoft.vrl:vrl:0.4.4.0.0` — **plugin source set only**. The old build
  asked for `0.4.3.2.4`, which was published to jcenter only and has been
  unresolvable since jcenter shut down. 0.4.4.0.0 is the newest release on Maven
  Central and satisfies the range declared in `CustomPluginConfigurator`
  (`"0.4.0"` .. `"0.4.x"`).

macOS bundle
------------

`macApp` uses `jpackage`, replacing `resources/AppTemplate.app` — that template's
`JavaApplicationStub` relied on a system-installed JVM, which macOS has not
shipped since 10.6. The bundle embeds its own runtime, built by `jlinkRuntime`
with only the modules the app uses (`java.base`, `java.desktop`, `java.logging`,
per `jdeps`); that keeps it at ~45 MB instead of the ~160 MB a full JDK costs.
It carries the same `.mat`/`.pmat`/`.vec`/`.pvec`/`.tarmat` file associations
`AppTemplate.app` declared, so opening one of those from Finder reaches
`MacOSXHelper`'s open-file handler.

`macDmg` wraps the same bundle in a compressed disk image
(`build/distributions/ConnectionViewer-<version>.dmg`, ~30 MB versus ~45 MB for
the plain `.app`). Prefer it for distribution; on macOS the only meaningful size
lever is compression, since `jlink` cannot shrink `java.desktop` or HotSpot
further (`--strip-native-debug-symbols` is Linux-only, and dropping
`java.logging` saves 8 KB).

The bundle is **unsigned**: on another Mac, Gatekeeper blocks it until the user
right-clicks and chooses Open. Signing requires an Apple Developer ID and
`jpackage --mac-sign --mac-signing-key-user-name`.

Source layout
-------------

Sources follow the Maven convention under `src/main/java`, with project assets
(examples, app template, images, gnome-mime) under `resources/`. The `.form`
files are NetBeans GUI-designer metadata — needed only to edit the UI in
NetBeans; the generated code already lives in the `.java` files.
