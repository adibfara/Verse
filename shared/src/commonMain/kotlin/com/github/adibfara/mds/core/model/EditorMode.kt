package com.github.adibfara.mds.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class EditorMode {
    Preview,
    Live,
    Raw,
}
