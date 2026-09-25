package axiom.client.ui;

import axiom.client.AxiomClient;
import axiom.client.features.AxiomFeature;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class AxiomFeaturesScreen extends Screen {
    private final Screen parent;
    private final List<Button> buttons = new ArrayList<>();

    public AxiomFeaturesScreen(Screen parent) {
        super(Component.literal("Axiom Features"));
        this.parent = parent;
    }

    @Override protected void init() {
        buttons.clear();
        List<AxiomFeature> all = new ArrayList<>(AxiomClient.FEATURES.all());
        int x = width / 2 - 155;
        int y = 48;
        int column = 0;
        for (AxiomFeature feature : all) {
            int bx = x + column * 158;
            int by = y;
            Button button = Button.builder(label(feature), b -> {
                feature.setEnabled(!feature.enabled());
                AxiomClient.FEATURES.save();
                b.setMessage(label(feature));
            }).bounds(bx, by, 150, 24).build();
            buttons.add(button);
            addRenderableWidget(button);
            if (++column == 2) {
                column = 0;
                y += 30;
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 100, height - 30, 200, 20).build());
    }

    private Component label(AxiomFeature feature) {
        return Component.literal(feature.name() + ": " + (feature.enabled() ? "ON" : "OFF"));
    }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        g.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal("Axiom features • F8"), width / 2, 31, 0xAAAAAA);
        super.render(g, mx, my, delta);
    }

    @Override public void onClose() {
        AxiomClient.FEATURES.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}