#!/usr/bin/env python3
"""Builds src/main/resources/com/codex/gearroadmap/gear-roadmap.json.

Inputs are the equipment and monster tables the OSRS Wiki's DPS calculator
publishes (weirdgloop/osrs-dps-calc, cdn/json), which are gathered from the
Old School RuneScape Wiki. Only game facts are taken: item ids, slots, attack
speeds, combat bonuses, weapon categories and their attack styles, and monster
defensive stats. The upgrade catalogue below (which items are worth buying or
earning, their wield requirements and what they cost to make) is Codex's own.

Usage: build_data.py <osrs-dps-calc checkout> <output json>
"""
import json
import os
import re
import sys

SLOTS = ["head", "cape", "neck", "ammo", "weapon", "body", "shield", "legs", "hands", "feet", "ring"]

# Which entry of a same-named group carries the stats used for it.
VERSION_PREFERENCE = ["Charged", "", "Normal", "Unpoisoned", "Active", "Activated", "Restored", "Undamaged",
                      "100", "Untrimmed", "Inactive", "Uncharged", "Empty"]

# Upgrade catalogue. req uses skill names as the game shows them. build lists the
# tradeable parts that are bought to make an item that is not itself tradeable;
# earn names where an untradeable item comes from. note is shown under the item.
A, S, D, R, M, P = "Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer"
CATALOGUE = [
    # Melee weapons
    ("Rune scimitar", {A: 40}),
    ("Dragon scimitar", {A: 60}, {"note": "Needs Monkey Madness I"}),
    ("Dragon longsword", {A: 60}),
    ("Dragon mace", {A: 60}),
    ("Dragon sword", {A: 60}),
    ("Dragon warhammer", {S: 60}),
    ("Abyssal whip", {A: 70}),
    ("Abyssal tentacle", {A: 75}, {"build": [12004, 4151], "note": "A whip and a kraken tentacle"}),
    ("Abyssal dagger", {A: 70}),
    ("Abyssal bludgeon", {S: 70}),
    ("Zamorakian hasta", {A: 70}),
    ("Saradomin sword", {A: 70}),
    ("Dragon claws", {A: 60}),
    ("Bandos godsword", {A: 75}),
    ("Elder maul", {A: 75, S: 75}),
    ("Blade of saeldor", {A: 75}, {"build": [25859], "note": "Made from an enhanced crystal weapon seed"}),
    ("Ghrazi rapier", {A: 80}),
    ("Inquisitor's mace", {A: 80}),
    ("Scythe of vitur", {A: 75, S: 75}),
    ("Osmumten's fang", {A: 82}),
    ("Voidwaker", {A: 75}),
    ("Noxious halberd", {A: 80, S: 80}),
    ("Soulreaper axe", {A: 80, S: 80}),
    ("Dual macuahuitl", {A: 85}),
    ("Dragon hunter lance", {A: 78}),
    ("Arclight", {A: 75}, {"earn": "Darklight and three ancient shards"}),
    ("Emberlight", {A: 75}, {"earn": "Arclight and a tormented synapse"}),
    # Melee armour
    ("Tzhaar-ket-om", {S: 60}),
    ("Toktz-xil-ak", {A: 60}),
    ("Rune full helm", {D: 40}),
    ("Berserker helm", {D: 45}),
    ("Helm of neitiznot", {D: 55}, {"note": "Needs The Fremennik Isles"}),
    ("Serpentine helm", {D: 75}),
    ("Neitiznot faceguard", {D: 70}, {"build": [24268], "note": "A helm of neitiznot and a basilisk jaw"}),
    ("Justiciar faceguard", {D: 75}),
    ("Inquisitor's great helm", {S: 70, D: 30}),
    ("Torva full helm", {D: 80}, {"build": [26376], "note": "Damaged helm, repaired after Nex"}),
    ("Obsidian helmet", {D: 60}),
    ("Rune platebody", {D: 40}, {"note": "Needs Dragon Slayer I"}),
    ("Fighter torso", {D: 40}, {"earn": "Barbarian Assault"}),
    ("Obsidian platebody", {D: 60}),
    ("Bandos chestplate", {D: 65}),
    ("Inquisitor's hauberk", {S: 70, D: 30}),
    ("Torva platebody", {D: 80}, {"build": [26378], "note": "Damaged body, repaired after Nex"}),
    ("Rune platelegs", {D: 40}),
    ("Dragon platelegs", {D: 60}),
    ("Obsidian platelegs", {D: 60}),
    ("Bandos tassets", {D: 65}),
    ("Inquisitor's plateskirt", {S: 70, D: 30}),
    ("Torva platelegs", {D: 80}, {"build": [26380], "note": "Damaged legs, repaired after Nex"}),
    ("Rune kiteshield", {D: 40}),
    ("Dragon defender", {A: 60, D: 60}, {"earn": "Warriors' Guild cyclopes"}),
    ("Avernic defender", {A: 70, D: 70}, {"build": [22477], "note": "A dragon defender and an avernic hilt"}),
    ("Amulet of strength", {}),
    ("Amulet of glory", {}),
    ("Amulet of fury", {}),
    ("Amulet of torture", {}),
    ("Amulet of blood fury", {}, {"build": [6585, 24777], "note": "An amulet of fury and a blood shard"}),
    ("Berserker necklace", {}),
    ("Salve amulet", {}, {"earn": "Haunted Mine quest"}),
    ("Salve amulet (e)", {}, {"earn": "Salve amulet and Lumbridge Elite tome"}),
    ("Salve amulet(ei)", {}, {"earn": "Imbue a salve amulet (e)"}),
    ("Obsidian cape", {}),
    ("Fire cape", {}, {"earn": "TzHaar Fight Cave"}),
    ("Infernal cape", {}, {"earn": "The Inferno"}),
    ("Combat bracelet", {}),
    ("Dragon gloves", {}, {"earn": "Recipe for Disaster"}),
    ("Barrows gloves", {}, {"earn": "Recipe for Disaster"}),
    ("Ferocious gloves", {A: 80, D: 80}, {"build": [22983], "note": "Made from hydra leather"}),
    ("Rune boots", {D: 40}),
    ("Dragon boots", {D: 60}),
    ("Primordial boots", {S: 75, D: 75}),
    ("Berserker ring", {}),
    ("Berserker ring (i)", {}, {"build": [6737], "note": "Imbue a berserker ring"}),
    ("Ultor ring", {}),
    ("Bellator ring", {}),
    # Ranged weapons
    ("Magic shortbow", {R: 50}),
    ("Magic shortbow (i)", {R: 50}, {"build": [861], "note": "Imbue a magic shortbow"}),
    ("Rune crossbow", {R: 61}),
    ("Dragon crossbow", {R: 64}),
    ("Dark bow", {R: 60}),
    ("Armadyl crossbow", {R: 70}),
    ("Dragon hunter crossbow", {R: 65}),
    ("Heavy ballista", {R: 75}),
    ("Toxic blowpipe", {R: 75}, {"build": [12924], "note": "With dragon darts"}),
    ("Crystal bow", {R: 50}, {"build": [4207], "note": "Made from a crystal weapon seed"}),
    ("Bow of faerdhinen", {R: 80, "Agility": 70}, {"build": [25859], "note": "Made from an enhanced crystal weapon seed"}),
    ("Venator bow", {R: 80}),
    ("Twisted bow", {R: 75}),
    ("Zaryte crossbow", {R: 80}),
    # Ammunition
    ("Rune arrow", {R: 40}),
    ("Amethyst arrow", {R: 50}),
    ("Dragon arrow", {R: 60}),
    ("Adamant bolts", {R: 46}),
    ("Runite bolts", {R: 61}),
    ("Diamond bolts (e)", {R: 61}),
    ("Ruby bolts (e)", {R: 61}),
    ("Dragon bolts", {R: 64}),
    ("Ruby dragon bolts (e)", {R: 64}),
    ("Diamond dragon bolts (e)", {R: 64}),
    ("Dragon javelin", {R: 75}),
    # Ranged armour
    ("Black d'hide body", {R: 70, D: 40}),
    ("Black d'hide chaps", {R: 70}),
    ("Black d'hide vambraces", {R: 70}),
    ("Archer helm", {D: 45}),
    ("Armadyl helmet", {R: 70, D: 70}),
    ("Armadyl chestplate", {R: 70, D: 70}),
    ("Armadyl chainskirt", {R: 70, D: 70}),
    ("Masori mask", {R: 80, D: 30}),
    ("Masori body", {R: 80, D: 30}),
    ("Masori chaps", {R: 80, D: 30}),
    ("Crystal helm", {R: 70, D: 70, "Agility": 50}, {"build": [23956], "note": "Made from a crystal armour seed"}),
    ("Crystal body", {R: 70, D: 70, "Agility": 50}, {"build": [23956, 23956, 23956], "note": "Three crystal armour seeds"}),
    ("Crystal legs", {R: 70, D: 70, "Agility": 50}, {"build": [23956, 23956], "note": "Two crystal armour seeds"}),
    ("Void knight top", {A: 42, S: 42, D: 42, R: 42, M: 42, P: 22}, {"earn": "Pest Control"}),
    ("Void knight robe", {A: 42, S: 42, D: 42, R: 42, M: 42, P: 22}, {"earn": "Pest Control"}),
    ("Void knight gloves", {A: 42, S: 42, D: 42, R: 42, M: 42, P: 22}, {"earn": "Pest Control"}),
    ("Void ranger helm", {A: 42, S: 42, D: 42, R: 42, M: 42, P: 22}, {"earn": "Pest Control"}),
    ("Void melee helm", {A: 42, S: 42, D: 42, R: 42, M: 42, P: 22}, {"earn": "Pest Control"}),
    ("Void mage helm", {A: 42, S: 42, D: 42, R: 42, M: 42, P: 22}, {"earn": "Pest Control"}),
    ("Elite void top", {A: 42, S: 42, D: 42, R: 42, M: 42, P: 22}, {"earn": "Pest Control, Western Provinces hard diary"}),
    ("Elite void robe", {A: 42, S: 42, D: 42, R: 42, M: 42, P: 22}, {"earn": "Pest Control, Western Provinces hard diary"}),
    ("Ava's accumulator", {R: 50}, {"earn": "Animal Magnetism quest"}),
    ("Ava's assembler", {R: 70}, {"earn": "Dragon Slayer II and Vorkath's head"}),
    ("Dizana's quiver", {R: 75}, {"earn": "Fortis Colosseum"}),
    ("Amulet of power", {}),
    ("Necklace of anguish", {}),
    ("Zaryte vambraces", {R: 80, D: 45}),
    ("Pegasian boots", {R: 75, D: 75}),
    ("Archers ring", {}),
    ("Archers ring (i)", {}, {"build": [6733], "note": "Imbue an archers ring"}),
    ("Venator ring", {}),
    ("Twisted buckler", {R: 75, D: 75}),
    # Magic weapons
    ("Staff of fire", {}),
    ("Mystic fire staff", {M: 40, A: 40}),
    ("Ancient staff", {M: 50, A: 50}),
    ("Master wand", {M: 60}),
    ("Kodai wand", {M: 75}),
    ("Staff of the dead", {M: 75, A: 75}),
    ("Toxic staff of the dead", {M: 75, A: 75}, {"build": [11791, 12932], "note": "A staff of the dead and a magic fang"}),
    ("Warped sceptre", {M: 62}),
    ("Trident of the seas", {M: 75}),
    ("Trident of the swamp", {M: 75}, {"build": [11908, 12932], "note": "A trident and a magic fang"}),
    ("Sanguinesti staff", {M: 82}),
    ("Harmonised nightmare staff", {M: 80}, {"build": [24422, 24511], "note": "Nightmare staff and harmonised orb"}),
    ("Eye of ayak", {M: 85}),
    ("Tumeken's shadow", {M: 85}),
    # Magic armour
    ("Mystic hat", {M: 40, D: 20}),
    ("Mystic robe top", {M: 40, D: 20}),
    ("Mystic robe bottom", {M: 40, D: 20}),
    ("Ahrim's hood", {M: 70, D: 70}),
    ("Ahrim's robetop", {M: 70, D: 70}),
    ("Ahrim's robeskirt", {M: 70, D: 70}),
    ("Ancestral hat", {M: 75, D: 65}),
    ("Ancestral robe top", {M: 75, D: 65}),
    ("Ancestral robe bottom", {M: 75, D: 65}),
    ("Occult necklace", {M: 70}),
    ("Mage's book", {M: 60}, {"earn": "Mage Training Arena"}),
    ("Arcane spirit shield", {M: 65, D: 75, P: 70}),
    ("Imbued saradomin cape", {M: 75}, {"earn": "Mage Arena II"}),
    ("Saradomin cape", {M: 60}, {"earn": "Mage Arena"}),
    ("Tormented bracelet", {}),
    ("Mystic boots", {M: 40, D: 20}),
    ("Eternal boots", {M: 75, D: 75}),
    ("Seers ring (i)", {}, {"build": [6731], "note": "Imbue a seers ring"}),
    ("Magus ring", {}),
]

