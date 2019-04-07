package com.yanis48.handystructures;

import com.yanis48.handystructures.command.StructureCommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;

public class HandyStructures implements ModInitializer {
	
	@Override
	public void onInitialize() {
		CommandRegistry.INSTANCE.register(false, (StructureCommand::register));
	}
}
