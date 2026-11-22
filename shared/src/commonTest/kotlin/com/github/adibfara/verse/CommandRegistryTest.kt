package com.github.adibfara.verse

import com.github.adibfara.verse.core.command.CommandRegistry
import com.github.adibfara.verse.core.command.Commands
import com.github.adibfara.verse.core.command.KeyBinding
import com.github.adibfara.verse.core.command.ShortcutKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandRegistryTest {

    private val registry = CommandRegistry()

    @Test
    fun noDuplicateCommandIds() {
        assertEquals(emptyList(), registry.duplicateIds())
    }

    @Test
    fun noConflictingDefaultBindings() {
        assertEquals(emptyList(), registry.conflictingBindings())
    }

    @Test
    fun resolvesBindingToCommand() {
        val binding = KeyBinding(ShortcutKey.N, control = true)
        assertEquals(Commands.NewNote.id, registry.resolve(binding))
    }

    @Test
    fun shiftVariantsResolveSeparately() {
        assertEquals(
            Commands.NewFolder.id,
            registry.resolve(KeyBinding(ShortcutKey.N, control = true, shift = true)),
        )
        assertEquals(
            Commands.PreviousTab.id,
            registry.resolve(KeyBinding(ShortcutKey.Tab, control = true, shift = true)),
        )
    }

    @Test
    fun unknownBindingResolvesToNothing() {
        assertNull(registry.resolve(KeyBinding(ShortcutKey.Q, control = true, alt = true)))
    }

    @Test
    fun overridesReplaceDefaultBindings() {
        val custom = KeyBinding(ShortcutKey.K, control = true)
        val overridden = CommandRegistry(overrides = mapOf(Commands.NewNote.id to custom))
        assertEquals(Commands.NewNote.id, overridden.resolve(custom))
        assertNull(overridden.resolve(KeyBinding(ShortcutKey.N, control = true)))
    }

    @Test
    fun tabJumpCommandsRoundTripTheirIndex() {
        (0..8).forEach { index ->
            assertEquals(index, Commands.tabIndexOf(Commands.jumpToTabId(index)))
        }
        assertNull(Commands.tabIndexOf(Commands.NewNote.id))
    }

    @Test
    fun searchFindsCommandsByPartialTitle() {
        val results = registry.search("sidebar")
        assertTrue(results.any { it.id == Commands.ToggleSidebar.id })
    }

    @Test
    fun searchWithBlankQueryReturnsEverything() {
        assertEquals(registry.commands.size, registry.search("").size)
    }

    @Test
    fun bindingLabelsAreReadable() {
        assertEquals("Ctrl+N", Commands.NewNote.binding?.label)
        assertEquals("Ctrl+Shift+N", Commands.NewFolder.binding?.label)
        assertEquals("Ctrl+,", Commands.OpenSettings.binding?.label)
        assertEquals("Ctrl+=", Commands.IncreaseFontSize.binding?.label)
        assertEquals("Ctrl+-", Commands.DecreaseFontSize.binding?.label)
    }

    @Test
    fun aliasBindingsResolveToTheirCommand() {
        assertEquals(
            Commands.IncreaseFontSize.id,
            registry.resolve(KeyBinding(ShortcutKey.Equals, control = true, shift = true)),
        )
        assertEquals(
            Commands.IncreaseFontSize.id,
            registry.resolve(KeyBinding(ShortcutKey.Plus, control = true)),
        )
    }

    @Test
    fun setOverridesUpdatesResolvedBindings() {
        val custom = KeyBinding(ShortcutKey.K, control = true)
        registry.setOverrides(mapOf(Commands.NewNote.id to custom))
        assertEquals(Commands.NewNote.id, registry.resolve(custom))
        assertNull(registry.resolve(KeyBinding(ShortcutKey.N, control = true)))
    }

    @Test
    fun nullOverrideUnbindsACommand() {
        registry.setOverrides(mapOf(Commands.NewNote.id to null))
        assertNull(registry.resolve(KeyBinding(ShortcutKey.N, control = true)))
        assertNull(registry.bindingFor(Commands.NewNote.id))
    }

    @Test
    fun overridesDropDefaultAliases() {
        val custom = KeyBinding(ShortcutKey.K, control = true)
        registry.setOverrides(mapOf(Commands.IncreaseFontSize.id to custom))
        assertNull(registry.resolve(KeyBinding(ShortcutKey.Equals, control = true)))
        assertNull(registry.resolve(KeyBinding(ShortcutKey.Plus, control = true)))
        assertEquals(Commands.IncreaseFontSize.id, registry.resolve(custom))
    }

    @Test
    fun overrideClaimsBindingFromAnotherCommandsPrimary() {
        registry.setOverrides(mapOf(Commands.NewNote.id to KeyBinding(ShortcutKey.S, control = true)))
        assertEquals(
            Commands.NewNote.id,
            registry.resolve(KeyBinding(ShortcutKey.S, control = true)),
        )
        assertNull(registry.bindingFor(Commands.Save.id))
        assertEquals(emptyList(), registry.conflictingBindings())
    }

    @Test
    fun overrideClaimsBindingFromAnotherCommandsAliasWithoutUnbindingIt() {
        val plus = KeyBinding(ShortcutKey.Plus, control = true)
        registry.setOverrides(mapOf(Commands.NewNote.id to plus))
        assertEquals(Commands.NewNote.id, registry.resolve(plus))
        assertEquals(
            Commands.IncreaseFontSize.id,
            registry.resolve(KeyBinding(ShortcutKey.Equals, control = true)),
        )
        assertEquals(emptyList(), registry.conflictingBindings())
    }
}
