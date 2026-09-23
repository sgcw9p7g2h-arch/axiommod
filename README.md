# Axiom

A client-side Fabric mod for Minecraft Java 1.21.11 focused on schematic building and utilities.

## Features

- Imports `.litematic` files, including multiple regions, palettes, packed block states, and negative region sizes.
- Supports legacy `.axschem` files and full serialized block-state properties.
- Browses local schematics and public online `.litematic` files.
- Previews schematics with a wireframe, tracks materials, and builds bottom-up using ordinary Minecraft interactions.
- Supports rotation, X/Z mirrors, existing-block skipping, reach checks, pause/resume/cancel, and a HUD.

## Controls

- **M** — open the schematic browser
- **B** — start, pause, or resume a build
- **P** — pause or resume
- **O** — set the build origin from the block you are looking at
- **R** — rotate the selected schematic clockwise
- **X** — cancel a build
- **Enter** — select a schematic in the browser
- **Ctrl** — open Online Schematics from the browser

## Schematic folder

Put `.litematic` or `.axschem` files in `.minecraft/axiom/schematics/`.

## Build

Install JDK 21 and run `gradlew.bat build` on Windows, or `./gradlew build` on macOS/Linux. The first build needs internet access to download Gradle and Minecraft/Fabric dependencies. The remapped mod JAR is written to `build/libs/`.

Fabric Loader and Fabric API are required in the Minecraft installation. Test in a disposable world first. Blocks whose placement depends on player orientation, block entities, or multi-block behavior may need in-game adjustment.

The builder uses normal Minecraft interaction. It does not use packet spoofing, anti-cheat bypasses, exploit abuse, or hidden-player-information discovery.
