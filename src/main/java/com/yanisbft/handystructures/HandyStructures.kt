package com.yanisbft.handystructures

import com.yanisbft.handystructures.command.StructureCommand
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

class HandyStructures : ModInitializer {
    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            StructureCommand.register(dispatcher)
        }
    }
}
