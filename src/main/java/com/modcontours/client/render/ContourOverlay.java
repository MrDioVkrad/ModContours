package com.modcontours.client.render;

import com.modcontours.client.color.ModColorProvider;
import com.modcontours.client.model.Cluster;
import com.modcontours.client.model.LineSegment;
import com.modcontours.client.model.SlotCell;
import com.modcontours.config.ModContoursConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;

public final class ContourOverlay {
    private static final String VANILLA_MOD_ID = "minecraft";
    private static final int LABEL_SPACING = 15;

    /* ---------- tooltip-position capture (set by RenderTooltipEvent.Pre) ---------- */
    private static int capturedTooltipBottom = -1;
    private static boolean tooltipCapturedThisFrame = false;

    private ContourOverlay() {
    }

    /* ================================================================== */
    /*  Event handlers                                                    */
    /* ================================================================== */

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            render(screen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }

    /**
     * Captures the real tooltip bounding box so we can place our label
     * tag below it — works for custom / image-containing tooltips too.
     */
    public static void onTooltipPre(RenderTooltipEvent.Pre event) {
        int contentHeight = event.getComponents().size() == 1 ? -2 : 0;
        for (var comp : event.getComponents()) {
            contentHeight += comp.getHeight();
        }
        // MC draws the tooltip frame as: (x-3, y-4) to (x+w+3, y+h+3)
        capturedTooltipBottom = event.getY() + contentHeight + 3;
        tooltipCapturedThisFrame = true;
    }

    /* ================================================================== */
    /*  Main render pipeline                                              */
    /* ================================================================== */

    private static void render(AbstractContainerScreen<?> screen, GuiGraphics graphics,
                               int mouseX, int mouseY) {
        // Consume the tooltip position captured earlier this frame
        int tooltipBottom = tooltipCapturedThisFrame ? capturedTooltipBottom : -1;
        tooltipCapturedThisFrame = false;

        List<SlotCell> cells = collectCells(screen);
        if (cells.isEmpty()) return;

        int thickness = ModContoursConfig.LINE_THICKNESS.get();
        List<RenderedCluster> all = new ArrayList<>();
        List<RenderedCluster> hovered = new ArrayList<>();

        for (Cluster cluster : ClusterBuilder.build(cells)) {
            List<LineSegment> outline = ContourGeometry.outline(cluster, thickness);
            RenderedCluster rc = new RenderedCluster(cluster, outline);
            all.add(rc);

            if (ContourGeometry.containsMouse(outline, mouseX, mouseY, thickness, 1)) {
                hovered.add(rc);
            }
        }

        // Pass 1 — background fills
        for (RenderedCluster rc : all) {
            drawFill(graphics, rc.cluster());
        }

        // Pass 2 — non-hovered borders
        for (RenderedCluster rc : all) {
            if (!hovered.contains(rc)) {
                drawClusterBorder(graphics, rc.outline(), rc.cluster().modId(), false);
            }
        }

        // Pass 3 — hovered borders (raised z, pulsing alpha)
        if (!hovered.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 10);
            for (RenderedCluster rc : hovered) {
                drawClusterBorder(graphics, rc.outline(), rc.cluster().modId(), true);
            }
            graphics.pose().popPose();
        }

