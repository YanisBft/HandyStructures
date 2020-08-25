package com.yanis48.handystructures;

import com.yanis48.handystructures.command.StructureCommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class HandyStructures implements ModInitializer {
	
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			StructureCommand.register(dispatcher);
		});
	}
}
