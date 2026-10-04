package com.github.adibfara.verse.feature.explorer.usecase

private val illegalCharacters = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

sealed interface NameValidation {
    data object Valid : NameValidation
    data class Invalid(val reason: String) : NameValidation
}

fun validateName(name: String): NameValidation {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> NameValidation.Invalid("Name cannot be empty")
        trimmed == "." || trimmed == ".." -> NameValidation.Invalid("Name is reserved")
        trimmed.startsWith(".") -> NameValidation.Invalid("Name cannot start with a dot")
        trimmed.any { it in illegalCharacters } ->
            NameValidation.Invalid("Name contains an illegal character")

        trimmed.length > 180 -> NameValidation.Invalid("Name is too long")
        else -> NameValidation.Valid
    }
}

fun withMarkdownExtension(name: String): String {
    val trimmed = name.trim()
    return if (trimmed.contains('.')) trimmed else "$trimmed.md"
}