        // Pass 4 — labels
        if (!hovered.isEmpty()) {
            drawLabels(graphics, hovered, mouseX, mouseY,
                    screen.width, screen.height, tooltipBottom);
        }
    }

    /* ================================================================== */
    /*  Cell collection                                                   */
    /* ================================================================== */

    private static List<SlotCell> collectCells(AbstractContainerScreen<?> screen) {
        List<SlotCell> cells = new ArrayList<>();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !slot.isActive() || !isVisibleSlot(screen, slot)) continue;

            String modId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
            if (VANILLA_MOD_ID.equals(modId)) continue;

            int gridX = Math.round(slot.x / (float) ContourGeometry.SLOT_STEP);
            int gridY = Math.round(slot.y / (float) ContourGeometry.SLOT_STEP);
            cells.add(new SlotCell(gridX, gridY, left + slot.x, top + slot.y, modId));
        }
        return cells;
    }

    private static boolean isVisibleSlot(AbstractContainerScreen<?> screen, Slot slot) {
        int x = screen.getGuiLeft() + slot.x;
        int y = screen.getGuiTop() + slot.y;
        return x >= screen.getGuiLeft()
                && y >= screen.getGuiTop()
                && x + ContourGeometry.SLOT_SIZE <= screen.getGuiLeft() + screen.getXSize()
                && y + ContourGeometry.SLOT_SIZE <= screen.getGuiTop() + screen.getYSize();
    }

    /* ================================================================== */
    /*  Drawing — fills                                                   */
    /* ================================================================== */

    private static void drawFill(GuiGraphics graphics, Cluster cluster) {
        int alpha = (int) Math.round(ModContoursConfig.FILL_OPACITY.get() * 255.0D);
        int color = ModColorProvider.argb(cluster.modId(), alpha);
        for (SlotCell cell : cluster.cells()) {
            graphics.fill(cell.x() - 1, cell.y() - 1,
                    cell.x() + ContourGeometry.SLOT_SIZE + 1,
                    cell.y() + ContourGeometry.SLOT_SIZE + 1,
                    color);
        }
    }

    /* ================================================================== */
    /*  Drawing — border                                                  */
    /* ================================================================== */

    private static void drawClusterBorder(GuiGraphics graphics, List<LineSegment> outline,
                                          String modId, boolean hovered) {
        int thickness = ModContoursConfig.LINE_THICKNESS.get();

        // Hover: pulsing alpha via sine wave, no thickness change
        float pulse = hovered
                ? (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.005))
                : 0;
        int alpha = hovered ? (int) (170 + 85 * pulse) : 220;
        int color = ModColorProvider.argb(modId, alpha);

        boolean dashed = ModContoursConfig.LINE_STYLE.get() == ModContoursConfig.LineStyle.DASHED;
        boolean neon   = ModContoursConfig.LINE_STYLE.get() == ModContoursConfig.LineStyle.NEON;

        // Neon glow — centred, also pulses when hovered
        if (neon) {
            int glowAlpha = hovered ? (int) (48 + 48 * pulse) : 64;
            int glow = ModColorProvider.argb(modId, glowAlpha);
            drawSegmentsCentred(graphics, outline, thickness + 2, glow, false);
        }

        // Main border — drawn inward (strictly inside cells)
        if (dashed) {
            drawSegmentsInward(graphics, outline, thickness, color, true);
        } else {
            drawSegmentsInward(graphics, outline, thickness, color, false);
        }
    }

    /* ================================================================== */
    /*  Drawing — inward-only segments                                    */
    /* ================================================================== */

    private static void drawSegmentsInward(GuiGraphics graphics, List<LineSegment> outline,
                                           int thickness, int color, boolean dashed) {
        for (LineSegment seg : outline) {
            if (dashed) {
                drawDashedInward(graphics, seg, thickness, color);
            } else {
                drawInward(graphics, seg, thickness, color);
            }
        }
    }

    /**
     * Draws a segment with thickness extending <b>inward</b> (toward the
     * interior of the cell), so two adjacent clusters never share pixels.
     */
    private static void drawInward(GuiGraphics graphics, LineSegment seg,
                                   int thickness, int color) {
        switch (seg.side()) {
            case TOP -> // inward = downward
                    graphics.fill(seg.x1(), seg.y1(), seg.x2(), seg.y1() + thickness, color);
            case BOTTOM -> // inward = upward
                    graphics.fill(seg.x1(), seg.y1() - thickness, seg.x2(), seg.y1(), color);
            case LEFT -> // inward = rightward
                    graphics.fill(seg.x1(), seg.y1(), seg.x1() + thickness, seg.y2(), color);
            case RIGHT -> // inward = leftward
                    graphics.fill(seg.x1() - thickness, seg.y1(), seg.x1(), seg.y2(), color);
        }
    }

    private static void drawDashedInward(GuiGraphics graphics, LineSegment seg,
                                         int thickness, int color) {
        int dash = 7, gap = 4;
        if (seg.isHorizontal()) {
            int start = Math.min(seg.x1(), seg.x2());
            int end   = Math.max(seg.x1(), seg.x2());
            for (int x = start; x < end; x += dash + gap) {
                drawInward(graphics,
                        new LineSegment(x, seg.y1(), Math.min(x + dash, end),
                                seg.y2(), seg.side()),
                        thickness, color);
            }
        } else {
            int start = Math.min(seg.y1(), seg.y2());
            int end   = Math.max(seg.y1(), seg.y2());
            for (int y = start; y < end; y += dash + gap) {
                drawInward(graphics,
                        new LineSegment(seg.x1(), y, seg.x2(),
                                Math.min(y + dash, end), seg.side()),
                        thickness, color);
            }
        }
    }

    /* ================================================================== */
    /*  Drawing — centred segments (used for neon glow only)              */
    /* ================================================================== */

    private static void drawSegmentsCentred(GuiGraphics graphics, List<LineSegment> outline,
                                            int thickness, int color, boolean dashed) {
        for (LineSegment seg : outline) {
            drawCentred(graphics, seg, thickness, color);
        }
    }

    private static void drawCentred(GuiGraphics graphics, LineSegment seg,
                                    int thickness, int color) {
        int half = thickness / 2;
        int otherHalf = thickness - half;
        if (seg.isHorizontal()) {
            graphics.fill(seg.x1(), seg.y1() - half, seg.x2(), seg.y1() + otherHalf, color);
        } else {
            graphics.fill(seg.x1() - half, seg.y1(), seg.x1() + otherHalf, seg.y2(), color);
        }
    }

    /* ================================================================== */
    /*  Labels                                                            */
    /* ================================================================== */

    private static void drawLabels(GuiGraphics graphics, List<RenderedCluster> hovered,
                                   int mouseX, int mouseY,
                                   int screenWidth, int screenHeight,
                                   int tooltipBottom) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 800);

        int y;
        if (tooltipBottom > 0) {
            // Place snugly below the real rendered tooltip (−6 = visually tight)
            y = tooltipBottom - 6;
        } else {
            // No tooltip — right at cursor level
            y = mouseY;
        }

        for (RenderedCluster rc : hovered) {
            y = drawLabel(graphics, rc.cluster(), mouseX, y, screenWidth, screenHeight);
        }

        graphics.pose().popPose();
    }

    private static int drawLabel(GuiGraphics graphics, Cluster cluster,
                                 int mouseX, int requestedY,
                                 int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        String label = ModColorProvider.displayName(cluster.modId());
        int textWidth = minecraft.font.width(label);
        int x = Math.min(mouseX + 12, screenWidth - textWidth - 8);
        int y = Math.min(requestedY, screenHeight - 14);
        int color = ModColorProvider.argb(cluster.modId(), 255);

        graphics.fill(x - 4, y - 3, x + textWidth + 4, y + 11, 0xcc101010);
        graphics.fill(x - 4, y - 3, x - 2, y + 11, color);
        graphics.drawString(minecraft.font, label, x, y, 0xffffffff, false);
        return y + LABEL_SPACING;
    }

    /* ================================================================== */
    /*  Internal data                                                     */
    /* ================================================================== */

    private record RenderedCluster(Cluster cluster, List<LineSegment> outline) {
    }
}
