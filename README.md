# Ultras Mod — Fabric 1.21.4

Minecraft Java Edition 1.21.4 · Fabric Loader · Java 21

## v0.3.6

- Placeable supporter flag and banner blocks with a four-frame cloth animation.
- Eight team-colour supporter NPC spawn items; no summon commands are required.
- In-game 16×16 flag/banner art editor.
- Scarf, megaphone, marching drum, clapper, and decorative red smoke particles.
- Original mono OGG sounds for megaphone, drum, and clapper.
- New 64×64 item textures and 64×256 animated cloth textures.

## Install

1. Use Minecraft Java 1.21.4 with Fabric Loader and Fabric API.
2. Put the downloaded `ultras-mod-0.3.6.jar` in `.minecraft/mods`.
3. Launch the Fabric 1.21.4 profile.

## Art editor limitation

The palette currently saves artwork to a resource pack and applies the design globally to all flags or all banners of that type. After saving, enable `UltrasMod_Custom_Art` in Resource Packs and press F3+T. Per-block saved designs are not yet supported.

## Build

GitHub Actions builds the mod with Java 21 and Gradle 8.12. The build generates the original image and audio assets before packaging the distributable JAR.
