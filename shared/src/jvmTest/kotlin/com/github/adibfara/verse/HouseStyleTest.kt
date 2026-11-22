package com.github.adibfara.verse

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class HouseStyleTest {

    @Test
    fun sourcesContainNoLineComments() {
        assertNoOffenders(
            rule = "Kotlin sources must not contain // comments",
            offenders = findings(LineComment).map { it.describe() },
        )
    }

    @Test
    fun colorLiteralsStayInTheTheme() {
        assertNoOffenders(
            rule = "Hard-coded colour literals are only allowed in core/designsystem/theme",
            offenders = findings(ColorLiteral)
                .filterNot { finding -> finding.path.contains(ThemeDirectory) }
                .map { it.describe() },
        )
    }

    @Test
    fun pointerButtonUsageStaysInTheJvmSourceSet() {
        assertNoOffenders(
            rule = "PointerButton is desktop-only; use core/designsystem/component/PointerInteractions.kt",
            offenders = findings(PointerButtonUsage)
                .filterNot { finding -> finding.path.startsWith(JvmSourceSet) }
                .map { it.describe() },
        )
    }

    @Test
    fun materialIconsAreNeverImported() {
        assertNoOffenders(
            rule = "material-icons-extended does not exist for Compose MP 1.12; add a VerseIcons path",
            offenders = findings(MaterialIconsImport).map { it.describe() },
        )
    }

    @Test
    fun domainLayerAvoidsComposeUiImports() {
        val offenders = sources()
            .filter { source -> source.isDomainLayer }
            .flatMap { source ->
                DomainLayerForbiddenImports
                    .filter { forbidden -> source.lines.any { it.startsWith("import $forbidden") } }
                    .map { forbidden -> "${source.path} imports $forbidden" }
            }
        assertNoOffenders(
            rule = "ViewModels, UiState files and use cases must not import Compose UI or material3",
            offenders = offenders,
        )
    }

    @Test
    fun theCheckItselfFindsTheSourcesItIsMeantToGuard() {
        val paths = sources().map { it.path }
        assertTrue(paths.contains(ExplorerViewModelPath), "Expected to scan $ExplorerViewModelPath, found $paths")
        assertTrue(paths.contains(VerseColorsPath), "Expected to scan $VerseColorsPath, found $paths")
        assertTrue(paths.size > 50, "Expected to scan the whole repository, found ${paths.size} Kotlin files")
    }

    private fun assertNoOffenders(rule: String, offenders: List<String>) {
        assertTrue(
            offenders.isEmpty(),
            "House style violation: $rule\n" + offenders.joinToString("\n") { "  $it" },
        )
    }

    private fun findings(pattern: Regex): List<Finding> = sources().flatMap { source ->
        source.lines.withIndex()
            .filter { (_, line) -> pattern.containsMatchIn(line) }
            .map { (index, line) -> Finding(path = source.path, line = index + 1, text = line) }
    }

    private fun sources(): List<Source> {
        val root = repositoryRoot()
        return root.walkTopDown()
            .onEnter { directory -> directory.name !in IgnoredDirectories }
            .filter { file -> file.isFile && file.name.endsWith(".kt") && !file.name.endsWith(".kts") }
            .map { file -> Source(file = file, root = root) }
            .toList()
    }

    private fun repositoryRoot(): File =
        generateSequence(File("").absoluteFile) { directory -> directory.parentFile }
            .firstOrNull { directory -> File(directory, "settings.gradle.kts").isFile }
            ?: error("Could not locate the repository root from ${File("").absolutePath}")

    private class Source(private val file: File, root: File) {

        val path: String = file.relativeTo(root).invariantSeparatorsPath

        val lines: List<String> = file.readLines()

        val isDomainLayer: Boolean =
            path.contains("/usecase/") ||
                file.name.endsWith("ViewModel.kt") ||
                file.name.endsWith("UiState.kt")
    }

    private class Finding(val path: String, val line: Int, val text: String) {
        fun describe(): String = "$path:$line  ${text.trim()}"
    }

    private companion object {
        const val ThemeDirectory = "/core/designsystem/theme/"
        const val JvmSourceSet = "shared/src/jvmMain/"
        const val ExplorerViewModelPath =
            "shared/src/commonMain/kotlin/com/github/adibfara/verse/feature/explorer/ExplorerViewModel.kt"
        const val VerseColorsPath =
            "shared/src/commonMain/kotlin/com/github/adibfara/verse/core/designsystem/theme/VerseColors.kt"

        val IgnoredDirectories = setOf("build", ".git", ".gradle", ".idea", ".kotlin", "node_modules")

        val LineComment = Regex("^\\s*//")
        val ColorLiteral = Regex("Color\\(0x")
        val PointerButtonUsage = Regex("PointerButton\\.")
        val MaterialIconsImport = Regex("^\\s*import androidx\\.compose\\.material\\.icons")

        val DomainLayerForbiddenImports = listOf(
            "androidx.compose.material3",
            "androidx.compose.foundation",
            "androidx.compose.material.icons",
        )
    }
}
