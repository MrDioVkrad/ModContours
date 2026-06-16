package com.modcontours.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ModContoursConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue LINE_THICKNESS;
    public static final ForgeConfigSpec.DoubleValue FILL_OPACITY;
    public static final ForgeConfigSpec.EnumValue<LineStyle> LINE_STYLE;
    public static final ForgeConfigSpec.BooleanValue TOOLTIP_TINT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COLOR_OVERRIDES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("visuals");

        LINE_THICKNESS = builder
                .comment("Contour line thickness in pixels.")
                .defineInRange("lineThickness", 2, 1, 6);
        FILL_OPACITY = builder
                .comment("Cluster background opacity from 0.0 to 0.8.")
                .defineInRange("fillOpacity", 0.18D, 0.0D, 0.8D);
        LINE_STYLE = builder
                .comment("Visual style used for contour lines.")
                .defineEnum("lineStyle", LineStyle.SOLID);
        TOOLTIP_TINT = builder
                .comment("Tint the mod-name line in item tooltips with the assigned contour color.")
                .define("tooltipTint", true);
        COLOR_OVERRIDES = builder
                .comment("Manual mod colors in the format modid=#RRGGBB, for example minecraft=#55ff55.")
                .defineListAllowEmpty("colorOverrides", List.of(), ModContoursConfig::isColorOverride);

        builder.pop();
        SPEC = builder.build();
    }

    private ModContoursConfig() {
    }

    private static boolean isColorOverride(Object value) {
        return value instanceof String text && text.matches("[a-z0-9_.-]+=#?[0-9a-fA-F]{6}");
    }

    public enum LineStyle {
        SOLID,
        DASHED,
        NEON
    }
}
