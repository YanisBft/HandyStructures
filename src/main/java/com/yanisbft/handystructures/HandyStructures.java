package com.yanisbft.handystructures;

import com.yanisbft.handystructures.command.StructureCommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class HandyStructures implements ModInitializer {
	
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, dedicated) -> {
			StructureCommand.register(dispatcher);
		});
	}
}
