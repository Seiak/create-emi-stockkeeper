package com.createemicompat;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("createemicompat")
public class CreateEmiCompat {
    public static final String MOD_ID = "createemicompat";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public CreateEmiCompat() {
        LOGGER.info("Create-EMI Stockkeeper Compat loaded");
    }
}
