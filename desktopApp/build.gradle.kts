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

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.github.adibfara.verse.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Verse"
            packageVersion = "1.0.0"
            description = "A fast, simple markdown workspace"

            windows {
                iconFile.set(project.file("icons/verse.ico"))
            }
            macOS {
                iconFile.set(project.file("icons/verse.icns"))
            }
            linux {
                iconFile.set(project.file("icons/verse.png"))
            }

            fileAssociation("text/markdown", "md", "Markdown document")
            fileAssociation("text/markdown", "markdown", "Markdown document")
            fileAssociation("text/plain", "txt", "Text document")
        }
    }
}