# Targets that only some attacks can hurt, by their label.
ONLY = {
    "Kraken": ["ranged", "magic"],
    "Kree'arra": ["ranged", "magic"],
    "Zulrah": ["ranged", "magic"],
    "Grotesque Guardians": ["melee"],
}

TARGETS = [
    # (group, name, version or None, label)
    ("Boss", "Vorkath", "Post-quest", None),
    ("Boss", "Zulrah", "Serpentine", "Zulrah"),
    ("Boss", "General Graardor", None, None),
    ("Boss", "Kree'arra", None, None),
    ("Boss", "Commander Zilyana", None, None),
    ("Boss", "K'ril Tsutsaroth", None, None),
    ("Boss", "Nex", None, None),
    ("Boss", "Corporeal Beast", None, None),
    ("Boss", "Cerberus", None, None),
    ("Boss", "Abyssal Sire", "Phase 1", "Abyssal Sire"),
    ("Boss", "Alchemical Hydra", "Serpentine", "Alchemical Hydra"),
    ("Boss", "Kraken", "Kraken", "Kraken"),
    ("Boss", "Thermonuclear smoke devil", None, None),
    ("Boss", "Dusk", "Second form", "Grotesque Guardians"),
    ("Boss", "Duke Sucellus", "Post-quest, Awake", "Duke Sucellus"),
    ("Boss", "The Leviathan", "Post-quest", "The Leviathan"),
    ("Boss", "The Whisperer", "Post-quest", "The Whisperer"),
    ("Boss", "Vardorvis", "Post-quest", "Vardorvis"),
    ("Boss", "Phantom Muspah", "Ranged", "Phantom Muspah"),
    ("Boss", "Sarachnis", None, None),
    ("Boss", "Kalphite Queen", "Crawling", "Kalphite Queen"),
    ("Boss", "Giant Mole", None, None),
    ("Boss", "Dagannoth Rex", None, None),
    ("Boss", "Dagannoth Prime", None, None),
    ("Boss", "Dagannoth Supreme", None, None),
    ("Boss", "TzTok-Jad", None, None),
    ("Boss", "Tormented Demon", "1", "Tormented Demon"),
    ("Boss", "Araxxor", None, None),
    ("Boss", "The Hueycoatl", "Normal", "The Hueycoatl"),
    ("Boss", "Scurrius", "Solo", "Scurrius"),
    ("Boss", "Amoxliatl", "Post-quest", "Amoxliatl"),
    ("Boss", "Skotizo", None, None),
    ("Boss", "Sol Heredit", None, None),
    ("Boss", "Yama", "Normal", "Yama"),
    ("Boss", "Callisto", None, None),
    ("Boss", "Venenatis", None, None),
    ("Boss", "Vet'ion", "Normal", "Vet'ion"),
    ("Boss", "Artio", None, None),
    ("Boss", "Spindel", None, None),
    ("Boss", "Scorpia", None, None),
    ("Boss", "Chaos Elemental", None, None),
    ("Boss", "Hespori", None, None),
    ("Slayer", "Abyssal demon", "Standard", "Abyssal demon"),
    ("Slayer", "Gargoyle", "Upstairs", "Gargoyle"),
    ("Slayer", "Dust devil", "Catacombs of Kourend", "Dust devil"),
    ("Slayer", "Nechryael", "Normal", "Nechryael"),
    ("Slayer", "Greater Nechryael", "Regular", "Greater Nechryael"),
    ("Slayer", "Smoke devil", None, None),
    ("Slayer", "Rune dragon", None, None),
    ("Slayer", "Adamant dragon", None, None),
    ("Slayer", "Brutal black dragon", None, None),
    ("Slayer", "Skeletal Wyvern", "1", "Skeletal Wyvern"),
    ("Slayer", "Black demon", "Level 178", "Black demon"),
    ("Slayer", "Demonic gorilla", "1", "Demonic gorilla"),
    ("Slayer", "Lizardman shaman", "Standard", "Lizardman shaman"),
    ("Slayer", "Wyrm", "Attacking", "Wyrm"),
    ("Slayer", "Drake", None, None),
    ("Slayer", "Hellhound", "Level 122", "Hellhound"),
    ("Slayer", "Basilisk Knight", None, None),
    ("Slayer", "Aberrant spectre", None, None),
    ("Slayer", "Bloodveld", "Normal", "Bloodveld"),
    ("Slayer", "Fire giant", "Level 86", "Fire giant"),
    ("Slayer", "Cave horror", "Worker", "Cave horror"),
    ("Slayer", "Kalphite Soldier", "Kalphite Lair", "Kalphite Soldier"),
    ("Slayer", "Ankou", "Level 82", "Ankou"),
    ("Training", "Ammonite Crab", None, None),
    ("Training", "Sand Crab", "Active", "Sand Crab"),
    ("Training", "Rock Crab", "Active", "Rock Crab"),
    ("Training", "Gemstone Crab", None, None),
    ("Training", "Experiment", "Level 51", "Experiment"),
    ("Training", "Hill Giant", None, None),
    ("Training", "Moss giant", "Level 42", "Moss giant"),
]

