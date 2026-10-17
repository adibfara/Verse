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

val envOrNull = { name: String ->
    providers.environmentVariable(name).map { it.trim() }.filter { it.isNotEmpty() }
}

val macSignIdentity = envOrNull("VERSE_MAC_SIGN_IDENTITY")
val macSignKeychain = envOrNull("VERSE_MAC_SIGN_KEYCHAIN")
val appleId = envOrNull("VERSE_APPLE_ID")
val applePassword = envOrNull("VERSE_APPLE_PASSWORD")
val appleTeamId = envOrNull("VERSE_APPLE_TEAM_ID")
val appVersion = envOrNull("VERSE_VERSION").orNull ?: "1.0.0"

compose.desktop {
    application {
        mainClass = "com.github.adibfara.verse.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Verse"
            packageVersion = appVersion
            description = "A fast, simple markdown workspace"

            windows {
                iconFile.set(project.file("icons/verse.ico"))
            }
            macOS {
                bundleID = "io.github.adibfara.verse"
                iconFile.set(project.file("icons/verse.icns"))
                signing {
                    sign.set(macSignIdentity.map { true }.orElse(false))
                    macSignIdentity.orNull?.let { identity.set(it) }
                    macSignKeychain.orNull?.let { keychain.set(it) }
                }
                notarization {
                    appleId.orNull?.let { appleID.set(it) }
                    applePassword.orNull?.let { password.set(it) }
                    appleTeamId.orNull?.let { teamID.set(it) }
                }
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

tasks.matching { it.name.startsWith("notarize") }.configureEach {
    notCompatibleWithConfigurationCache("Compose notarization settings carry a throwing deprecated property")
}