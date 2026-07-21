package com.soytrunix;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyRPC implements ModInitializer {
	public static final String MOD_ID = "easyrpc";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("EasyRPC: Server-side initialization");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}