package com.modcontours;

import com.modcontours.client.ModContoursClient;
import com.modcontours.config.ModContoursConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ModContours.MOD_ID)
public final class ModContours {
    public static final String MOD_ID = "modcontours";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ModContours() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModContoursConfig.SPEC);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ModContoursClient::init);
        MinecraftForge.EVENT_BUS.register(this);
    }
}
