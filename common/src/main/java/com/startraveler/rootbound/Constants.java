package com.startraveler.rootbound;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

	public static final String MOD_ID = "rootbound";
	public static final String MOD_NAME = "Rootbound";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

	public static Identifier location(String path) {
		return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
	}
}