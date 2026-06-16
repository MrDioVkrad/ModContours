package com.modcontours.client.color;

import com.modcontours.config.ModContoursConfig;
import java.awt.Color;
import java.util.Locale;
import java.util.OptionalInt;
import net.minecraftforge.fml.ModList;

public final class ModColorProvider {
    private static final float SATURATION = 0.66F;
    private static final float BRIGHTNESS = 0.95F;

    private ModColorProvider() {
    }

    public static int argb(String modId, int alpha) {
        int rgb = rgb(modId);
        return (clamp(alpha) << 24) | rgb;
    }

    public static int rgb(String modId) {
        OptionalInt override = overrideColor(modId);
        if (override.isPresent()) {
            return override.getAsInt();
        }

        // Murmur3-style bit mixing for much better hue distribution.
        // Plain String.hashCode() easily maps different mod IDs to the same hue
        // because collisions modulo 360 are common.
        int hash = modId.toLowerCase(Locale.ROOT).hashCode();
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);

        float hue = ((hash & 0x7fffffff) % 360) / 360.0F;
        return Color.HSBtoRGB(hue, SATURATION, BRIGHTNESS) & 0x00ffffff;
    }

    public static String displayName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    private static int clamp(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }

    private static OptionalInt overrideColor(String modId) {
        String prefix = modId.toLowerCase(Locale.ROOT) + "=";
        for (String entry : ModContoursConfig.COLOR_OVERRIDES.get()) {
            String normalized = entry.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith(prefix)) {
                continue;
            }

            String hex = entry.substring(entry.indexOf('=') + 1).replace("#", "");
            try {
                return OptionalInt.of(Integer.parseInt(hex, 16) & 0x00ffffff);
            } catch (NumberFormatException ignored) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.empty();
    }
}
