package com.yanisbft.handystructures.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType.*
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.enums.StructureBlockMode
import net.minecraft.command.CommandException
import net.minecraft.command.argument.BlockPosArgumentType.*
import net.minecraft.command.argument.IdentifierArgumentType.*
import net.minecraft.server.command.CommandManager.*
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import kotlin.math.abs
import kotlin.math.min


object StructureCommand {
    private const val STRUCTURE_BLOCK_SAVE = "minecraft:structure_block[mode=save]"
    private val SAVE_FAILED_EXCEPTION = DynamicCommandExceptionType { name ->
        Text.translatable("structure_block.save_failure", name)
    }

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(literal("structure")
            .requires { commandSource -> commandSource.hasPermissionLevel(2) }
            .then(
                literal("block").then(
                    argument("pos", blockPos()).executes { context ->
                        saveFromStructureBlock(context.source, getBlockPos(context, "pos")); 0
                    }
                )
            ).then(
                argument("from", blockPos()).then(
                    argument("to", blockPos()).then(
                        argument("name", identifier()).executes { context ->
                            saveStructure(
                                context.source,
                                getBlockPos(context, "from"),
                                getBlockPos(context, "to"),
                                getIdentifier(context, "name"),
                                true
                            ); 0
                        }.then(
                            argument("ignoreEntities", bool()).executes { context ->
                                saveStructure(
                                    context.source,
                                    getBlockPos(context, "from"),
                                    getBlockPos(context, "to"),
                                    getIdentifier(context, "name"),
                                    getBool(context, "ignoreEntities")
                                ); 0
                            }
                        )
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
}
