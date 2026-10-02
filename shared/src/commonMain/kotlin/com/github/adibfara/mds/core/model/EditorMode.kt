package com.github.adibfara.mds.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class EditorMode {
    Live,
    Raw,
    Preview;

    val isEditable: Boolean get() = this != Preview
}
