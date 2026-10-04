package com.github.adibfara.verse.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class EditorMode {
    Preview,
    Live,
    Raw,
}