ATTRIBUTES = {"dragon", "demon", "undead", "kalphite", "leafy", "vampyre1", "vampyre2", "vampyre3", "golem", "rat",
              "shade", "spectral", "xerician", "fiery", "penance"}


def preferred(entries):
    def rank(e):
        v = e["version"]
        return VERSION_PREFERENCE.index(v) if v in VERSION_PREFERENCE else len(VERSION_PREFERENCE)
    return sorted(entries, key=rank)[0]


def read_styles(calc):
    """The attack styles each weapon category offers, as (attack type, stance) pairs."""
    names = dict(re.findall(r"(\w+)\s*=\s*'([^']*)'", open(os.path.join(calc, "src/enums/EquipmentCategory.ts")).read()))
    src = open(os.path.join(calc, "src/utils.ts")).read()
    body = src[src.index("export const getCombatStylesForCategory"):]
    body = body[:body.index("\n};")]
    styles = {}
    for m in re.finditer(r"((?:\s*case EquipmentCategory\.\w+:)+)(.*?)(?=case EquipmentCategory|default:)", body, re.S):
        pairs = [[t, s] for _, t, _, s in re.findall(r"type: (null|'(\w+)'), stance: (null|'([^']+)')", m.group(2)) if t]
        for c in re.findall(r"EquipmentCategory\.(\w+)", m.group(1)):
            styles[names.get(c, c)] = pairs
    return styles


