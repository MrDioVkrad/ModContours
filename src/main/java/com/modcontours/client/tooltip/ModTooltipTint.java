package com.modcontours.client.tooltip;

import com.modcontours.client.color.ModColorProvider;
import com.modcontours.config.ModContoursConfig;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.ModList;

public final class ModTooltipTint {
    private static final String VANILLA_MOD_ID = "minecraft";

    private ModTooltipTint() {
    }

    /* ------------------------------------------------------------------ */
    /*  Handler 1: ItemTooltipEvent (LOWEST priority)                     */
    /* ------------------------------------------------------------------ */

    public static void onTooltipColor(ItemTooltipEvent event) {
        if (!ModContoursConfig.TOOLTIP_TINT.get()) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        String modId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        if (VANILLA_MOD_ID.equals(modId)) return;

        String displayName = modDisplayName(modId);
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) return;

        // Pass 1 — style-first: find the Forge blue+italic line (most reliable)
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            Component line = tooltip.get(i);
            if (hasForgeModStyle(line)) {
                // Use the existing text from the line (in case it differs from our lookup)
                String text = line.getString();
                tooltip.set(i, tinted(text.isEmpty() ? displayName : text, modId));
                return;
            }
        }

        // Pass 2 — text match fallback
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            Component line = tooltip.get(i);
            if (matchesModLine(line.getString(), displayName, modId)) {
                tooltip.set(i, tinted(displayName, modId));
                return;
            }
        }

        // Pass 3 — italic-only fallback: catch mod names where the blue shade
        // doesn't match ChatFormatting.BLUE exactly (some mods restyle it)
        for (int i = tooltip.size() - 1; i >= 1; i--) {
            Component line = tooltip.get(i);
            if (hasItalicStyle(line)) {
                String text = line.getString();
                tooltip.set(i, tinted(text.isEmpty() ? displayName : text, modId));
                return;
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Handler 2: RenderTooltipEvent.GatherComponents (LOWEST priority)  */
    /* ------------------------------------------------------------------ */

    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        if (!ModContoursConfig.TOOLTIP_TINT.get()) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        String modId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        if (VANILLA_MOD_ID.equals(modId)) return;

        String displayName = modDisplayName(modId);
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();

        // Pass 1 — style-first
        for (int i = elements.size() - 1; i >= 0; i--) {
            Optional<FormattedText> left = elements.get(i).left();
            if (left.isEmpty()) continue;

            FormattedText text = left.get();
            if (text instanceof Component comp && hasForgeModStyle(comp)) {
                if (!isAlreadyTinted(comp, modId)) {
                    String lineText = comp.getString();
                    elements.set(i, Either.left(tinted(lineText.isEmpty() ? displayName : lineText, modId)));
                }
                return;
            }
        }

        // Pass 2 — text match fallback
        for (int i = elements.size() - 1; i >= 0; i--) {
            Optional<FormattedText> left = elements.get(i).left();
            if (left.isEmpty()) continue;

            FormattedText text = left.get();
            String plain = text.getString();
            if (matchesModLine(plain, displayName, modId)) {
                if (!(text instanceof Component comp && isAlreadyTinted(comp, modId))) {
                    elements.set(i, Either.left(tinted(displayName, modId)));
                }
                return;
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                           */
    /* ------------------------------------------------------------------ */

    private static String modDisplayName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getDisplayName())
                .orElse(modId);
    }

    private static boolean matchesModLine(String plainText, String displayName, String modId) {
        if (plainText == null || plainText.isEmpty()) return false;
        String t = plainText.trim();
        return t.equals(displayName)
                || t.equalsIgnoreCase(displayName)
                || t.equals(modId)
                || t.contains(displayName);
    }

    /** Recursively checks if a component (or any sibling) uses Forge's blue+italic style. */
    private static boolean hasForgeModStyle(Component component) {
        if (isBlueItalic(component.getStyle())) return true;
        for (Component sibling : component.getSiblings()) {
            if (hasForgeModStyle(sibling)) return true;
        }
        return false;
    }

    private static boolean isBlueItalic(Style style) {
        TextColor color = style.getColor();
        if (color == null) return false;
        TextColor blue = TextColor.fromLegacyFormat(ChatFormatting.BLUE);
        return blue != null && blue.equals(color) && Boolean.TRUE.equals(style.isItalic());
    }

    /** Recursively checks if a component (or any sibling) uses italic style. */
    private static boolean hasItalicStyle(Component component) {
        if (Boolean.TRUE.equals(component.getStyle().isItalic())) return true;
        for (Component sibling : component.getSiblings()) {
            if (hasItalicStyle(sibling)) return true;
        }
        return false;
    }

    /** Returns true if the component already carries our custom tint. */
    private static boolean isAlreadyTinted(Component comp, String modId) {
        TextColor color = comp.getStyle().getColor();
        if (color == null) return false;
        return color.getValue() == ModColorProvider.rgb(modId);
    }

    private static MutableComponent tinted(String displayName, String modId) {
        return Component.literal(displayName)
                .withStyle(s -> s
                        .withColor(TextColor.fromRgb(ModColorProvider.rgb(modId)))
                        .withItalic(false));
    }
}
