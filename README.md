# Create-EMI Stockkeeper Compat

A Minecraft Forge 1.20.1 compatibility mod that bridges [EMI](https://github.com/emilyploszaj/emi)'s recipe tree system with [Create](https://github.com/Creators-of-Create/Create)'s Stockkeeper block.

## Features

### Recipe Ingredients Category
When you pin a recipe tree in EMI, a **Recipe Ingredients** pseudo-category appears at the top of the Stockkeeper UI. Items are listed in recipe tree order, so you can see exactly what you need at a glance.

### Color-Coded Highlights
Items in the Stockkeeper are highlighted based on fulfillment:
- **Green** — You have enough in stock
- **Yellow** — Partial stock (some but not enough)
- **Red** — Missing entirely (not in your network)

### Needed Quantity Overlay
Each ingredient shows the required quantity (top-left) alongside the stock count (bottom-right), so you can compare what you need vs. what you have.

### Missing Items
Items needed by the recipe but not present in your storage network appear as red entries with their icons, so you know what to go craft or find.

### Auto-Order (Alt+Click)
Alt+Click any ingredient in the Stockkeeper to automatically queue the exact amount needed for your recipe into the export slots.

### Configurable
Click the gear icon above the item grid to toggle features on/off:
- Show Missing Items
- Show Needed Quantities
- Color Highlights
- Alt+Click Auto-Order

Settings persist to `config/createemicompat.properties`.

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- [Create](https://www.curseforge.com/minecraft/mc-mods/create) 6.0+
- [EMI](https://www.curseforge.com/minecraft/mc-mods/emi) 1.1+

## Installation

1. Download the latest jar from [Releases](https://github.com/Seiak/create-emi-stockkeeper/releases)
2. Drop it into your `mods/` folder
3. Launch the game

## Building from Source

```bash
git clone https://github.com/Seiak/create-emi-stockkeeper.git
cd create-emi-stockkeeper
# Place Create and EMI jars in libs/
./gradlew build
# Output: build/libs/create-emi-stockkeeper-*.jar
```

## Disclaimer

This mod was built with the assistance of AI (Claude) because the functionality didn't exist and I needed it. If that bothers you, don't download it — or better yet, take the code and make it better. That's why it's MIT licensed.

This was developed and tested with [TerraFirmaGreg](https://github.com/TerraFirmaGreg-Team/Modpack-Modern) (TFG) in mind. It should work with any modpack running Create 6.0+ and EMI on Forge 1.20.1, but your experience may vary outside of TFG.

## License

[MIT](LICENSE) — do whatever you want with it.
