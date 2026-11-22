import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.components.resources)
    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "ru.sotber.sapr_wolf.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SAPR-WOLF"
            packageVersion = "1.0.0"

            includeAllModules = true

            windows {
                menuGroup = "SAPR-WOLF"
                shortcut = true
                iconFile.set(project.file("icons/icon.ico"))
            }

            macOS {
                bundleID = "ru.sotber.sapr_wolf"
                dockName = "SAPR-WOLF"
                iconFile.set(project.file("icons/icon.icns"))
            }
        }

        jvmArgs += listOf(
            "--enable-native-access=ALL-UNNAMED"
        )
    }
}