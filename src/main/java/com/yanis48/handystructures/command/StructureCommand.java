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
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPos;

public class StructureCommand {
	public static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType((name) -> {
		return new TranslatableTextComponent("structure_block.load_not_found", new Object[]{name});
	});
	public static final DynamicCommandExceptionType SAVE_FAILED_EXCEPTION = new DynamicCommandExceptionType((name) -> {
		return new TranslatableTextComponent("structure_block.save_failure", new Object[]{name});
	});
	
	public static final SuggestionProvider<ServerCommandSource> ROTATION_SUGGESTIONS = (commandContext, suggestionsBuilder) -> {
		Collection<String> ROTATIONS = Arrays.asList("0", "90", "180", "270");
		return CommandSource.suggestMatching(ROTATIONS, suggestionsBuilder);
	};
	
	public static final SuggestionProvider<ServerCommandSource> MIRROR_SUGGESTIONS = (commandContext, suggestionsBuilder) -> {
		Collection<String> MIRRORS = Arrays.asList("front_back", "left_right", "none");
		return CommandSource.suggestMatching(MIRRORS, suggestionsBuilder);
	};
	
	public static void register(CommandDispatcher<ServerCommandSource> commandDispatcher_1) {
		commandDispatcher_1.register((ServerCommandManager.literal("structure").requires((serverCommandSource_1) -> {
			return serverCommandSource_1.hasPermissionLevel(2);
		})).then(ServerCommandManager.literal("load").then(ServerCommandManager.argument("name", IdentifierArgumentType.create()).then(ServerCommandManager.argument("pos", BlockPosArgumentType.create()).executes((context) -> {
			return loadStructure(context.getSource(), IdentifierArgumentType.getIdentifierArgument(context, "name"), BlockPosArgumentType.getPosArgument(context, "pos"), 0, "none", true);
		}).then(ServerCommandManager.argument("rotation", IntegerArgumentType.integer()).suggests(ROTATION_SUGGESTIONS).executes((context) -> {
			return loadStructure(context.getSource(), IdentifierArgumentType.getIdentifierArgument(context, "name"), BlockPosArgumentType.getPosArgument(context, "pos"), IntegerArgumentType.getInteger(context, "rotation"), "none", true);
		}).then(ServerCommandManager.argument("mirror", StringArgumentType.string()).suggests(MIRROR_SUGGESTIONS).executes((context) -> {
			return loadStructure(context.getSource(), IdentifierArgumentType.getIdentifierArgument(context, "name"), BlockPosArgumentType.getPosArgument(context, "pos"), IntegerArgumentType.getInteger(context, "rotation"), StringArgumentType.getString(context, "mirror"), true);
		}).then(ServerCommandManager.argument("ignoreEntities", BoolArgumentType.bool()).executes((context) -> {
			return loadStructure(context.getSource(), IdentifierArgumentType.getIdentifierArgument(context, "name"), BlockPosArgumentType.getPosArgument(context, "pos"), IntegerArgumentType.getInteger(context, "rotation"), StringArgumentType.getString(context, "mirror"), BoolArgumentType.getBool(context, "ignoreEntities"));
		}))))))).then(ServerCommandManager.literal("save").then(ServerCommandManager.argument("from", BlockPosArgumentType.create()).then(ServerCommandManager.argument("to", BlockPosArgumentType.create()).then(ServerCommandManager.argument("name", IdentifierArgumentType.create()).executes((context) -> {
			return saveStructure(context.getSource(), BlockPosArgumentType.getPosArgument(context, "from"), BlockPosArgumentType.getPosArgument(context, "to"), IdentifierArgumentType.getIdentifierArgument(context, "name"), true);
		}).then(ServerCommandManager.argument("ignoreEntities", BoolArgumentType.bool()).executes((context) -> {
			return saveStructure(context.getSource(), BlockPosArgumentType.getPosArgument(context, "from"), BlockPosArgumentType.getPosArgument(context, "to"), IdentifierArgumentType.getIdentifierArgument(context, "name"), BoolArgumentType.getBool(context, "ignoreEntities"));
		})))))));
	}

	public static int loadStructure(ServerCommandSource source, Identifier name, BlockPos pos, int rot, String mir, boolean ignoreEntities) throws CommandSyntaxException {
		int int_1 = 0;
		World world = source.getWorld();
		BlockPos offset = new BlockPos(0, 1, 0);
		Rotation rotation;
		switch (rot) {
			default:
			case 0:
				rotation = Rotation.ROT_0;
				break;
			case 90:
				rotation = Rotation.ROT_90;
				break;
			case 180:
				rotation = Rotation.ROT_180;
				break;
			case 270:
				rotation = Rotation.ROT_270;
				break;
		}
		Mirror mirror;
		switch (mir) {
			default:
			case "none":
				mirror = Mirror.NONE;
				break;
			case "left_right":
				mirror = Mirror.LEFT_RIGHT;
				break;
			case "front_back":
				mirror = Mirror.FRONT_BACK;
				break;
		}
		
		if (!world.isClient && name != null) {
			BlockPos pos_1 = pos;
			BlockPos pos_2 = pos_1.add(offset);
			ServerWorld serverWorld = (ServerWorld)world;
			StructureManager structureManager = serverWorld.getStructureManager();
			Structure structure = structureManager.getStructure(name);
			StructurePlacementData placementData = (new StructurePlacementData()).setMirrored(mirror).setRotation(rotation).setIgnoreEntities(ignoreEntities).setChunkPosition((ChunkPos)null);
			
			if (structureManager.getStructure(name) != null) {
				structure.place(world, pos_2, placementData);
				source.sendFeedback(new TranslatableTextComponent("structure_block.load_success", new Object[]{name}), true);
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
				source.sendFeedback(new TranslatableTextComponent("structure_block.save_success", new Object[]{name}), true);
			} else {
				throw SAVE_FAILED_EXCEPTION.create(name);
			}
		}
		return int_1;
	}
}
