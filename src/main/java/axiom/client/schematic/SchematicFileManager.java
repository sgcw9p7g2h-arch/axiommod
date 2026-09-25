package axiom.client.schematic;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loads Axiom and Litematica schematics from the game directory. */
public final class SchematicFileManager {
    private final Path dir = FabricLoader.getInstance().getGameDir().resolve("axiom/schematics");
    private static final int MAX_SCHEMATIC_FILES = 128;
    private static final long MAX_AXSCHEM_BYTES = 16 * 1024 * 1024L;
    private final List<SchematicEngine> loaded = new ArrayList<>();
    private SchematicEngine selected;

    public void loadAll() {
        loaded.clear();
        selected = null;
        try {
            Files.createDirectories(dir);
            try (var stream = Files.list(dir)) {
                stream.filter(p -> { String n=p.getFileName().toString().toLowerCase(java.util.Locale.ROOT); return n.endsWith(".axschem") || n.endsWith(".litematic"); })
                        .sorted(java.util.Comparator.comparing(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT)))
                        .limit(MAX_SCHEMATIC_FILES)
                        .forEach(path -> {
                            try {
                                loaded.add(loadAny(path));
                            } catch (Exception ignored) {
                                // Ignore one bad schematic instead of preventing the others from loading.
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }

    public List<SchematicEngine> all() { return Collections.unmodifiableList(loaded); }
    public SchematicEngine selected() { return selected; }
    public Path directory() { return dir; }

    public void select(int index) {
        if (index >= 0 && index < loaded.size()) selected = loaded.get(index);
    }


    private SchematicEngine loadAny(Path path) throws IOException {
        String name=path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if(name.endsWith(".litematic")) return new LitematicImporter().load(path);
        return load(path);
    }

    private SchematicEngine load(Path path) throws IOException {
        if (Files.size(path) > MAX_AXSCHEM_BYTES) throw new IOException("AXSCHEM file is larger than 16 MB");
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty() || !"AXSCHEM 1".equals(lines.get(0).trim())) {
            throw new IOException("Bad AXSCHEM header");
        }

        List<SchematicBlock> blocks = new ArrayList<>();
        for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
            String line = lines.get(lineNumber - 1).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) throw new IOException("Invalid block at line " + lineNumber);
            String[] xyz = parts[0].trim().split(",");
            if (xyz.length != 3 || parts[1].trim().isEmpty()) {
                throw new IOException("Invalid block at line " + lineNumber);
            }

            try {
                blocks.add(new SchematicBlock(
                        Integer.parseInt(xyz[0].trim()),
                        Integer.parseInt(xyz[1].trim()),
                        Integer.parseInt(xyz[2].trim()),
                        parts[1].trim()));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid coordinates at line " + lineNumber, e);
            }
        }
        return new SchematicEngine(stripExtension(path.getFileName().toString()), blocks);
    }

    private String stripExtension(String name) {
        if (name.endsWith(".axschem")) return name.substring(0, name.length() - ".axschem".length());
        if (name.endsWith(".litematic")) return name.substring(0, name.length() - ".litematic".length());
        return name;
    }
}
