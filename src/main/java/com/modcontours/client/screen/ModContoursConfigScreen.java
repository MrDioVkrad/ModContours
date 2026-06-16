package com.modcontours.client.screen;

import com.modcontours.config.ModContoursConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModContoursConfigScreen extends Screen {
    private final Screen parent;

    public ModContoursConfigScreen(Screen parent) {
        super(Component.literal("ModContours"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 155;
        int right = width / 2 + 5;
        int y = height / 2 - 48;

        addRenderableWidget(Button.builder(thicknessMessage(), button -> {
            int next = ModContoursConfig.LINE_THICKNESS.get() >= 6 ? 1 : ModContoursConfig.LINE_THICKNESS.get() + 1;
            ModContoursConfig.LINE_THICKNESS.set(next);
            button.setMessage(thicknessMessage());
        }).bounds(left, y, 150, 20).build());

        addRenderableWidget(new FillOpacitySlider(right, y, 150, 20));

        addRenderableWidget(Button.builder(styleMessage(), button -> {
            ModContoursConfig.LineStyle[] values = ModContoursConfig.LineStyle.values();
            int next = (ModContoursConfig.LINE_STYLE.get().ordinal() + 1) % values.length;
            ModContoursConfig.LINE_STYLE.set(values[next]);
            button.setMessage(styleMessage());
        }).bounds(left, y + 26, 150, 20).build());

        addRenderableWidget(Button.builder(tooltipTintMessage(), button -> {
            ModContoursConfig.TOOLTIP_TINT.set(!ModContoursConfig.TOOLTIP_TINT.get());
            button.setMessage(tooltipTintMessage());
        }).bounds(right, y + 26, 150, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            ModContoursConfig.SPEC.save();
            minecraft.setScreen(parent);
        }).bounds(width / 2 - 100, height - 27, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xffffffff);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ModContoursConfig.SPEC.save();
        minecraft.setScreen(parent);
    }

    private static Component thicknessMessage() {
        return Component.literal(Component.translatable("modcontours.config.line_thickness").getString()
                + ": " + ModContoursConfig.LINE_THICKNESS.get() + " px");
    }

    private static Component styleMessage() {
        return Component.literal(Component.translatable("modcontours.config.line_style").getString()
                + ": " + ModContoursConfig.LINE_STYLE.get().name().toLowerCase());
    }

    private static Component tooltipTintMessage() {
        return Component.literal(Component.translatable("modcontours.config.tooltip_tint").getString()
                + ": " + (ModContoursConfig.TOOLTIP_TINT.get() ? "on" : "off"));
    }

    private static Component fillOpacityMessage(double value) {
        return Component.literal(Component.translatable("modcontours.config.fill_opacity").getString()
                + ": " + Math.round(value * 80.0D) + "%");
    }

    private static final class FillOpacitySlider extends AbstractSliderButton {
        private FillOpacitySlider(int x, int y, int width, int height) {
            super(x, y, width, height, fillOpacityMessage(ModContoursConfig.FILL_OPACITY.get() / 0.8D),
                    ModContoursConfig.FILL_OPACITY.get() / 0.8D);
        }

        @Override
        protected void updateMessage() {
            setMessage(fillOpacityMessage(value));
        }

        @Override
        protected void applyValue() {
            ModContoursConfig.FILL_OPACITY.set(value * 0.8D);
        }
    }
}
