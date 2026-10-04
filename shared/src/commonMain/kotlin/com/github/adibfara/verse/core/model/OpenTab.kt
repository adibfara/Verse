package com.github.adibfara.verse.core.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class TabId(val value: String)

data class OpenTab(
    val id: TabId,
    val path: FilePath,
    val title: String,
    val kind: FileKind,
    val isDirty: Boolean = false,
)
