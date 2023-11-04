package com.yanisbft.handystructures.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;
import static net.minecraft.command.argument.IdentifierArgumentType.getIdentifier;
import static net.minecraft.command.argument.IdentifierArgumentType.identifier;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class StructureCommand {
    public static final DynamicCommandExceptionType SAVE_FAILED_EXCEPTION = new DynamicCommandExceptionType(
        (name) -> Text.translatable("structure_block.save_failure", name)
    );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((literal("structure").requires(
            (commandSource) -> commandSource.hasPermissionLevel(2))
        ).then(
            literal("block").then(
                argument("pos", blockPos())
                .executes((context) -> saveFromStructureBlock(
                    context.getSource(),
                    getBlockPos(context, "pos")
                ))
            )
        ).then(
            argument("from", blockPos())
            .then(
                argument("to", blockPos())
                .then(
                    argument("name", identifier())
                    .executes((context) -> saveStructure(
                        context.getSource(),
                        getBlockPos(context, "from"),
                        getBlockPos(context, "to"),
                        getIdentifier(context, "name"),
                        true
                    )).then(
                        argument("ignoreEntities", bool())
                        .executes((context) -> saveStructure(
                            context.getSource(),
                            getBlockPos(context, "from"),
                            getBlockPos(context, "to"),
                            getIdentifier(context, "name"),
                            getBool(context, "ignoreEntities")
                        ))
                    )
                )
            )
        ));
    }

    public static int saveStructure(ServerCommandSource source, BlockPos from, BlockPos to, Identifier name, boolean ignoreEntities) throws CommandSyntaxException {
        ServerWorld world = source.getWorld();
        if (world.isClient)
            return 0;

        BlockPos pos = new BlockPos(
            Math.min(from.getX(), to.getX()),
            Math.min(from.getY(), to.getY()),
            Math.min(from.getZ(), to.getZ())
        );

        BlockPos size = new BlockPos(
            Math.abs(from.getX() - to.getX()) + 1,
            Math.abs(from.getY() - to.getY()) + 1,
            Math.abs(from.getZ() - to.getZ()) + 1
        );

        if(_saveStructure(world, pos, size, name, ignoreEntities))
            source.sendFeedback(() -> Text.translatable("structure_block.save_success", name), true);
        else
            throw SAVE_FAILED_EXCEPTION.create(name);

        return 0;
    }

    public static int saveFromStructureBlock(ServerCommandSource source, BlockPos pos) throws CommandException, CommandSyntaxException {
        ServerWorld world = source.getWorld();
        if (world.isClient)
            return 0;

        var blockOrEmpty = world.getBlockEntity(pos, BlockEntityType.STRUCTURE_BLOCK);
        if (blockOrEmpty.isEmpty() || blockOrEmpty.get().getMode() != StructureBlockMode.SAVE)
            throw new CommandException(Text.translatable("parsing.expected", "minecraft:structure_block[mode=save]"));
        var block = blockOrEmpty.get();

        if (!block.hasStructureName())
            throw new CommandException(Text.translatable("structure_block.invalid_structure_name", ""));

        if (block.getSize().getX() <= 0 || block.getSize().getY() <= 0 || block.getSize().getZ() <= 0)
            throw SAVE_FAILED_EXCEPTION.create(block.getTemplateName());

        if (block.saveStructure())
            source.sendFeedback(() -> Text.translatable("structure_block.save_success", block.getTemplateName()), true);
        else
            throw SAVE_FAILED_EXCEPTION.create(block.getTemplateName());

        return 0;
    }

    private static boolean _saveStructure(
        ServerWorld world, BlockPos pos, BlockPos size,
        Identifier name, boolean ignoreEntities
    ) {
        if (name == null)
            return false;

        StructureTemplateManager structureManager = world.getStructureTemplateManager();
        var structure = structureManager.getTemplateOrBlank(name);
        structure.saveFromWorld(world, pos, size, !ignoreEntities, Blocks.STRUCTURE_VOID);
        structureManager.saveTemplate(name);
        return structureManager.getTemplate(name).isPresent();
    }
}
