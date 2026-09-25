package axiom.client.schematic;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Loads Axiom and Litematica schematics from the game directory. */
public final class SchematicFileManager {
    private static final int MAX_SCHEMATIC_FILES = 128;
    private static final long MAX_AXSCHEM_BYTES = 16 * 1024 * 1024L;
    private static final int MAX_AXSCHEM_BLOCKS = 1_000_000;

    private final Path dir = FabricLoader.getInstance().getGameDir().resolve("axiom/schematics");
    private final List<SchematicEngine> loaded = new ArrayList<>();
    private SchematicEngine selected;

    public void loadAll() {
        loaded.clear();
        selected = null;
        try {
            Files.createDirectories(dir);
            try (var stream = Files.list(dir)) {
                stream.filter(p -> {
                            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            return n.endsWith(".axschem") || n.endsWith(".litematic");
                        })
                        .sorted(java.util.Comparator.comparing(
                                p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
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
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".litematic")) return new LitematicImporter().load(path);
        return load(path);
    }

    private SchematicEngine load(Path path) throws IOException {
        if (Files.size(path) > MAX_AXSCHEM_BYTES) {
            throw new IOException("AXSCHEM file is larger than 16 MB");
        }

        List<SchematicBlock> blocks = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!"AXSCHEM 1".equals(header == null ? null : header.trim())) {
                throw new IOException("Bad AXSCHEM header");
            }

            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (blocks.size() >= MAX_AXSCHEM_BLOCKS) {
                    throw new IOException("AXSCHEM contains more than 1,000,000 blocks");
                }

                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) {
                    throw new IOException("Invalid block at line " + lineNumber);
                }

                String[] xyz = parts[0].trim().split(",");
                String blockId = parts[1].trim();
                if (xyz.length != 3 || blockId.isEmpty()) {
                    throw new IOException("Invalid block at line " + lineNumber);
                }

                try {
                    blocks.add(new SchematicBlock(
                            Integer.parseInt(xyz[0].trim()),
                            Integer.parseInt(xyz[1].trim()),
                            Integer.parseInt(xyz[2].trim()),
                            blockId));
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid coordinates at line " + lineNumber, e);
                }
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
