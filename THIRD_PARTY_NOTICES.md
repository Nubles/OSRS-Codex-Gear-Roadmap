# Third-party notices

## Old School RuneScape Wiki

Equipment stats, monster stats, attack styles and which ammunition each ranged weapon fires come from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki), collected by the wiki's own [DPS calculator](https://github.com/weirdgloop/osrs-dps-calc) (`cdn/json` at commit `89c3e25b344aea90d0189746e4b5f73dde0f0383`). Wiki content is licensed under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/), so the bundled data file `src/main/resources/com/codex/gearroadmap/gear-roadmap.json` is shared under the same license. `tools/build_data.py` rebuilds it from a checkout of the calculator.

No code from the DPS calculator is included. Codex Gear Roadmap's combat formulas are its own implementation of the game's mechanics as the wiki documents them, and its tests check them against results from the calculator.

The upgrade catalogue, item requirements and how each upgrade is earned are Codex's own.
