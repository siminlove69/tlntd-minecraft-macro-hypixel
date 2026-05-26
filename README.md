# TLNTD — Professional Farming Macro

A client-side **Fabric mod** for Minecraft that automates crop farming with a fully custom in-game UI, configurable macros per crop type, safety systems, and session tracking.

---

## Features

### Macros
- Per-crop macro configuration for 14 crop types: Sugar Cane, Pumpkin, Melon, Nether Wart, Wheat, Potato, Carrot, Red & Brown Mushroom, Moonflower, Sunflower, Wild Rose, Cocoa Beans, and Cactus
- Step-based macro editor — define sequences of actions (drag to reorder)
- General toggles: re-equip tool, auto-sell, replant, and more
- Color-coded crop cards with icons

### Safety System
- Stop when a player is detected within a configurable range (5–100 blocks)
- Stop when Hypixel staff appear in the tab list
- TPS auto-pause — stops the macro when server TPS drops below a set threshold
- Input detection — pauses on mouse movement or key press
- Player whitelist — trusted players that won't trigger the stop condition

### Dashboard
- Live session stats: crops collected, session uptime, farming time
- Bot state display (running / stopped)
- Real-time event log with timestamps (START, STOP, SAFETY, INFO events)
- Toast notifications

### HUD
- On-screen overlay showing macro status while the menu is closed
- Configurable hearts display
- Minimal, unobtrusive design

### Settings
- RGB rainbow accent mode or custom R/G/B accent color
- Theme presets (multiple color schemes)
- Background style selector: Hearts, Stars, Matrix, or None
- UI scale adjustment
- Anti-AFK toggle with configurable interval
- Scheduled auto-stop timer
- Profile system — save, load, export/import configurations
- Full config reset

### Console
- In-game console log for debug and status output

---

## Requirements

| Requirement | Version |
|---|---|
| Minecraft | 1.21.10 |
| Fabric Loader | ≥ 0.18.4 |
| Fabric API | 0.138.4+1.21.10 |
| Java | ≥ 21 |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.21.10**
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your `mods/` folder
3. Build TLNTD (see below) or place the compiled `.jar` in your `mods/` folder
4. Launch Minecraft with the Fabric profile

---

## Building from Source

Requirements: JDK 21+, internet connection (Gradle downloads dependencies automatically)

```bash
git clone <repo-url>
cd TLNTD
./gradlew build
```

The compiled mod jar will be output to:
```
build/libs/TLNTD-1.0.0.jar
```

Copy it to your `.minecraft/mods/` folder.

---

## Project Structure

```
src/main/java/me/shimmy/tlntd/
├── Tlntd.java            # Mod entry point, bot logic, session stats
├── TlntdMenu.java        # Main in-game GUI window
├── TlntdConfig.java      # Config model, profiles, presets
├── TlntdHud.java         # On-screen HUD overlay
├── UI.java               # Shared rendering utilities
├── IMenuSection.java     # Section interface
├── MacrosSection.java    # Crop macro editor
├── DashboardSection.java # Live stats dashboard
├── SafetySection.java    # Safety & detection settings
├── SettingsSection.java  # Visual & behavior settings
├── EventsSection.java    # Event log viewer
├── ConsoleSection.java   # Console output
├── HudSection.java       # HUD settings
├── StepData.java         # Macro step model
└── mixin/
    ├── BlockBreakMixin.java  # Block break event hook
    └── MouseMixin.java       # Mouse input hook
```

---

## Usage

1. Join a Minecraft server
2. Press the configured keybind (default: set in controls) to open the TLNTD menu
3. Go to **Macros** → select your crop → configure steps
4. Set up **Safety** rules as needed
5. Press **Start** from the Dashboard or use the hotkey

---

## License

All Rights Reserved — see `fabric.mod.json` for details.
