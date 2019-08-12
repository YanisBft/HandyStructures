package com.yanis48.handystructures.command;

import java.util.Arrays;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.block.Blocks;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class StructureCommand {
	public static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType((name) -> {
		return new TranslatableText("structure_block.load_not_found", new Object[]{name});
	});
	public static final DynamicCommandExceptionType SAVE_FAILED_EXCEPTION = new DynamicCommandExceptionType((name) -> {
		return new TranslatableText("structure_block.save_failure", new Object[]{name});
	});
	
	public static final SuggestionProvider<ServerCommandSource> ROTATION_SUGGESTIONS = (commandContext, suggestionsBuilder) -> {
		Collection<String> ROTATIONS = Arrays.asList("0", "90", "180", "270");
		return CommandSource.suggestMatching(ROTATIONS, suggestionsBuilder);
	};
	
	public static final SuggestionProvider<ServerCommandSource> MIRROR_SUGGESTIONS = (commandContext, suggestionsBuilder) -> {
		Collection<String> MIRRORS = Arrays.asList("front_back", "left_right", "none");
		return CommandSource.suggestMatching(MIRRORS, suggestionsBuilder);
	};
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register((CommandManager.literal("structure").requires((commandSource) -> {
			return commandSource.hasPermissionLevel(2);
		})).then(CommandManager.literal("load").then(CommandManager.argument("name", IdentifierArgumentType.identifier()).then(CommandManager.argument("pos", BlockPosArgumentType.blockPos()).executes((context) -> {
			return loadStructure(context.getSource(), IdentifierArgumentType.getIdentifier(context, "name"), BlockPosArgumentType.getBlockPos(context, "pos"), 0, "none", true);
		}).then(CommandManager.argument("rotation", IntegerArgumentType.integer()).suggests(ROTATION_SUGGESTIONS).executes((context) -> {
			return loadStructure(context.getSource(), IdentifierArgumentType.getIdentifier(context, "name"), BlockPosArgumentType.getBlockPos(context, "pos"), IntegerArgumentType.getInteger(context, "rotation"), "none", true);
		}).then(CommandManager.argument("mirror", StringArgumentType.string()).suggests(MIRROR_SUGGESTIONS).executes((context) -> {
			return loadStructure(context.getSource(), IdentifierArgumentType.getIdentifier(context, "name"), BlockPosArgumentType.getBlockPos(context, "pos"), IntegerArgumentType.getInteger(context, "rotation"), StringArgumentType.getString(context, "mirror"), true);
		}).then(CommandManager.argument("ignoreEntities", BoolArgumentType.bool()).executes((context) -> {
			return loadStructure(context.getSource(), IdentifierArgumentType.getIdentifier(context, "name"), BlockPosArgumentType.getBlockPos(context, "pos"), IntegerArgumentType.getInteger(context, "rotation"), StringArgumentType.getString(context, "mirror"), BoolArgumentType.getBool(context, "ignoreEntities"));
		}))))))).then(CommandManager.literal("save").then(CommandManager.argument("from", BlockPosArgumentType.blockPos()).then(CommandManager.argument("to", BlockPosArgumentType.blockPos()).then(CommandManager.argument("name", IdentifierArgumentType.identifier()).executes((context) -> {
			return saveStructure(context.getSource(), BlockPosArgumentType.getBlockPos(context, "from"), BlockPosArgumentType.getBlockPos(context, "to"), IdentifierArgumentType.getIdentifier(context, "name"), true);
		}).then(CommandManager.argument("ignoreEntities", BoolArgumentType.bool()).executes((context) -> {
			return saveStructure(context.getSource(), BlockPosArgumentType.getBlockPos(context, "from"), BlockPosArgumentType.getBlockPos(context, "to"), IdentifierArgumentType.getIdentifier(context, "name"), BoolArgumentType.getBool(context, "ignoreEntities"));
		})))))));
	}

	public static int loadStructure(ServerCommandSource source, Identifier name, BlockPos pos, int rot, String mir, boolean ignoreEntities) throws CommandSyntaxException {
		int int_1 = 0;
		World world = source.getWorld();
		BlockPos offset = new BlockPos(0, 1, 0);
		BlockRotation rotation;
		switch (rot) {
			default:
			case 0:
				rotation = BlockRotation.NONE;
				break;
			case 90:
				rotation = BlockRotation.CLOCKWISE_90;
				break;
			case 180:
				rotation = BlockRotation.CLOCKWISE_180;
				break;
			case 270:
				rotation = BlockRotation.COUNTERCLOCKWISE_90;
				break;
		}
		BlockMirror mirror;
		switch (mir) {
			default:
			case "none":
				mirror = BlockMirror.NONE;
				break;
			case "left_right":
				mirror = BlockMirror.LEFT_RIGHT;
				break;
			case "front_back":
				mirror = BlockMirror.FRONT_BACK;
				break;
		}
		
		if (!world.isClient && name != null) {
			BlockPos pos_1 = pos;
			BlockPos pos_2 = pos_1.add(offset);
			ServerWorld serverWorld = (ServerWorld) world;
			StructureManager structureManager = serverWorld.getStructureManager();
			Structure structure = structureManager.getStructure(name);
			StructurePlacementData placementData = new StructurePlacementData().setMirrored(mirror).setRotation(rotation).setIgnoreEntities(ignoreEntities).setChunkPosition((ChunkPos) null);
			
			if (structureManager.getStructure(name) != null) {
				structure.place(world, pos_2, placementData);
				source.sendFeedback(new TranslatableText("structure_block.load_success", new Object[]{name}), true);
			} else {
				throw STRUCTURE_NOT_FOUND_EXCEPTION.create(name);
			}
		}
		return int_1;
	}

	public static int saveStructure(ServerCommandSource source, BlockPos from, BlockPos to, Identifier name, boolean ignoreEntities) throws CommandSyntaxException {
		int int_1 = 0;		
		World world = source.getWorld();
		int x1 = from.getX() < to.getX() ? from.getX() : to.getX();
		int y1 = from.getY() < to.getY() ? from.getY() : to.getY();
		int z1 = from.getZ() < to.getZ() ? from.getZ() : to.getZ();
		BlockPos pos = new BlockPos(x1, y1, z1);
		int x2 = from.getX() < to.getX() ? to.getX() - from.getX() : from.getX() - to.getX();
		int y2 = from.getY() < to.getY() ? to.getY() - from.getY() : from.getY() - to.getY();
		int z2 = from.getZ() < to.getZ() ? to.getZ() - from.getZ() : from.getZ() - to.getZ();
		BlockPos size = new BlockPos(x2, y2, z2);
		
		if (!world.isClient && name != null) {
			ServerWorld serverWorld = (ServerWorld)world;
			StructureManager structureManager = serverWorld.getStructureManager();
			Structure structure = structureManager.getStructureOrBlank(name);

			if (structureManager.getStructure(name) != null) {
				structure.method_15174(world, pos, size.add(1, 1, 1), !ignoreEntities, Blocks.STRUCTURE_VOID);
				structureManager.saveStructure(name);
				source.sendFeedback(new TranslatableText("structure_block.save_success", new Object[]{name}), true);
			} else {
				throw SAVE_FAILED_EXCEPTION.create(name);
			}
		}
		return int_1;
	}
}
