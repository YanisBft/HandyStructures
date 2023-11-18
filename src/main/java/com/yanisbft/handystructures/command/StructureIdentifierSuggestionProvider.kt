package com.yanisbft.handystructures.command

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
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
            .filter {
                try {
                    val id = IdentifierArgumentType.getIdentifier(context, fieldName)
                    if (id.namespace == Identifier.DEFAULT_NAMESPACE)
                        it.toString().contains(id.path)
                    else
                        it.namespace == id.namespace && it.path.contains(id.path)
                } catch (_: IllegalArgumentException) {
                    true
                }
            }
            .forEach { builder.suggest(it.toString()) }
        return builder.buildFuture()
    }
}