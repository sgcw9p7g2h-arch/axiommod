package axiom.client.features;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public final class FeatureManager {
    private final Map<String, AxiomFeature> features = new LinkedHashMap<>();
    private final Path file;

    public FeatureManager(Path configDir) {
        this.file = configDir.resolve("axiom-features.properties");
        registerDefaults();
    }

    private void registerDefaults() {
        register("toggle_sprint", "Toggle Sprint", "Movement", true);
        register("toggle_sneak", "Toggle Sneak", "Movement", false);
        register("zoom", "Zoom", "Visual", true);
        register("keystrokes", "Keystrokes", "HUD", true);
        register("cps", "CPS Counter", "HUD", true);
        register("coordinates", "Coordinates", "HUD", true);
        register("armor_hud", "Armor HUD", "HUD", true);
        register("fps", "FPS Counter", "HUD", true);
        register("fullbright", "Fullbright", "Visual", false);
        register("crosshair", "Crosshair", "Visual", true);
        register("screenshot", "Screenshot Tools", "Utility", true);
    }

    public void register(String id, String name, String category, boolean enabled) {
        features.putIfAbsent(id, new AxiomFeature(id, name, category, enabled));
    }

    public Collection<AxiomFeature> all() { return features.values(); }

    public AxiomFeature get(String id) { return features.get(id); }

    public boolean isEnabled(String id) {
        AxiomFeature feature = features.get(id);
        return feature != null && feature.enabled();
    }

    public void setEnabled(String id, boolean enabled) {
        AxiomFeature feature = features.get(id);
        if (feature != null) feature.setEnabled(enabled);
    }

    public void load() {
        if (!Files.exists(file)) return;
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file)) {
            properties.load(input);
            for (AxiomFeature feature : features.values()) {
                String value = properties.getProperty(feature.id());
                if (value != null) feature.setEnabled(Boolean.parseBoolean(value));
            }
        } catch (IOException ignored) {
        }
    }

    public void save() {
        Properties properties = new Properties();
        for (AxiomFeature feature : features.values())
            properties.setProperty(feature.id(), Boolean.toString(feature.enabled()));
        try {
            Files.createDirectories(file.getParent());
            try (var output = Files.newOutputStream(file)) {
                properties.store(output, "Axiom feature settings");
            }
        } catch (IOException ignored) {
        }
    }
}
