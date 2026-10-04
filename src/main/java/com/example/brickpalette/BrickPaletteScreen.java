package com.example.brickpalette;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public class BrickPaletteScreen extends Screen {

    public BrickPaletteScreen() {
        super(Component.literal("Brick Palette"));
    }

    @Override
    protected void init() {
        int w = 200, x = width / 2 - w / 2, y = height / 2 - 80, gap = 24;

        addRenderableWidget(new IntSlider(x, y, w, "Stone Bricks", BrickPaletteClient.wStone, 0, 100,
                v -> BrickPaletteClient.wStone = v));
        addRenderableWidget(new IntSlider(x, y + gap, w, "Mossy", BrickPaletteClient.wMossy, 0, 100,
                v -> BrickPaletteClient.wMossy = v));
        addRenderableWidget(new IntSlider(x, y + gap * 2, w, "Cracked", BrickPaletteClient.wCracked, 0, 100,
                v -> BrickPaletteClient.wCracked = v));

        addRenderableWidget(Button.builder(modeText(), b -> {
            BrickPaletteClient.connectedMode = !BrickPaletteClient.connectedMode;
            b.setMessage(modeText());
        }).bounds(x, y + gap * 3 + 6, w, 20).build());

        addRenderableWidget(new IntSlider(x, y + gap * 4 + 6, w, "Max blocks", BrickPaletteClient.maxBlocks, 1, 256,
                v -> BrickPaletteClient.maxBlocks = v));

        addRenderableWidget(Button.builder(Component.literal("Apply to block I'm looking at"), b -> {
            onClose();
            BrickPaletteClient.apply();
        }).bounds(x, y + gap * 5 + 14, w, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y + gap * 6 + 14, w, 20).build());
    }

    private static Component modeText() {
        return Component.literal("Mode: " + (BrickPaletteClient.connectedMode
                ? "Connected same-blocks" : "Single block"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int total = BrickPaletteClient.wStone + BrickPaletteClient.wMossy + BrickPaletteClient.wCracked;
        String preview = total <= 0 ? "All weights are 0 (will use plain stone bricks)"
                : String.format("Result: %d%% / %d%% / %d%%",
                Math.round(100.0 * BrickPaletteClient.wStone / total),
                Math.round(100.0 * BrickPaletteClient.wMossy / total),
                Math.round(100.0 * BrickPaletteClient.wCracked / total));

        String title = "Brick Palette";
        // font, text, x, y, color, hasShadow
        graphics.text(this.font, title, width / 2 - font.width(title) / 2, height / 2 - 105, 0xFFFFFFFF, true);
        graphics.text(this.font, preview, width / 2 - font.width(preview) / 2, height / 2 - 92, 0xFFAAAAAA, true);
    }

    /** Integer slider with a label. */
    private static class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min, max;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, String label, int current, int min, int max, IntConsumer setter) {
            super(x, y, w, 20, Component.literal(label + ": " + current), (current - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal((label == null ? "" : label) + ": " + current()));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
