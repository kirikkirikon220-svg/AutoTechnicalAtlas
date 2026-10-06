# Auto Technical Atlas

Personal offline Android technical atlas for vehicle configurations.

## Current application layer

- vehicle selector;
- offline JSON vehicle packages;
- JSON import from Android file picker;
- professional monochrome engineering viewer;
- zoom / pan / rotation gestures;
- exploded-view slider;
- system isolation;
- component search;
- component selection;
- connection tracing/highlighting;
- source and verification metadata;
- GitHub Actions Release APK publishing.

## Data architecture

Vehicle
→ model
→ generation
→ year
→ modification
→ engine
→ transmission
→ drivetrain
→ systems
→ components
→ connections
→ sources

Each exact configuration is a separate dataset. The app does not assume that two engines, gearboxes or model years share the same technical routing.

## JSON package

A vehicle package contains:

- `systems` — technical systems;
- `components` — physical parts/nodes and drawing coordinates;
- `connections` — wires, CAN/LIN, hoses, fuel/oil/coolant, brake and mechanical links;
- `sources` — origin, scheme number, configuration and verification state.

The built-in BMW package is deliberately marked `verified: false`. It exists to test the application engine and must not be treated as factory documentation.

## Next data stage

Verified packages can be imported through `ИМПОРТ JSON` without changing the application code.