def read_ammo(calc):
    """Which ammunition each ranged weapon fires. An empty list means it fires none."""
    src = open(os.path.join(calc, "src/lib/Equipment.ts")).read()
    sets = {}
    common = src[src.index("const commonAmmoCategories"):src.index("const ammoForRangedWeapons")]

    def resolve(text):
        ids = []
        for spread in re.findall(r"\.\.\.ret\.(\w+)", text):
            ids += sets[spread]
        ids += [int(x) for x in re.findall(r"(?<![\w.])(\d+)(?![\w])", re.sub(r"//.*", "", re.sub(r"\.\.\.ret\.\w+", "", text)))]
        return ids

    for name, body in re.findall(r"(\w+): \[(.*?)\]", common.split("};")[0], re.S):
        sets[name] = resolve(body)
    for name, body in re.findall(r"ret\.(\w+) = \[(.*?)\];", common, re.S):
        sets[name] = resolve(body)
    table = src[src.index("const ammoForRangedWeapons"):]
    table = table[:table.index("\n};")]
    weapons = {}
    for wid, body in re.findall(r"^\s*(\d+): (.*?),\s*//", table, re.M):
        if body.startswith("commonAmmoCategories()."):
            weapons[int(wid)] = sets[body.split(".")[1]]
        else:
            weapons[int(wid)] = resolve(body.strip("[]"))
    return weapons


