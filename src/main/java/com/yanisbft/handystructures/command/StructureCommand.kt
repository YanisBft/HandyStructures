package com.yanisbft.handystructures.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.mojang.brigadier.arguments.BoolArgumentType.getBool
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.enums.StructureBlockMode
import net.minecraft.command.CommandException
import net.minecraft.command.argument.BlockPosArgumentType.blockPos
import net.minecraft.command.argument.BlockPosArgumentType.getBlockPos
import net.minecraft.command.argument.IdentifierArgumentType.getIdentifier
import net.minecraft.command.argument.IdentifierArgumentType.identifier
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.io.File
import kotlin.math.abs
import kotlin.math.min


private fun File.isEmpty(exclude: File): Boolean {
    for (file in walkTopDown().filter { it == exclude })
        return false
    return true
}

object StructureCommand {
    private const val STRUCTURE_BLOCK = "minecraft:structure_block"
    private const val STRUCTURE_BLOCK_SAVE = "$STRUCTURE_BLOCK[mode=save]"
    private val SAVE_FAILED_EXCEPTION = DynamicCommandExceptionType { name ->
        Text.translatable("structure_block.save_failure", name)
    }
    private val STRUCTURE_NOT_FOUND_EXCEPTION = DynamicCommandExceptionType { name ->
        Text.translatable("structure_block.load_not_found", name)
    }

    private object Keys {
        const val FROM = "from"
        const val TO = "to"
        const val POS = "pos"
        const val NAME = "name"
        const val IGNORE_ENTITIES = "ignoreEntities"
    }

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val structuresSuggestionsProvider = StructureIdentifierSuggestionProvider(Keys.NAME)
        dispatcher.register(literal("structure")
            .requires { commandSource -> commandSource.hasPermissionLevel(2) }
            .then(
                literal("save")
                .then(
                    literal("block").then(
                        argument(Keys.POS, blockPos()).executes { context ->
                            saveFromStructureBlock(context.source, getBlockPos(context, Keys.POS)); 0
                        }
                    )
                ).then(
                    argument(Keys.FROM, blockPos()).then(
                        argument(Keys.TO, blockPos()).then(
                            argument(Keys.NAME, identifier())
                            .suggests(structuresSuggestionsProvider)
                            .executes { context ->
                                saveStructure(
                                    context.source,
                                    getBlockPos(context, Keys.FROM),
                                    getBlockPos(context, Keys.TO),
                                    getIdentifier(context, Keys.NAME),
                                    true
                                ); 0
                            }.then(
                                argument(Keys.IGNORE_ENTITIES, bool()).executes { context ->
                                    saveStructure(
                                        context.source,
                                        getBlockPos(context, Keys.FROM),
                                        getBlockPos(context, Keys.TO),
                                        getIdentifier(context, Keys.NAME),
                                        getBool(context, Keys.IGNORE_ENTITIES)
                                    ); 0
                                }
                            )
                        )
                    )
                )
            )
            .then(
                literal("remove")
                .then(argument(Keys.NAME, identifier())
                .suggests(structuresSuggestionsProvider)
                .executes { context ->
                    removeStructure(
                        context.source,
                        getIdentifier(context, Keys.NAME),
                    ); 0
                })
                .then(
                    literal("block").then(
                        argument(Keys.POS, blockPos()).executes { context ->
                            removeStructureFromBlock(context.source, getBlockPos(context, Keys.POS)); 0
                        }
                    )
                )
            )
        )
    }

    fun saveStructure(
        source: ServerCommandSource,
        from: BlockPos, to: BlockPos,
        name: Identifier,
        ignoreEntities: Boolean
    ) {
        val world = source.world
        if (world.isClient) return

        val pos = BlockPos(min(from.x, to.x), min(from.y, to.y), min(from.z, to.z))
        val size = BlockPos(
            abs(from.x - to.x) + 1,
            abs(from.y - to.y) + 1,
            abs(from.z - to.z) + 1
        )

        if (saveStructure(world, pos, size, name, ignoreEntities))
            source.sendFeedback({
                Text.translatable("structure_block.save_success", name)
            },true)
        else throw SAVE_FAILED_EXCEPTION.create(name)
    }

    fun saveFromStructureBlock(source: ServerCommandSource, pos: BlockPos) {
        val world = source.world
        if (world.isClient) return

        val blockOrEmpty = world.getBlockEntity(pos, BlockEntityType.STRUCTURE_BLOCK)
        if (blockOrEmpty.isEmpty || blockOrEmpty.get().mode != StructureBlockMode.SAVE)
            throw CommandException(Text.translatable("parsing.expected", STRUCTURE_BLOCK_SAVE))
        val block = blockOrEmpty.get()

        if (!block.hasStructureName())
            throw CommandException(Text.translatable("structure_block.invalid_structure_name", "")
        )

        if (block.size.x <= 0 || block.size.y <= 0 || block.size.z <= 0)
            throw SAVE_FAILED_EXCEPTION.create(block.templateName)

        if (block.saveStructure())
            source.sendFeedback(
                { Text.translatable("structure_block.save_success", block.templateName) },
                true
            )
        else throw SAVE_FAILED_EXCEPTION.create(block.templateName)
    }

    fun saveStructure(
        world: ServerWorld, pos: BlockPos, size: BlockPos,
        name: Identifier, ignoreEntities: Boolean
    ): Boolean {
        val structureManager = world.structureTemplateManager
        val structure = structureManager.getTemplateOrBlank(name)
        structure.saveFromWorld(world, pos, size, !ignoreEntities, Blocks.STRUCTURE_VOID)
        structureManager.saveTemplate(name)
        return structureManager.getTemplate(name).isPresent
    }

    fun removeStructureFromBlock(source: ServerCommandSource, pos: BlockPos) {
        val world = source.world
        if (world.isClient) return
        val blockOrEmpty = world.getBlockEntity(pos, BlockEntityType.STRUCTURE_BLOCK)
        if (blockOrEmpty.isEmpty)
            throw CommandException(Text.translatable("parsing.expected", STRUCTURE_BLOCK))
        val block = blockOrEmpty.get()

        val name = Identifier.tryParse(block.templateName)
            ?: throw CommandException(Text.translatable("structure_block.invalid_structure_name", ""))

        removeStructure(source, name)
    }

    fun removeStructure(source: ServerCommandSource, name: Identifier) {
        val world = source.world
        if (world.isClient) return
        val structureManager = world.structureTemplateManager
        if (!structureManager.getTemplate(name).isPresent)
            throw STRUCTURE_NOT_FOUND_EXCEPTION.create(name)

        var path = structureManager.getTemplatePath(name, ".nbt").toFile()
        var prevPath: File
        do {
            prevPath = path
            path.delete()
            path = path.parentFile
        } while (path.isEmpty(prevPath) && !(path.endsWith("generated") && path.isDirectory))

        structureManager.unloadTemplate(name)
        source.sendFeedback(
            { Text.literal("Structure ").append(name.toString()).append(" removed!") },
            true
        )
    }
}
