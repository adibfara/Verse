package com.github.adibfara.verse.core.command

class CommandRegistry(
    commands: List<Command> = Commands.all,
    overrides: Map<CommandId, KeyBinding> = emptyMap(),
) {

    val commands: List<Command> = commands.map { command ->
        overrides[command.id]?.let { command.copy(binding = it) } ?: command
    }

    private val byId: Map<CommandId, Command> = this.commands.associateBy { it.id }

    private val byBinding: Map<KeyBinding, CommandId> = this.commands
        .mapNotNull { command -> command.binding?.let { it to command.id } }
        .toMap()

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
        .mapNotNull { it.binding }
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
        .toList()
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
