# Axiom Launcher

Windows-first standalone launcher for Axiom.

## Planned release flow

Axiom Launcher → select Axiom profile/version → authenticate with Microsoft → install/update Fabric + Axiom → launch Minecraft.

## Design goals

- Lightweight launcher with no in-game overhead after launch.
- Separate Axiom game directory.
- Profile/version management.
- Automatic Axiom updates.
- Official Microsoft authentication flow; the launcher never asks for a Microsoft password.
- Fabric support for Minecraft 1.21.11.
- Repair/reinstall controls.

The current first milestone is the Windows launcher shell and Minecraft launch pipeline. Online Microsoft authentication and Fabric/Axiom profile installation are the next milestone.
