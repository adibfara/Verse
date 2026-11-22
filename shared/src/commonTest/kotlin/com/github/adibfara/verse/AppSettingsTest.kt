package com.github.adibfara.verse

import com.github.adibfara.verse.core.command.Commands
import com.github.adibfara.verse.core.command.KeyBinding
import com.github.adibfara.verse.core.command.ShortcutKey
import com.github.adibfara.verse.core.persistence.AppSettings
import com.github.adibfara.verse.core.persistence.EditorFontSizeStep
import com.github.adibfara.verse.core.persistence.MaxEditorFontSize
import com.github.adibfara.verse.core.persistence.MinEditorFontSize
import com.github.adibfara.verse.core.persistence.VerseJson
import com.github.adibfara.verse.core.persistence.withAdjustedEditorFontSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class AppSettingsTest {

    @Test
    fun fontSizeGrowsInSteps() {
        val settings = AppSettings(editorFontSize = 15.5f)
        assertEquals(16.5f, settings.withAdjustedEditorFontSize(EditorFontSizeStep).editorFontSize)
        assertEquals(14.5f, settings.withAdjustedEditorFontSize(-EditorFontSizeStep).editorFontSize)
    }

    @Test
    fun fontSizeClampsAtBounds() {
        val above = AppSettings(editorFontSize = MaxEditorFontSize)
        assertEquals(MaxEditorFontSize, above.withAdjustedEditorFontSize(EditorFontSizeStep).editorFontSize)

        val below = AppSettings(editorFontSize = MinEditorFontSize)
        assertEquals(MinEditorFontSize, below.withAdjustedEditorFontSize(-EditorFontSizeStep).editorFontSize)
    }

    @Test
    fun keyBindingOverridesRoundTripThroughJson() {
        val settings = AppSettings(
            keyBindingOverrides = mapOf(
                Commands.NewNote.id.value to KeyBinding(ShortcutKey.K, control = true),
                Commands.Save.id.value to null,
            ),
        )
        val restored = VerseJson.decodeFromString<AppSettings>(VerseJson.encodeToString(settings))
        assertEquals(settings.keyBindingOverrides, restored.keyBindingOverrides)
    }
}
