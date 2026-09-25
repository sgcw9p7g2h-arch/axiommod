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
        int x = width / 2 - 150;
        int y = 42;
        String category = "";
        for (AxiomFeature feature : AxiomClient.FEATURES.all()) {
            if (!feature.category().equals(category)) {
                category = feature.category();
                y += 8;
                addRenderableWidget(Button.builder(Component.literal("[" + category + "]"), b -> {})
                        .bounds(x, y, 300, 20).build());
                y += 24;
            }
            Button button = Button.builder(label(feature), b -> {
                feature.setEnabled(!feature.enabled());
                AxiomClient.FEATURES.save();
                b.setMessage(label(feature));
            }).bounds(x, y, 300, 20).build();
            buttons.add(button);
            addRenderableWidget(button);
            y += 24;
            if (y > height - 60) break;
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, height - 30, 300, 20).build());
    }

    private Component label(AxiomFeature feature) {
        return Component.literal(feature.name() + ": " + (feature.enabled() ? "ON" : "OFF"));
    }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        g.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal("Axiom client features"), width / 2, 30, 0xAAAAAA);
        super.render(g, mx, my, delta);
    }

    @Override public void onClose() {
        AxiomClient.FEATURES.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
