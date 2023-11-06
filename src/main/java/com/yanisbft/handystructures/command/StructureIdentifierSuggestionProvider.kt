package com.yanisbft.handystructures.command

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture
import kotlin.io.path.exists

class StructureIdentifierSuggestionProvider(
    private val fieldName: String,
): SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val structureManager = context.source.world.structureTemplateManager
        structureManager.streamTemplates()
            .filter {
                // Minecraft think that built-in structures lays in generated folder
                // Which is not true. So we can leave only player created ones
                structureManager.getTemplatePath(it, ".nbt").exists()
            }
            .map { it.toString() }
            .filter {
                try {
                    it.contains(IdentifierArgumentType.getIdentifier(context, fieldName).toString())
                } catch (_: IllegalArgumentException) {
                    true
                }
            }
            .forEach { builder.suggest(it) }
        return builder.buildFuture()
    }
}