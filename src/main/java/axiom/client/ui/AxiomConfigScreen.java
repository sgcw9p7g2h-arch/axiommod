package axiom.client.ui;

import axiom.client.AxiomClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class AxiomConfigScreen extends Screen {
    private final Screen parent;
    private KeyMapping waitingFor;
    private Button waitingButton;
    private Button skipExistingButton;
    private Button previewButton;

    public AxiomConfigScreen(Screen parent) {
        super(Component.literal("Axiom Settings"));
        this.parent = parent;
    }

    @Override protected void init() {
        int x = width / 2 - 155;
        int y = 45;

        addRenderableWidget(keyButton("Open browser", AxiomClient.menuKey(), x, y));
        addRenderableWidget(keyButton("Build / pause", AxiomClient.buildKey(), x, y += 30));
        addRenderableWidget(keyButton("Pause / resume", AxiomClient.pauseKey(), x, y += 30));
        addRenderableWidget(keyButton("Set origin", AxiomClient.originKey(), x, y += 30));
        addRenderableWidget(keyButton("Rotate", AxiomClient.rotateKey(), x, y += 30));
        addRenderableWidget(keyButton("Cancel build", AxiomClient.cancelKey(), x, y += 30));

        skipExistingButton = Button.builder(
                Component.literal("Skip existing: " + (AxiomClient.CONFIG.skipExisting ? "ON" : "OFF")),
                b -> {
                    AxiomClient.CONFIG.skipExisting = !AxiomClient.CONFIG.skipExisting;
                    AxiomClient.CONFIG.save();
                    b.setMessage(Component.literal("Skip existing: " + (AxiomClient.CONFIG.skipExisting ? "ON" : "OFF")));
                }).bounds(x, y += 34, 310, 20).build();
        addRenderableWidget(skipExistingButton);

        previewButton = Button.builder(
                Component.literal("Preview: " + (axiom.client.schematic.SchematicPreview.enabled() ? "ON" : "OFF")),
                b -> {
                    axiom.client.schematic.SchematicPreview.toggle();
                    b.setMessage(Component.literal("Preview: " + (axiom.client.schematic.SchematicPreview.enabled() ? "ON" : "OFF")));
                }).bounds(x, y += 25, 310, 20).build();
        addRenderableWidget(previewButton);

        addRenderableWidget(Button.builder(Component.literal("Build delay: " + AxiomClient.CONFIG.tickDelay + " ticks"),
                b -> {
                    AxiomClient.CONFIG.tickDelay = AxiomClient.CONFIG.tickDelay >= 10 ? 0 : AxiomClient.CONFIG.tickDelay + 1;
                    AxiomClient.CONFIG.save();
                    b.setMessage(Component.literal("Build delay: " + AxiomClient.CONFIG.tickDelay + " ticks"));
                }).bounds(x, y += 25, 310, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y += 35, 310, 20).build());
    }

    private String keyLabel(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private Button keyButton(String label, KeyMapping mapping, int x, int y) {
        return Button.builder(Component.literal(label + ": " + mapping.getTranslatedKeyMessage().getString()), b -> {
            waitingFor = mapping;
            waitingButton = b;
            b.setMessage(Component.literal(label + ": press a key..."));
        }).bounds(x, y, 310, 20).build();
    }

    @Override public boolean keyPressed(KeyEvent key) {
        if (waitingFor != null) {
            int code = key.key();
            if (code == 256) {
                waitingFor.setKey(InputConstants.UNKNOWN);
            } else {
                waitingFor.setKey(InputConstants.getKey(key));
            }
            minecraft.options.save();
            if (waitingButton != null) {
                waitingButton.setMessage(Component.literal(keyLabel(waitingFor)));
            }
            waitingFor = null;
            waitingButton = null;
            return true;
        }
        if (key.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(key);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        g.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal("Click a bind, then press the new key."), width / 2, 32, 0xAAAAAA);
        super.render(g, mx, my, delta);
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
