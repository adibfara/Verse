package com.github.adibfara.mds.core.model

enum class FileKind {
    Markdown,
    Text,
    Image,
    Other;

    val isEditable: Boolean get() = this == Markdown || this == Text

    companion object {
        private val markdownExtensions = setOf("md", "markdown", "mdown", "mkd")
        private val textExtensions = setOf("txt", "text", "log", "json", "yaml", "yml", "xml", "csv")
        private val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg")

        fun fromExtension(extension: String): FileKind = when (extension.lowercase()) {
            in markdownExtensions -> Markdown
            in textExtensions -> Text
            in imageExtensions -> Image
            else -> Other
        }
    }
}

data class FileNode(
    val path: FilePath,
    val name: String,
    val isDirectory: Boolean,
    val kind: FileKind,
    val lastModified: Long,
    val size: Long,
) {
    val isMarkdown: Boolean get() = !isDirectory && kind == FileKind.Markdown
}
