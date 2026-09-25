# Axiom

A standalone Minecraft Java client and launcher for Windows, with the current Fabric codebase serving as the technical foundation for client-side systems.

## Client

The final product is intended to behave like a standalone Minecraft client rather than a mod that users manually install into another launcher.

The Windows launcher is responsible for:

- Microsoft account authentication.
- Axiom game-directory management.
- Minecraft version installation.
- Fabric runtime setup used by the current technical foundation.
- Axiom client updates.
- Per-profile game directories and RAM settings.
- Launching Minecraft through the Axiom profile.
- Packaging a standalone `AxiomLauncher.exe` release.

The current launcher milestone targets Minecraft Java 1.21.11.

## Development direction

The standalone launcher/client pipeline is the active development priority. The existing in-game feature system is intentionally frozen while the client foundation is completed.

The repository may contain Fabric-based prototype components because they are the current technical foundation. They are not the final product definition.

## Build

The repository contains both the Java client foundation and the Windows launcher.

For the Java foundation, use JDK 21 and run `gradlew.bat build` on Windows or `./gradlew build` on macOS/Linux.

For the Windows launcher, use .NET 8 and run:

```text
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true
```

The release workflow builds both components and publishes the launcher executable together with the current Axiom client asset.

## Repository structure

- `launcher/AxiomLauncher/` — standalone Windows launcher.
- `client/axiom-client.json` — release manifest that keeps the launcher and client runtime versions in sync.
- `src/main/java/` — current Java/Fabric technical foundation.
- `.github/workflows/` — client, launcher, verification, and release automation.
