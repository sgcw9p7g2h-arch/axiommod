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
    private final List<AxiomFeature> visible = new ArrayList<>();
    private int scroll;
    private int maxScroll;

    public AxiomFeaturesScreen(Screen parent) {
        super(Component.literal("Axiom Features"));
        this.parent = parent;
    }

    @Override protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets();
        visible.clear();
        List<AxiomFeature> all = new ArrayList<>(AxiomClient.FEATURES.all());
        int rows = Math.max(1, (height - 92) / 26);
        maxScroll = Math.max(0, all.size() - rows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int x = width / 2 - 150;
        int y = 48;
        for (int i = scroll; i < all.size() && visible.size() < rows; i++) {
            AxiomFeature feature = all.get(i);
            visible.add(feature);
            addRenderableWidget(Button.builder(label(feature), b -> {
                feature.setEnabled(!feature.enabled());
                AxiomClient.FEATURES.save();
                b.setMessage(label(feature));
            }).bounds(x, y, 300, 22).build());
            y += 26;
        }
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> { if(scroll>0){scroll--;rebuild();} })
                .bounds(x, height-58, 48, 22).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> { if(scroll<maxScroll){scroll++;rebuild();} })
                .bounds(x+252, height-58, 48, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x+56, height-58, 188, 22).build());
    }

    private Component label(AxiomFeature f) {
        return Component.literal(f.category()+"  •  "+f.name()+": "+(f.enabled()?"ON":"OFF"));
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta > 0 && scroll > 0) { scroll--; rebuild(); return true; }
        if (delta < 0 && scroll < maxScroll) { scroll++; rebuild(); return true; }
        return super.mouseScrolled(mx,my,delta);
    }

    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        g.drawCenteredString(font,title,width/2,18,0xFFFFFF);
        g.drawCenteredString(font,Component.literal("Feature settings • F8"),width/2,30,0xAAAAAA);
        super.render(g,mx,my,delta);
    }

    @Override public void onClose() {
        AxiomClient.FEATURES.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}