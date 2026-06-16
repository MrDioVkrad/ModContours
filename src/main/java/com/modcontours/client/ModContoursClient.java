package com.modcontours.client;

import com.modcontours.ModContours;
import com.modcontours.client.render.ContourOverlay;
import com.modcontours.client.screen.ModContoursConfigScreen;
import com.modcontours.client.tooltip.ModTooltipTint;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;

public final class ModContoursClient {
    private ModContoursClient() {
    }

    public static void init() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ModContoursConfigScreen(parent))
        );
        MinecraftForge.EVENT_BUS.addListener(ContourOverlay::onScreenRender);
        // Capture real tooltip dimensions so the mod-name tag avoids custom tooltips
        MinecraftForge.EVENT_BUS.addListener(ContourOverlay::onTooltipPre);
        // LOWEST priority: Forge adds the mod-name line before firing the event,
        // but other mods may also modify the list; running last guarantees we see it.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, ModTooltipTint::onTooltipColor);
        // Fallback: also hook into the render-phase component gathering.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, ModTooltipTint::onGatherComponents);
        ModContours.LOGGER.debug("ModContours client hooks registered");
    }
}
