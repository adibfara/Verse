package com.github.adibfara.verse.core.command

class CommandRegistry(
    private val baseCommands: List<Command> = Commands.all,
    overrides: Map<CommandId, KeyBinding?> = emptyMap(),
) {

    private var currentOverrides: Map<CommandId, KeyBinding?> = overrides
    private var resolvedCommands: List<Command> = applyOverrides(baseCommands, overrides)
    private var byId: Map<CommandId, Command> = resolvedCommands.associateBy { it.id }
    private var byBinding: Map<KeyBinding, CommandId> = indexBindings(resolvedCommands)

    val commands: List<Command> get() = resolvedCommands

    val overrides: Map<CommandId, KeyBinding?> get() = currentOverrides

    fun setOverrides(overrides: Map<CommandId, KeyBinding?>) {
        currentOverrides = overrides
        resolvedCommands = applyOverrides(baseCommands, overrides)
        byId = resolvedCommands.associateBy { it.id }
        byBinding = indexBindings(resolvedCommands)
    }

    fun commandFor(id: CommandId): Command? = byId[id]

    fun resolve(binding: KeyBinding): CommandId? = byBinding[binding]

    fun bindingFor(id: CommandId): KeyBinding? = byId[id]?.binding

    fun search(query: String): List<Command> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return commands
        return commands
            .mapNotNull { command ->
                val score = fuzzyScore(command.title, trimmed)
                    ?: fuzzyScore(command.category.label, trimmed)
                score?.let { command to it }
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    fun duplicateIds(): List<CommandId> = commands
        .groupingBy { it.id }
        .eachCount()
        .filterValues { it > 1 }
        .keys
        .toList()

    fun conflictingBindings(): List<KeyBinding> = commands
        .flatMap { it.allBindings }
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
        .toList()

    private fun applyOverrides(
        commands: List<Command>,
        overrides: Map<CommandId, KeyBinding?>,
    ): List<Command> {
        val claimed = overrides.values.filterNotNull().toSet()
        return commands.map { command ->
            if (overrides.containsKey(command.id)) {
                command.copy(binding = overrides[command.id], aliasBindings = emptyList())
            } else {
                val binding = command.binding?.takeIf { it !in claimed }
                val aliases = command.aliasBindings.filterNot { it in claimed }
                when {
                    binding == command.binding && aliases == command.aliasBindings -> command
                    else -> command.copy(binding = binding, aliasBindings = aliases)
                }
            }
        }
    }

    private fun indexBindings(commands: List<Command>): Map<KeyBinding, CommandId> = commands
        .flatMap { command -> command.allBindings.map { binding -> binding to command.id } }
        .toMap()
}

fun fuzzyScore(candidate: String, query: String): Int? {
    if (query.isEmpty()) return 0
    val haystack = candidate.lowercase()
    val needle = query.lowercase()

    val exactIndex = haystack.indexOf(needle)
    if (exactIndex >= 0) return exactIndex

    var searchFrom = 0
    var score = 0
    needle.forEach { character ->
        val index = haystack.indexOf(character, searchFrom)
        if (index < 0) return null
        score += index - searchFrom + 1
        searchFrom = index + 1
    }
    return score + 1000
}
