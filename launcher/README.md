# Axiom Launcher

Windows-first launcher for the Axiom standalone Minecraft client.

## Product architecture

The finished product is **Axiom Client**, not a Minecraft mod:

**Axiom Launcher → Axiom Client Runtime → Minecraft**

The current Fabric integration is only the first technical foundation used to prove the launcher, authentication, installation, repair, update, and Minecraft process pipeline. It is not the final Axiom product architecture.

## Current launcher foundation

- Microsoft authentication without collecting a Microsoft password
- Axiom profiles and per-profile game directories
- RAM configuration
- Minecraft installation and launch pipeline
- Client release manifest
- Verified client asset downloads
- Runtime integrity state and repair flow
- Separate Axiom runtime storage under the game directory's `.axiom` folder
- Windows self-contained launcher publishing

## Standalone client migration

The runtime is being separated into its own Axiom-owned directory and state model so the launcher can progressively move away from treating Axiom as a conventional Fabric mod.

The migration target is:

1. Launcher owns authentication, installation, profiles, updates, and repair.
2. Axiom runtime owns client startup and client-specific services.
3. Minecraft is launched as the game runtime managed by Axiom.
4. The final distribution ships as an installable Axiom Client rather than requiring users to manually install a mod.

Feature development remains frozen while this architecture is built.
