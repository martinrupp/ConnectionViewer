// Auto-downloads a matching JDK if the toolchain requested in build.gradle.kts
// is not installed locally, so a fresh checkout builds without manual setup.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ConnectionViewer"
