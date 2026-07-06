package dev.typicalfarmingmacro;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tfm implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("typicalfarmingmacro");

    @Override
    public void onInitialize() {
        LOGGER.info("Tfm v1 Initialized!");
    }
}