def main():
    calc, out = sys.argv[1], sys.argv[2]
    equipment = json.load(open(os.path.join(calc, "cdn/json/equipment.json")))
    monsters = json.load(open(os.path.join(calc, "cdn/json/monsters.json")))
    aliases = json.load(open(os.path.join(calc, "cdn/json/equipment_aliases.json")))
    styles = read_styles(calc)
    ammo = read_ammo(calc)

    groups = {}
    for e in equipment:
        if e["slot"] not in SLOTS:
            continue
        groups.setdefault(e["name"].lower(), []).append(e)

    by_id = {}
    for name, entries in groups.items():
        for e in entries:
            by_id[e["id"]] = name

    catalogue_names = {c[0].lower() for c in CATALOGUE}
    missing = [c[0] for c in CATALOGUE if c[0].lower() not in groups]
    if missing:
        sys.exit("Catalogue names not in the equipment table: " + ", ".join(missing))

    gear = []
    index_of = {}
    ammo_sets = {}
    for name in sorted(groups):
        entries = groups[name]
        e = preferred(entries)
        o = e["offensive"]
        b = e["bonuses"]
        offence = [o["stab"], o["slash"], o["crush"], o["magic"], o["ranged"]]
        power = [b["str"], b["ranged_str"], b["magic_str"]]
        useful = any(v > 0 for v in offence + power) or name in catalogue_names
        if not useful or e["slot"] == "weapon" and e["category"] == "Unarmed" and name not in catalogue_names:
            continue
        ids = sorted({x["id"] for x in entries})
        item = {
            "id": e["id"],
            "name": e["name"],
            "slot": SLOTS.index(e["slot"]),
            "o": offence,
            "p": power,
            "pr": b["prayer"],
        }
        if len(ids) > 1:
            item["ids"] = ids
        if e["slot"] == "weapon":
            item["cat"] = e["category"]
            item["spd"] = e["speed"]
            if e["isTwoHanded"]:
                item["two"] = True
            fires = None
            for x in entries:
                if x["id"] in ammo:
                    fires = ammo[x["id"]]
            if fires is not None:
                key = ",".join(map(str, sorted(set(fires))))
                item["am"] = ammo_sets.setdefault(key, len(ammo_sets))
        index_of[name] = len(gear)
        gear.append(item)

    # Ornament kits and cosmetic recolours point at their base item.
    for variant, base in aliases.items():
        base_name = by_id.get(base)
        if base_name in index_of:
            item = gear[index_of[base_name]]
            ids = set(item.get("ids", [item["id"]]))
            ids.add(int(variant))
            item["ids"] = sorted(ids)

    catalogue = []
    for row in CATALOGUE:
        name, req = row[0], row[1]
        extra = row[2] if len(row) > 2 else {}
        entry = {"name": groups[name.lower()][0]["name"]}
        if req:
            entry["req"] = req
        entry.update(extra)
        catalogue.append(entry)

    targets = []
    for group, name, version, label in TARGETS:
        found = [m for m in monsters if m["name"] == name and (version is None or m["version"] == version)]
        if not found:
            sys.exit("Monster not found: %s (%s)" % (name, version))
        m = found[0]
        d = m["defensive"]
        sk = m["skills"]
        targets.append({
            "id": m["id"],
            "name": label or name,
            "group": group,
            "hp": sk["hp"],
            "def": sk["def"],
            "mag": sk["magic"],
            "mb": m["offensive"]["magic"],
            "d": [d["stab"], d["slash"], d["crush"], d["magic"], d["light"], d["standard"], d["heavy"]],
            "size": m["size"],
            "attr": sorted(a for a in m["attributes"] if a in ATTRIBUTES),
            "slayer": bool(m.get("is_slayer_monster")),
            "weak": [m["weakness"]["element"], m["weakness"]["severity"]] if m.get("weakness") else None,
        })
        if (label or name) in ONLY:
            targets[-1]["only"] = ONLY[label or name]

    ammo_lists = [None] * len(ammo_sets)
    for key, i in ammo_sets.items():
        ammo_lists[i] = [int(x) for x in key.split(",")] if key else []
    data = {"styles": styles, "ammo": ammo_lists, "gear": gear, "catalogue": catalogue, "targets": targets}
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w") as f:
        json.dump(data, f, separators=(",", ":"), sort_keys=True)
    print("%d gear, %d catalogue, %d targets, %d bytes" % (len(gear), len(catalogue), len(targets), os.path.getsize(out)))


if __name__ == "__main__":
    main()
