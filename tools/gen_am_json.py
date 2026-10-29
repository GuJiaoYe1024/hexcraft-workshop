# -*- coding: utf-8 -*-
"""v0.6.22 Alex's Mobs 联动：批量生成 hextech / equipment JSON"""
import json, os

HEX_DIR = r"H:\MC1314\MOD\src\main\resources\data\hexworkshop\hextech"
EQUIP_DIR = r"H:\MC1314\MOD\src\main\resources\data\hexworkshop\equipment"

# id -> (rarity, weight, event)
HEX = {
    # 白银 ×6
    "hex_am_mob_damage":  ("SILVER", 10, "am_mob_damage:0.06"),
    "hex_am_mob_resist":  ("SILVER", 10, "am_mob_resist:0.06"),
    "hex_am_beast_damage":("SILVER", 8,  "am_beast_damage:0.12"),
    "hex_am_venom_damage":("SILVER", 8,  "am_venom_damage:0.12"),
    "hex_am_flying_damage":("SILVER", 8, "am_flying_damage:0.12"),
    "hex_am_aquatic_damage":("SILVER", 8,"am_aquatic_damage:0.12"),
    # 黄金 ×6
    "hex_am_titan_damage": ("GOLD", 7,  "am_titan_damage:0.18"),
    "hex_am_boss_damage":  ("GOLD", 6,  "am_boss_damage:0.2"),
    "hex_am_kill_heal":    ("GOLD", 8,  "am_kill_heal:2"),
    "hex_am_kill_buff":    ("GOLD", 8,  "am_kill_buff:1"),
    "hex_am_kill_speed":   ("GOLD", 7,  "am_kill_speed:1"),
    "hex_am_mob_master":   ("GOLD", 6,  "am_mob_master:0.12"),
    # 棱彩 ×4
    "hex_am_apex":      ("PRISMATIC", 5, "am_apex:0.4,am_totem:0.2"),
    "hex_am_wild_lord": ("PRISMATIC", 5, "am_wild_lord:0.25,am_kill_heal:3"),
    "hex_am_predator":  ("PRISMATIC", 5, "am_predator:0.2,am_kill_heal:2"),
    "hex_am_totem":     ("PRISMATIC", 5, "am_totem:0.3,am_mob_damage:0.1"),
}

EQUIP = {
    # id -> (rarity, weight, icon, sell, mods, event)
    "equip_am_beastbone": ("GOLD", 10, "\u2767", 625,
                           [{"perLevel": 2, "attr": "attack_damage"}, {"perLevel": 25, "attr": "max_health"}],
                           "am_beast_damage:0.1,am_titan_damage:0.1"),
    "equip_am_wild_totem": ("GOLD", 10, "\u2625", 700,
                            [{"perLevel": 4, "attr": "armor"}, {"perLevel": 2, "attr": "attack_damage"}],
                            "am_mob_damage:0.15,am_totem:0.08"),
}

def main():
    for hid, (rarity, weight, event) in HEX.items():
        d = {"rarity": rarity, "weight": weight, "source": "alexsmobs", "event": event}
        p = os.path.join(HEX_DIR, hid + ".json")
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(d, ensure_ascii=False, indent=4))
        print("hex ok", hid)
    for eid, (rarity, weight, icon, sell, mods, event) in EQUIP.items():
        d = {"rarity": rarity, "weight": weight, "icon": icon, "sell": sell,
             "source": "alexsmobs", "mods": mods, "event": event}
        p = os.path.join(EQUIP_DIR, eid + ".json")
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(d, ensure_ascii=False, indent=4))
        print("equip ok", eid)

if __name__ == "__main__":
    main()
