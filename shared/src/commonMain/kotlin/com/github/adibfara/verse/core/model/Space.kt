package com.github.adibfara.verse.core.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class SpaceId(val value: String)

@Serializable
data class Space(
    val id: SpaceId,
    val name: String,
    val location: String,
    val isEphemeral: Boolean = false,
)

@Serializable
data class RecentSpace(
    val id: SpaceId,
    val name: String,
    val location: String,
    val lastOpenedAt: Long,
)
