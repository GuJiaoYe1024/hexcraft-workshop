Hexcraft Workshop v0.6.6 - Equipment rebalance (all 44 items buffed) + full hex system audit
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated. (This release note also covers the v0.6.5 update, since only v0.6.6 is being published.)

- EQUIPMENT REBALANCE (44 items, all buffed): previously a 2500/4000 crystal roll often gave stats weaker than forging. New target: Gold-tier items ≈ 4 forge-roll strength, Prismatic-tier ≈ 6
  - Pure-mechanic items now grant base stats too (Hextech Gunblade +4 ATK, Liandry's +3 ATK, Mortal Reminder +4 ATK, TACZ Barrel +3 ATK, etc.)
  - Attack items raised to +4~+5 (many were +1~+2), armor +8~+16, HP +16~+30, attack speed +20%~+45%
  - Shield / lifesteal / splash / thorns / execute / crit mechanics scaled up together
  - Fixed Gold/Prismatic inversion (Gold Randuin used to out-stat Prismatic Gargoyle)
  - Draw prices and 25% sell-back unchanged; all 44 item descriptions rewritten (EN/CN)
- HEX SYSTEM FULL AUDIT PASSED: all 184 hexes + 44 items + 40 world hexes - JSON fields OK, event keys match code consumption, translations complete, no numeric anomalies; no hex bugs needed fixing this round (v0.6.3~0.6.5 known issues all fixed)
- SELF-CHECK: 976 bilingual keys aligned, 0 leftover design labels, protocol VERSION 20 unchanged (same as v0.6.4/v0.6.5 - everyone just updates to v0.6.6)

Included from v0.6.5:
- FIXED (major bug): world hex draw no longer charges 2000 XP before saying "you own all" - pool is checked first, no charge when empty or when XP is insufficient
- NEW: "Owned Lv.X" golden badge on hex/equipment 3-choice cards (forge rolls and world hex draws excluded)
- NEW: Equipment Codex button on the equipment page - browse all 44 items, scroll, hover for full details; owned items flagged
- NEW: Forge tier multiplier hint (x1/x2/x3) shown next to each tier title, updates with config changes
- NEW: Random Gift low-value filter config (server-side, default OFF = original full item pool; ON filters vanilla building/decor items like dirt/cobblestone/wood/wool)

Also included from v0.6.4 (previously unpublished): multiplayer hex screen showing empty on clients fixed, ammo count capped at 9999 (no more -1.6B overflow), random gift text calibration.

Install: replace the jar in the mods folder with hexworkshop-1.20.1-0.6.6.jar. Multiplayer: everyone on v0.6.6 (protocol VERSION 20). Old saves work fine - item stats apply instantly, no new world needed.

Hexcraft Workshop v0.6.3 - Stay-open toggle + live cost refresh + multiplayer sync + content review fixes
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- NEW CLIENT CONFIG keepWorkshopOpen (default ON): after a roll (hex draw / forge / equip / world hex), return to the workshop entry page to keep drawing, or close the UI entirely
- NEW CLIENT CONFIG confirmSelect (default ON): click a card to select it, then press "Confirm" to actually take it (prevents misclicks); turn it OFF for instant select (old behavior)
- LIVE COST REFRESH: draw/forge/equip/world costs refresh in the UI as discounts change (server-authoritative; min 10% price floor, world draw cost floor of 1 XP)
- MULTIPLAYER SYNC HARDENING: opening the world hex screen requests fresh data, every roll/buy/cancel pushes a sync packet; re-entrance guard prevents duplicate charges; protocol VERSION 19
- FIXED (major bug): cancelling a 3-choice roll no longer deadlocks with "please finish the current choice" - new C2SCancelRoll packet clears pendingRoll / worldDrawOptions on the server; the cancel button and Esc both apply the keepWorkshopOpen rule. Fees are NOT refunded: cancel = abandon the roll, which prevents free infinite re-roll previews
- FIXED: world hex draw cost sign bug - Genesis Contract & Peaceful Vision stored draw_cost as +0.45 (treated as +45% increase) while the formula is cost x (1+sum); changed to -0.55 so cost is REDUCED as described (Genesis Contract alone: 2000 -> 900 XP); stacking reductions have a floor of 1 XP (no refunds / no exploitation)
- FIXED: 30 tooltip/stat mismatches (percentages scaled wrongly, missing numbers, outdated leftovers), 22 mechanic/stat conflicts (Dragonhorn 100%->75%, Void Shipment bleed etc), 14 unit inconsistencies (regen unified as "per 5 seconds", kill_heal in HP points, arcane_shield x100%, TACZ ammo/gun damage normalized)
- CLEANED: 10 hex descriptions no longer show design annotations like "Gold: value+mechanism"
- REMOVED: the redundant "Complete" button after a roll - selecting/confirming now leaves the screen immediately
- OPTIMIZATIONS: Zeal ASPD 80%->40%, Night Digger / Dig Master mining speed 120%->60%, Dragon Heart +3HP->+6 & thorns 1->2, War Cry / Frost Crown / Field Medic prismatic weight 8->6
- Content total: 184 hexes + 44 equipment + 13 forge stats + 40 world hexes (281 items); zh_cn & en_us 972 keys aligned
- Protocol VERSION 19: v0.6.2 and earlier clients must update to match

Hexcraft Workshop v0.6.2 - Forge template tooltips + 6 events live + stacking tag system + DLC pack system
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- FORGE TOOLTIP TEMPLATES: the 13 static forge traits now render real values x tier multiplier in-game via {tierName} {displayValue} templates - "Silver: +1.0 armor/level", "Gold: +2.0", "Prismatic: +15% HP/level"; tooltip follows Cloth multiplier changes instantly
- 6 RESERVED EVENTS NOW LIVE: bleed (damage over time, independent timers, stackable), hit slow (4s, 15% strength/level, max 3), thorns fire (ignite attacker 4s), bleed spread (2.5 blocks on death), mark target (3.5s, damage taken up), execute (below 20% HP threshold -> 35% max-HP magic damage)
- STACKING TAG SYSTEM (stackTag): 12 unified tag labels (permanent/combat/timed/on_hit/on_kill/on_hurt/on_tick + damage/armor/speed/leech/shield), tooltip auto-shows 【Stack: xxx｜xxx】 - new hexes only need the stackTag array in JSON
- 14 NEW STACKING HEXES (Silver 4 / Gold 6 / Prismatic 4): Charge Dash, Bulwark, Bloodied Armor, Pain-Heal Quick, Blood Pact Inheritance, Battle Shield, War Soul, Time-Burning Blade, Wound Leech, Run to Eternity, Eternal Campaign, Undying Shell, Life-Death Interweave, Instant Vastness; 8 old stacking hexes got their tags
- DLC PACK SYSTEM: drop third-party content packs into hexworkshop_dlc/packs (folder or zip, must contain dlc_meta.json); enable via Cloth config page or /hexcraft reload dlc; zip loads in memory (nothing extracted to disk); data dirs data/hex, data/equipment, data/world_hex
- CLOTH CONFIG: v060 group retitled; 2 new server configs - permanent stack global multiplier (0.1~10, default 1.0) and global cap (10~10000, default 200); server-authoritative (client changes ignored)
- TEXT POLISH: 40 world hexes + TACZ hexes clarified (negative effects marked, close/long range gun damage, gun crit); Heartsteel calibrated to hit-stack wording (25% chance, +0.15% max HP per stack, 2 thorns)
- 4 NEW WORLD HEXES: Eternal Multiplication (prismatic, permanent stacks +50%), War Wane (gold, combat decay +50%), Long War (gold, combat decay -30%), Twinkle (gold, timed stack duration -50%)
- Content total: 184 hexes + 44 equipment + 13 forge stats + 40 world hexes (281 items); zh_cn & en_us 960 keys
- Protocol unchanged (VERSION=16) - v0.6.0/v0.6.1 players can update directly

Hexcraft Workshop v0.6.1 - Forge multiplier config + Cloth v060 fix + 12 new hexes
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- FORGE TOOLTIP SHOWS REAL VALUES: hovering a forge trait now appends the actual gain in brackets (base value x tier multiplier, runtime-composed, both languages) - "Strength (+0.5/level) [Silver]", "Strength (+1.0/level) [Gold]", "Strength (+1.5/level) [Prismatic]", "Vitality Surge (+15%/level) [Prismatic]" - instantly verify the multiplier is applied
- FORGE MULTIPLIERS NOW CONFIGURABLE (3 new v060 configs): forgeMultiplierSilver=1.0 / forgeMultiplierGold=2.0 / forgeMultiplierPrismatic=3.0 shown in the Cloth config screen (e.g. set 1/2/4 for a fun pack); SERVER-AUTHORITATIVE - client-side changes do not apply (anti-cheat); tooltip follows the runtime value
- PERCENT STATS NOW TIER-SCALED: with the single shared pool, HPPct/ASPDPct now also multiply by tier (+5/10/15% HP per level, +3/6/9% ASPD per level) - a Silver-drawn percent stat no longer equals Prismatic; tooltip = real effect
- CLOTH v060 GROUP FIXED: the 4 v060 configs (extra armor DR threshold/rate/cap, duplicate rule) existed only in toml without UI nodes; now mounted on the config tree (plus the 3 new multipliers), also visible to third-party config tools; user values kept
- 12 NEW HEXES (all reuse existing event keys, zero new event code, datapack-overridable): Silver "Flesh Rend / Sluggish Pursuit / Mining Tenacity / Small Shield"; Gold "Burning Rebound / Fatal Wound / Soul Siphon / Rock Body / Hunter's Mark"; Prismatic "Bleed Avatar / Eternal Bulwark / Mark Judgement" - 6 brand-new event keys (bleed_damage/slow_on_hit/thorns_fire/bleed_spread/mark_target/mark_execute) have no handler yet; per design they are reserved JSON only, desc annotated "pending", no effect, no crash
- Content total: 170 hexes + 44 equipment + 13 forge stats + 36 world hexes (263 items); zh_cn & en_us complete (908 keys)
- Protocol unchanged (VERSION=16) - v0.6.0 players can update directly; all v0.6.0 confirmed fixes untouched

Hexcraft Workshop v0.6.0 - Full-system refactor: universal stack manager + forge rework + datapack reload
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- UNIVERSAL STACK MANAGER (framework): new stack system (key/cap/combat/out-of-combat decay/death-clear/world modifiers). Hex JSON can declare an optional `stack` object; combat stacks clear on death and decay per second out of combat (affected by the "Scattered Offensive" world hex decay bonus); the Attributes page shows a new "Battle Stacks" section (value/cap live)
- CRESCENDO BLADE MOVED FROM FORGE INTO HEXES: forge no longer drops mechanic aspects; Crescendo Blade (prismatic, 200 stack cap, combat stack with decay) plus Dance/Bloodlust/Reaper/Lightning/Fire/Poison/Pierce blades all became hexes (10 new: Gold x6 + Prismatic x5)
- SWIFT HORN DUAL VARIANTS + RESONANCE EGG: Swift Horn - Quick (+100% stack gain) / Swift Horn - Thick (+100% stack cap); Easter egg "Resonance" - holding both Crescendo Blade and Blade Dance, attack-speed overflow bonus damage x min(2, stacks/100)
- FORGE REWORK (tier-honest): all dynamic mechanic aspects removed (moved into hexes); single static attribute pool of 13 - Silver 9 (Atk/Armor/HP/Speed/Aspd/Luck/Knockback/Resist/Tough, x1), Gold 11 (+Reach/Step, x2), Prismatic 13 (+Vitality Surge +5%HP/level, +Alacrity Surge +3%ASPD/level, x3). "Prismatic best > Gold middle > Silver worst" is now guaranteed by tier multiplier + high-tier exclusive stats
- ATTRIBUTE CEILING & EXTRA MULTIPLIER: modifier path naturally bypasses vanilla maxValue (maxValue only clamps baseValue), unlimited forging is not capped; new config - extra armor damage reduction independent multiplier (armor above threshold: X% DR per point, capped Y%) and duplicate rule switch (FORGE_DUPLICATE_RULE: STACK unlimited / BEST_ONLY upgrade-refund)
- FOUR DATAPACKS OPEN + HOT RELOAD: hexes/forging/equipment/world hexes all overridable via datapack; new `/hexcraft reload all|hexes|forging|equipment|world` module hot-reload (server-side instant, client UI auto-refresh)
- UI AUTO-REFRESH: new S2CDataRefresh broadcast (protocol VERSION 15->16); the four main screens rebuild automatically after reload/rule changes
- CLIENT-LOBBY COMPAT: when the server lacks this mod, H/K hotkeys and inventory tabs auto-hide with a "server missing this mod" message - no crash
- WORLD HEXES DATAPACK + 4 NEW MECHANIC ONES: Scattered Offensive (Gold: +100% battle-stack out-of-combat decay, +15% kill XP), Blunt Weapons (Gold: -15% player damage dealt, +20% coins from hostile kills), Eternal Growth (Prismatic: +50% permanent stack gain, -30% world-hex draw XP cost), Thick Battlefield (Prismatic: +50% battle stack cap, +30% hostile mob HP)
- NEW CONFIG: v060 section (extra armor DR threshold/rate/cap + duplicate rule), renders fine in the Cloth config screen
- Content total: 158 hexes + 44 equipment + 13 forge stats + 36 world hexes (251 items); all JSON parses, zh_cn & en_us complete (874 keys)
- Protocol VERSION=16 (new S2CDataRefresh) - v0.5.0 players must update to the same version
Hexcraft Workshop v0.5.0 - Content overhaul: 3-tier forge pools + 10 new hexes + save sanitization
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- FIXED Stack Drake showing in the growth page: Stack Drake never stacks itself (it only amplifies other stack effects), so it is excluded from the "Growth Stacks" panel (no more "x0 stacks")
- FORGE OVERHAUL - single shared static stat pool (13 aspects): Atk/Armor/HP/Speed/Aspd/Luck/Knockback/Resist/Tough + Reach/Step + HPPct (+5% HP/level) + ASPDPct (+3% ASPD/level). All three tiers draw from the SAME pool - no exclusive aspects; higher-tier words (Reach/Step/HPPct/ASPDPct) are weighted higher in Gold/Prismatic and weighted low (but still obtainable) in Silver. **TIER STRENGTH**: every aspect value is multiplied by the forge tier (Silver x1 / Gold x2 / Prismatic x3) - the same aspect is genuinely 3x stronger from a Prismatic forge, so Prismatic results match its "best" tier
- **BASE STATS BUFFED**: Atk 0.25->0.5, Armor 0.5->1.0, HP 1.0->2.0 per level; Prismatic percentage stats buffed (Vitality +2%->+5%, Alacrity +1.5%->+3% per level) - forge strength now matches the tier prices
- **CLEARER PROMPTS**: random-gift and stack prompts reworded ("[Random Gift] Got XX xN!") - how many stacks / what you got is now obvious at a glance
- FIXED two extra-damage infinite recursion (Pierce Blade / Reaper Blade resolved magic damage without the re-entry lock - holding them could re-trigger on-hit effects in a loop) - now routed through the locked extra-damage channel
- EQUIPMENT DEDUP: Randuin's Omen duplicated Frozen Heart (both frost_bite:0.3) - Randuin now focuses on high armor/HP + thorn reflect (thorns:1.5), clearly distinct from Frozen Heart's frost slow
- 10 NEW HEXES (Silver = pure numbers, Gold = numbers+mechanics, Prismatic = percentages+mechanics): Silver "Armor Core / Lucky Dice / Swift Feet"; Gold "Critical Leech / Frosty Fortune / Echo Rampage / Executioner's Gold"; Prismatic "Prismatic Immortal / Full-HP Blade (25% crit + 50% crit damage at full HP) / Lucky Streak". All reuse existing effect keys - zero new event code, lowest risk
- OLD SAVES AUTO-UPDATE: save sanitization on load (login / respawn copy) - hexes/forge stats/equipment whose IDs were removed are dropped automatically (with a log line), so old saves never touch empty definitions; also cleaned one redundant registry lookup in the page screen
- Content total: 148 hexes + 44 equipment (192 items); all JSON parses, zh_cn & en_us translations complete
- Protocol stays VERSION=15 - v0.4.15 players can upgrade directly

Hexcraft Workshop v0.4.15 - XP recognition fix: accepts experience granted by other mods (FTB Quests etc.)
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- FIXED draw not recognizing external XP: hex draw & world-hex draw checked the cached totalExperience field, which desyncs when mods (FTB Quests, commands, XP injection) set the player's level directly - players with plenty of XP got "not enough XP". Now the real total XP is recomputed from the level formula every time, so XP from ANY source counts - if you have the XP, you can roll
- Cost is still deducted via giveExperiencePoints(-cost), syncing the cache from the real level
- Pure server-side logic change, protocol stays VERSION=15 - v0.4.14 players can upgrade directly

Hexcraft Workshop v0.4.14 - Full persistence: data survives logout/login + final full self-check
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- **New full persistence (fixes losing data on logout/re-login)**: previously PlayerData (hexes/forges/equips/coins) only lived in runtime memory and was lost on logout or world rejoin (only a death backup could restore it). v0.4.14 adds four persistence channels: 1) full snapshot written on death; 2) snapshot auto-written before the player data is saved to disk (SaveToFile); 3) auto-restore on file load when the capability is empty (LoadFromFile); 4) on login, restore the death backup first, then the full snapshot - data survives logout/login and death
- **Final full self-check (all passed)**: all 38 HexConfig options have an application point (0 "registered but not applied"; PARTICLES_ENABLED is a display/reserved item); all 4 gamerules (keep hexes/forges/equips/coins) are referenced in onKill decision, onLogin auto-apply and Packets UI sync; four restore channels with logs (respawn/login/file-load)
- **Multiplayer hardening**: C2SSetGameRule now has a permission guard - normal players on a dedicated server cannot change the server death-keep rules (host/OP only; unauthorized requests are rejected and logged); the config screen shows a new multiplayer note ("server rules are set by the host/admin"); all v0.4.12-0.4.14 additions (login auto-apply/death backup/two-channel respawn/file persistence) are pure server-side logic and are skipped safely on dedicated servers without a client config
- Death-keep chain unchanged: client config OR server gamerule - whichever enables it wins; config screen set & saved from the main menu; protocol stays VERSION=15 - v0.4.13 players can upgrade directly

Hexcraft Workshop v0.4.13 - Death-keep fully fixed: two-channel respawn recovery (persistentData backup + capability copyFrom fallback)
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- **Root cause found (you tested: keep decision was all-true but hexes/forges/equips/coins still emptied on death)**: the respawn data copy channel was broken - PlayerEvent.Clone only relied on capability copyFrom, but the old entity capability can already be invalidated during the death/respawn flow (returns null), the copy was skipped and the new entity spawned empty
- **Fix (two-channel recovery)**: 1) on death, once the keep decision passes, the full PlayerData is backed up into the player's persistentData (saved with the player NBT and auto-copied on vanilla respawn); 2) on Clone, restore from the persistentData backup first, and additionally copyFrom when the old capability is still valid
- **Login fallback**: on login, if the capability is empty but a persistentData backup exists, it is restored automatically - data survives logout/login after a death
- **New recovery diagnostic log**: [Hexcraft] player respawn restore: way=persistentData-backup/copyFrom/NONE hexes=N forges=N equips=N coins=N - success and channel are visible at a glance
- Existing chain unchanged: client config OR server gamerule - whichever enables it wins; config screen set & saved from the main menu; protocol stays VERSION=15 - v0.4.12 players can upgrade directly

Hexcraft Workshop v0.4.12 - Death-keep double safety (auto-apply client config to gamerule + diagnostic log)
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- On joining a world, the client death-keep config (Hexes/Forges/Equips/Coins) is now automatically applied to the server gamerule (set only, never clears) - as long as the client config is enabled, the gamerule becomes true on login
- A diagnostic log line is printed on player death ([Hexcraft] player XXX death: gamerule=... client-config=... -> actual keep=...) to make the actual read values visible in logs/latest.log
- Existing chain unchanged: client config OR server gamerule - whichever enables it wins; config screen can be set & saved from the main menu (no network, no crash); Create-World screen and /gamerule still fully work
- Network protocol stays VERSION=15 - v0.4.11 players can upgrade directly

Hexcraft Workshop v0.4.11 - Fixed Cloth Config crash + death-keep rules are now client-config-first
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- **Fixed Cloth Config screen crash (crash report crash-2026-10-22_12.03.08)**: clicking the death-keep toggles on the "Game Rules" tab while NOT in a world (main menu / before world creation) threw an NPE inside sendToServer because Minecraft.getConnection() was null - the whole Cloth Config screen crashed, and the change never reached the server anyway
- **Death-keep rules are now CLIENT-CONFIG-FIRST (core change)**: the four toggles (Hexes / Forges / Equips / Coins) now write to the client-local config hexworkshop-client.toml - they can be set and saved from the main menu, work without any network connection, never crash, and apply to every world you join; while in a world, toggling also pushes the server gamerule (instant effect, syncs for multiplayer)
- **Combined death-keep semantics**: client config OR server gamerule - whichever enables it, you keep it. Rules set via the Create-World screen or /gamerule in-game are still fully honored
- **Verified the world-creation path**: gamerules set on the Create-World screen are correctly written into the save's level.dat (hexworkshopKeepHexes/Forges/Equips/Coins=true confirmed present in a fresh save); the vanilla transfer works - the earlier "not working" was mostly caused by the main-menu crash discarding your changes / the old stale-snapshot display
- **Network protocol stays VERSION=15** (client display/config layer only, no structural change) - v0.4.10 players can upgrade directly

Hexcraft Workshop v0.4.10 - Fixed "death-keep game rules not working": UI display desynced from real server gamerule + toggle now sets value directly
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- **Root cause 1 (UI illusion)**: the four toggles under the "Game Rules" tab (keep Hexes / Forges / Equips / Coins on death) displayed a class-load-time static snapshot, while the server-side gamerule changes when the world changes / the server restarts / another player edits it - the UI could show "on" while the server was actually "off", so death still wiped everything, looking like "enabled but not working"
- **Root cause 2 (toggle semantics)**: clicking sent a "flip" request (invert the server's current value); when UI and server were desynced, one click flipped the wrong way
- **Fix**: the UI now reads the latest server-synced value when opened (no more stale snapshot); clicking sends a "set target value" request (true=keep / false=wipe on death) - UI display and server state are now one-to-one and take effect immediately
- **Network protocol VERSION 14->15** (rule packet structure changed; all clients must update to v0.4.10 to play together)

Hexcraft Workshop v0.4.9 - Demon Contract rework (debuffs scale but capped) + Full code audit + Content table update
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- **Demon Contract (hostile World Hex) debuff rework (per your confirmed design)**: the Weakness/Slowness applied to players can now scale with contract strength (Weakness I/II, Slowness II/III - no longer pinned at level I); but the trigger chance is reduced (about 40% discount, capped at 40%); duration stacking is capped (7s per proc, total capped at 10s - no more infinite refresh extension)
- **Full code audit (nothing registered-but-unused)**: all 138 player hex JSONs (all 80 event keys have a settlement point, including special keys sell_bonus / exchange_bonus / trade_discount / hex·forge·equip_power / stack_attr / transform), all 32 World Hexes (all 34 event keys settled in WorldHexEvents), all 44 equipment pieces (all 27 event keys applied), all 32 forge aspects (65 EN/ZH translation pairs complete), all 4 game rules (keepHexes / keepForges / keepEquips / keepCoins all referenced in death handling)
- **Content master table fully updated**: player hexes 124 -> 138, World Hexes 32 (v0.4.7 tier numbers), equipment 44, forge aspects 32
- World Hex draw animation (kept from v0.4.8): fade-in cards + draw progress hint on the 3-choice screen
- Effect-logic/client-layer only: network protocol stays VERSION=14, v0.4.8 players can upgrade directly

Hexcraft Workshop v0.4.8 - Player Hex Gambling System: Transform-into-random hexes + Random Gift text fix + New pool entries
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- **New gambling/random player hexes (transform on pick, Arena-of-Impact style)**:
  - "Bet: Rise" (Silver): on pick, instantly transform into 1 random Gold hex - the gamble is the randomness: god-tier or bottom-tier
  - "Bet: Prismatic" (Gold): on pick, instantly transform into 1 random Prismatic hex
  - "Bet: Double" (Prismatic): on pick, instantly gain 2 random hexes (each rarity rolled independently - double the gamble)
- **Random Gift text fixed & polished**: kill message now shows the item count ("Random gift: Iron Ingot x2")
- **All 29 English raw-format descriptions rewritten**: hover tooltips used to show raw mechanic code (e.g. "random_loot 1"); now natural English descriptions aligned with the Chinese ones
- **3 new regular player hexes**: Lucky Clover (Silver, 25% bonus emeralds on kill), Shield Bash (Gold, 3 shield at low HP + 15% reflect), Omni Drain (Prismatic, 30% lifesteal + 15% echo + 15% vamp rage at low HP)
- Player pool now 138 hexes (+6); ZH/EN translations updated in sync (810 keys each)
- Content/effect-definition layer only: network protocol stays VERSION=14, v0.4.7 players can upgrade directly

Hexcraft Workshop v0.4.7 - World Hex Number Rework: Silver=Flat / Gold=Flat+Small % / Prismatic=Percentage & Multipliers + Damage Layering + New Prismatic Hex
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- **World Hex number design reworked (per your confirmed design)**: Silver = pure flat numbers (fixed points like +6 max health / +3 attack / flat XP / flat coin tax, no percentages); Gold = flat number + a small percentage (<=20%); Prismatic = percentage / multiplier numbers (percentage max health, percentage attack, percentage speed or XP multipliers, 30%+ and up to x2/x3)
- **New percentage effect keys (independent multiplier, independent UUID modifiers)**: mob percentage health/attack `hostile/neutral/passive_hp_pct, atk_pct` (MULTIPLY_TOTAL) and player percentage health `player_hp_pct` - Prismatic hexes now bring real percentage shifts like "+40% max health", "+30% attack"
- **New silver flat keys**: `player_xp_flat` (flat XP per hostile kill) and `player_coin_flat` (flat coin tax per kill), so the Silver tier is truly "flat numbers"
- **All 32 World Hexes re-numbered to the new tiers**: ids/categories/rarities unchanged, numbers and descriptions rewritten (table below)
- **Reflect damage fixed (damage layering, Arena-style)**: thorns / reflect-forge / counter damage is now "pure reflect" by default - it no longer triggers lifesteal, crits, damage boosts, forge aspects, enchantments, stacking or on-hit rewards
- **New Prismatic player hex "Omni-Damage Edge" (all_damage_proc)**: all damage attributed to you (including thorns/reflect) triggers every on-hit effect (lifesteal, crit, stacking, forge aspects, enchantments) - want reflect to stack Heartsteel again? Draw this hex
- ZH/EN translations updated in sync: all 32 World Hex descriptions rewritten plus the new hex's name/desc (795 keys each)
- Content/effect-definition layer only: network protocol stays VERSION=14, v0.4.6 players can upgrade directly

World Hex design table (v0.4.7):
- General: Silver XP Affinity = +3 flat XP per hostile kill; Silver Coin Tax = -3 flat coins per kill; Gold Supply Shortage = -15% damage / +10% hostile-kill coins; Gold Eternal Flame = +15% damage taken / +10% XP; Gold Life Echo = +8 player max health + tamed regen; Prismatic Ancient Weakness = -30% damage / -15% max health / +50% death XP penalty; Prismatic Death Harvest = -30% damage / x3 death XP penalty; Prismatic Genesis Pact = +75% XP / -55% draw cost
- Friendly: Silver Pasture Harmony = +10% friendly speed; Silver Herd Watchful = +6 friendly max health; Gold Loyal Guard = +3 friendly attack + tamed regen; Gold Flock Shadow = -10 friendly health + 30% no-drop; Gold Pacifist Oath = 100% no-drop + 5% speed; Prismatic Beast King = +30% health / +20% attack + 35% hit-frenzy; Prismatic Angel Grace = tamed regen 2 + 30% speed; Prismatic Wrath of Fields = 100% hit-frenzy + 30% speed
- Neutral: Silver Wild Spirit = +3 neutral attack; Silver Vigilant Eye = +40% aggro; Gold Pack Rage = +4.5 attack + 25% counter; Gold Hunter Eye = +20% damage to neutrals / +10% XP; Gold Wild Instinct = 75% counter + 10% speed; Prismatic Apex Presence = +35% health / +25% attack + 30% frenzy; Prismatic Peace Vision = +60% vs neutrals / -55% draw cost; Prismatic Neutral Dominion = +30% health / +25% attack / players -45% vs them / 20% counter
- Hostile: Silver Shadow Power = +6 hostile health; Silver Fang Claw = +3 hostile attack; Gold Predator Swift = +20% speed + 10% lifesteal; Gold Armor Shell = +9 armor + 25% debuff; Gold Undead March = 40% summon + 4 health; Prismatic Abyss Awakened = +40% health / +30% attack / +40% speed / 30% debuff; Prismatic Blood Feast = +35% health + 20% lifesteal + 60% kill coins; Prismatic Demon Contract = 40% lifesteal + 40% debuff

Previous release notes:
- v0.4.6: World Hex rarity design rework (Silver=Stat / Gold=Stat+Mechanic / Prismatic=Percentage+Mechanic), protocol v14.
Hexcraft Workshop v0.4.6 - World Hex Rarity Design Rework (Silver=Stat / Gold=Stat+Mechanic / Prismatic=Percentage+Mechanic)
=============================================================================

Apology: the author's ability is limited, this mod is mostly built with AI assistance and may not live up to expectations. It is open source (MIT) and will keep being updated.

- **New World Hex rarity design standard**: Silver = stat-based (plain stats/multipliers); Gold = stat + mechanic; Prismatic = percentage stat + mechanic (hardest and strongest). A design direction for World Hex (more designs may come later); all 32 entries are reworked to fit it now
- **All 32 World Hex reworked**: hell difficulty core stats kept, mechanic effects added to Gold/Prismatic entries that were previously pure stats (hit-frenzy / death-summon / lifesteal / debuff / tamed regen / drop suppression / kill coin bonus / draw discount / death XP penalty) so every tier now lives up to its name
- **Hover label simplified**: World Hex entries now show only "Silver/Gold/Prismatic + description" on hover; internal design tags removed
- ZH/EN translations updated in sync: all 32 World Hex descriptions rewritten to match new effects (both languages stay at 793 keys)
- Content + client-label only: network protocol stays VERSION=14, v0.4.5 players can upgrade directly

Previous release notes:
- v0.4.5: UI redesign - World Hex 2x2 equal grid / Forge scroll clipping / TACZ tag fix / coin display fix, protocol v14.
Hexcraft Workshop v0.4.5 - UI Redesign: World Hex 2x2 Equal Grid / Forge Scroll Clipping / TACZ Tag Fix / Coin Display Fix
=============================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

- **World Hex screen fully redesigned (2x2 equal grid)**: all four category panels are now identical in width and height, symmetric and tidy, with rows evenly distributed; no more panels clipped off-screen at any GUI Scale (2/3/4) - the right-side Friendly/Hostile panels are no longer pushed outside the screen
- **World Hex entries now show info on hover**: entries only display their name (colored by rarity); the full description appears on mouse hover, with a new rarity tier label - Silver = Stat-based, Gold = Stat + Mechanic, Prismatic = Percentage + Mechanic
- **Fixed Forge screen scroll overlap**: the forged-stats list now has a scroll clip; group titles and panels are clipped away once scrolled out, no longer floating over the "Forged Stats" title and price buttons
- **Fixed TACZ tag overlap on the Forge screen**: the cyan "TACZ" tag now positions dynamically after the actual stat name width, no longer overlapping the right-column stat (e.g. TA Lightning Blade)
- **Fixed coin balance display**: the decorative dot no longer overlaps the text; the balance right-aligns to the content area and shrinks on narrow screens so it never crosses the edge or collides with the title
- **Code self-review**: verified all 34 config entries have real use points - except "Effects Switch" (PARTICLES_ENABLED, reserved config without a particle implementation yet); also fixed a hidden bug where the stats-page scroll clip used unscaled coordinates under UI Scale
- Client-side display only: network protocol stays VERSION=14, v0.4.4 players can upgrade directly

Previous release notes:
- v0.4.4: UI fixes - UI Scale now works, World Hex compact layout, Forge text overlap, protocol v14.
Hexcraft Workshop v0.4.4 - UI Fixes: UI Scale Now Works, World Hex Layout, Forge Text Overlap
=============================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

- **"UI Scale" option now actually works**: Config menu -> Client -> "UI Scale" (0.5x-2.0x, default 1.0) now scales the whole mod UI (main screen, World Hex screen, three-choice draws, "Coming Soon" page). Changes apply immediately, no restart needed
- **World Hex screen no longer hugs the screen bottom at GUI Scale 3/4**: tightened the compact-layout parameters - at GUI Scale 3 (1920x1080) both category rows fit fully with margin, no scrolling required
- **Fixed overlapping text on the Forge screen**: the "Forged Stats" title no longer collides with the group panel top; the three tier price buttons gained spacing from the "Forged N times" text; stat row spacing increased so long names and the "TACZ" tag no longer crowd
- **Code self-review**: all 6 screens now share a scaling base class (rendered with a global scale transform, mouse coordinates and hover detection converted accordingly), no conflicting references left; offline build passed (23s); server smoke OK (Done 3.1s); jar swapped into the client (328486B)
- Client-side display only: network protocol stays VERSION=14, v0.4.3 players can upgrade directly

Previous release notes:
- v0.4.3: Fixes - scroll overlap, Heartsteel stacking, Forge UI categories, protocol v14.
Hexcraft Workshop v0.4.3 - Fixes: Scroll Overlap, Heartsteel Stacking, Forge UI
=============================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

- **Fixed scroll overlap on the World Hex screen**: the "Draw World Hex" button used to stay pinned on top and covered the category panels while scrolling; it now scrolls together with the content, so nothing is hidden behind it
- **Fixed Heartsteel stacking on incoming damage**: "stack on hit" augments/gear (stack_attr_onhit) were wrongly triggered by thorns reflect damage - the Heartsteel gear comes with built-in thorns, so when a mob hits you the reflected damage counted as "your attack" and stacked layers. Reflect (thorns) damage no longer triggers stack-on-hit; stacking now only happens on your own attack hits. Heartsteel Soul (kill stacking) is unaffected
- **Forge UI category redesign**: forged stats now show in sci-fi group panels (Silver/Gold/Prismatic: tier color bar + group name + stat count, two-column layout within each group), fixing the previous layout bug where the first stat landed in the right column after a category header
- **Code self-review**: rechecked player event (deal damage / take damage / kill) cross-triggering and recursion guards; World Hex screen scroll-rebuild consistency; world vs player hex multipliers stay independent
- **Protocol upgraded to VERSION=14**: mechanic fix included - everyone must update to the same version (v0.4.2 clients failing the handshake with new servers is expected)
- Verified: offline build passed (23s); server smoke OK (Done 3.1s); jar swapped into the client (325788B)

Previous release notes:
- v0.4.2: UI fixes - text ghosting, GUI Scale adaption + scrolling, "Coming Soon" page, protocol v13.
- v0.4.1: All 32 world augments re-buffed, mechanic escalation (III tiers), UI fixes, unified sci-fi buttons, new K keybind, protocol v13.
- v0.4.0: World Hextech "Infernal Difficulty" - 32 augments, cap 8, protocol v12.
- v0.3.21: World Hextech (global world augments) - 24 augments, XP draw, coin cancel, second inventory tab.
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens.
- v0.3.18: Linked hextech system.

====================================================

Hexcraft Workshop v0.4.6 - 世界海克斯分层设计重排：白银=数值 / 黄金=数值+机制 / 棱彩=百分比数值+机制
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **确立世界海克斯稀有度分层设计规范**：白银 = 数值类（纯属性/纯倍率）；黄金 = 数值 + 机制；棱彩 = 百分比数值 + 机制（最难最强）。这是世界海克斯的一种设计要求（未来可扩展更多设计），当前全部 32 个按此规范重新整理
- **32 个世界海克斯全部重排**：保留炼狱难度主数值不变，给黄金/棱彩中原本"纯数值"的海克斯补充机制效果（受击反扑 / 死亡召唤 / 吸血 / 施毒 / 驯服回血 / 掉落抑制 / 击杀晶币加成 / 抽取折扣 / 死亡经验惩罚），让每一档名副其实
- **悬停标注精简**：世界海克斯条目悬停提示只显示「稀有度（白银/黄金/棱彩）+ 介绍」，不再显示内部设计标签
- 中英翻译同步更新：32 个世界海克斯介绍全部按新效果改写，双语言 key 数对等（各 793）
- 纯内容与客户端标注改动：网络协议保持 VERSION=14，v0.4.5 联机玩家可直接升级

之前版本：
- v0.4.5：UI 重排版 - 世界海克斯 2×2 等分网格 / 锻造器滚动裁剪 / TACZ 角标修复 / 余额显示修复，协议 v14。
Hexcraft Workshop v0.4.5 - UI 重排版版：世界海克斯 2×2 等分网格 / 锻造器滚动裁剪 / TACZ 角标修复 / 余额显示修复
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **世界海克斯界面全新重排（2×2 等分网格）**：四大分类区块同宽同高、上下左右对称工整，条目行高等分铺满；任意界面尺寸（GUI Scale 2/3/4）下都不再超屏缺角，右侧「友好/敌对」区块不再被挤出屏幕外
- **世界海克斯条目改为悬停查看**：条目只显示名字（按稀有度着色），鼠标放上去才显示完整介绍；并新增稀有度分层标注——白银=数值类、黄金=数值+机制、棱彩=百分比数值+机制
- **修复锻造器界面滚动遮挡**：为锻造列表加滚动裁剪，分类标题条与分组框滚出可视区即被裁剪隐藏，不再固定悬浮遮挡「已锻造属性」与价格按钮
- **修复锻造器 TACZ 角标重叠**：弹药补给等 TACZ 词条的「TACZ」角标改为按词条名实际宽度动态定位，不再侵入右侧「雷电之刃」等词条的显示区域
- **修复晶币余额显示**：余额文字与顶部光点装饰不再压叠，余额改为右对齐内容区、窄屏下自动收缩不超界面、不与「海克斯工坊」标题冲突
- **代码自检**：全量核查 34 项配置的引用点——除「特效开关」为保留配置项（当前无粒子实现点，已注明）外，其余全部有实际应用；修复属性页裁剪在界面缩放下坐标错位的隐藏问题
- 纯客户端显示层改动：网络协议保持 VERSION=14，v0.4.4 联机玩家可直接升级

之前版本：
- v0.4.4：UI 修复版 - 界面缩放生效 / 世界海克斯矮屏布局 / 锻造器文字重叠，协议 v14。
Hexcraft Workshop v0.4.4 - UI 修复版：界面缩放生效 / 世界海克斯矮屏布局 / 锻造器文字重叠
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **「界面缩放」配置现在真正生效**：配置菜单 → 客户端 →「界面缩放」（0.5x ~ 2.0x，默认 1.0）可整体放大/缩小海克斯工坊全部界面（主界面、世界海克斯、三选一抽取、敬请期待页），修改后即时生效、无需重开
- **修复世界海克斯界面在界面尺寸 3 / 4 下仍挤贴屏幕底**：矮屏压缩参数收紧，界面尺寸 3（1920×1080）下两排分类区块完整容纳、留有余量，不再贴底
- **修复锻造器界面文字重叠遮挡**：「已锻造属性」标题与下方分组框顶部不再重叠；三档价格按钮与「已锻造 N 次」文字留出间距；词条行距加大，长词条与「TACZ」角标不再拥挤
- **代码自检**：6 个界面统一接入缩放基类（渲染整体缩放 + 鼠标坐标/悬停判定同步换算），无冲突引用残留；编译构建成功（offline build 23s）；服务器冒烟启动正常（Done 3.1s）；正式客户端已替换（328486B）
- 纯客户端显示层改动：网络协议保持 VERSION=14，v0.4.3 联机玩家可直接升级

之前版本：
- v0.4.3：修复轮 - 滚动遮挡 / 心之钢叠层 / 锻造器分类，协议 v14。
Hexcraft Workshop v0.4.3 - 修复轮：滚动遮挡 / 心之钢叠层 / 锻造器分类
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **修复世界海克斯界面滚动遮挡**：顶部「抽取世界海克斯」按钮不再固定挡内容，随滚动一起移动，滚动查看不互相遮挡
- **修复心之钢受击也叠层的 Bug**：心之钢等「攻击命中叠层」之前会被荆棘反伤误触发（心之钢装备自带荆棘反伤，生物攻击你时反弹伤害被判定为"玩家造成伤害"）。现在反伤来源的伤害不再触发叠层，仅玩家主动攻击命中时叠层；心之钢魂（击杀叠层）不受影响
- **锻造器界面分类 UI 优化**：已锻造词条改为科技风分组框（白银/黄金/棱彩：档位色条 + 组名 + 计数，组内两列），修复分类标题导致首词条错位到右列的布局问题
- **代码自检**：复查玩家事件触发交叉与递归防护、界面滚动重建一致性、世界海克斯乘区独立性
- **网络协议升级至 VERSION=14**：涉及机制修复，联机需全员同步更新（v0.4.2 旧客户端连新服务端握手失败属预期）
- 自检：编译 / 构建成功（offline build 23s）；服务器冒烟启动正常（Done 3.1s）；正式客户端已替换（325788B）

====================================================

Hexcraft Workshop v0.4.2 - UI Fixes: Ghosting, GUI Scale, Coming Soon Page
=========================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

- **Fixed text ghosting on the 3-choice draw screen**: removed the old "glow = draw same text twice" trick (a +1px offset made it look like ghosting); names are now drawn once, cleanly
- **World Hex screen now adapts to GUI Scale**: layout is computed from screen height dynamically (rows and panels shrink on small screens) and supports mouse-wheel scrolling - no more off-screen panels when GUI Scale is set to 3 or 4; stays roomy on high resolutions
- **"More" button now opens a "Coming Soon" screen**: the left-sidebar "More" button on the World Hex screen opens a coming-soon page (title + note + back button) as the entry point for future categories; new i18n keys added (zh/en)
- Pure client-side display changes: no network/data/registry changes, protocol stays VERSION=13 - v0.4.1 players can update directly without a full re-install (syncing everyone is still recommended)
- Verified: offline build passed; server smoke OK; jar swapped into the client

Previous release notes:
- v0.4.1: All 32 world augments re-buffed, mechanic escalation (III tiers), UI fixes, unified sci-fi buttons, new K keybind, protocol v13.
- v0.4.0: World Hextech "Infernal Difficulty" - 32 augments, cap 8, protocol v12.
- v0.3.21: World Hextech (global world augments) - 24 augments, XP draw, coin cancel, second inventory tab.
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens.
- v0.3.18: Linked hextech system.

====================================================

Hexcraft Workshop v0.4.2 - 界面修复轮：重影 / 超屏 / 敬请期待页
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **修复三选一界面文字重影**：移除旧的"辉光 = 同名文字画两次"做法（+1 像素偏移叠加观感如重影），现在名称只绘制一遍，观感干净
- **世界海克斯界面随界面尺寸（GUI Scale）自适应**：布局改为按屏幕高度动态计算（矮屏自动压缩行高与区块高度），并支持滚轮滚动——界面尺寸调到 3 / 4 时也不再超屏，第二排分类区块滚动即可完整查看；高分辨率下保持舒展布局
- **「更多」按钮点击后打开「敬请期待」页**：世界海克斯界面左侧「更多」按钮现在会弹出一个敬请期待页面（标题 + 说明 + 返回按钮），提示更多分类正在开发中，为后续版本预留入口；新增中英翻译 key
- 纯客户端显示层改动：不动网络协议 / 同步数据 / 注册表，协议保持 VERSION=13，v0.4.1 联机玩家可直接升级，无需全员重装（仍建议全员同步更新）
- 自检：编译 / 构建成功（offline build）；服务器冒烟启动正常；正式客户端已替换

====================================================

Hexcraft Workshop v0.4.1 - Infernal Difficulty Raised Again + UI/Buttons/Keybind Fixes
======================================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

- **All 32 world augments re-buffed (higher stats, higher percentages)**:
  - General: -30% coins, -25% damage dealt, +30% damage taken, Ancient Weakness -15 HP/-18% output, Death Harvest -25% output + x2.5 death XP loss, Genesis Pact +50% XP / -60% draw cost
  - Friendly: 75% no-drop, Flock Shadow -10 HP, Beast King +14 HP/+4 atk, Angel Grace 1.5 regen, Pasture Harmony +15% speed
  - Neutral: +4.5 atk, +60% follow range, Apex Presence +24 HP/+40% speed, Wild Instinct 75% counter, Neutral Dominion +10 HP/+6 atk / players -45% vs neutrals
  - Hostile: +6 HP, +3 atk, +45% speed, +9 armor, Undead March 40% summon, Abyss Awaken +30 HP/+9 atk/+40% speed, Blood Feast +18 HP/+100% coins, Demon Contract 40% lifesteal + 40% debuff
- **Mechanic escalation**: berserk/counter becomes Strength/Speed III when chance >=75% (else II); debuff becomes Weakness II + Slowness III when chance >=35%; all debuff/berserk durations 5s -> 7s
- **UI fixes (World Hex Screen)**: block backgrounds now fixed height (empty categories render fully, no ragged panels); item text and cancel buttons are separated into columns (buttons no longer cover text)
- **Button style unified**: all buttons on the World Hex screen and the 3-choice draw screen now use the Hextech sci-fi style (dark blue-purple gradient, neon top edge, hover glow), matching the main UI instead of vanilla gray
- **New keybind**: K opens the World Hex screen directly (changeable in Controls), alongside H for the main screen
- **Network protocol upgraded to VERSION=13**: all players must use the same version; v0.4.0 clients fail the handshake against a new server (expected)
- Lang: 31 world-hex descriptions updated + 1 new keybind key per language (788 keys each)
- Verified: offline build passed; server smoke OK; jar swapped into the client; content table synced

Previous release notes:
- v0.4.0: World Hextech "Infernal Difficulty" - 32 augments, cap 8, protocol v12.
- v0.3.21: World Hextech (global world augments) - 24 augments, XP draw, coin cancel, second inventory tab.
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens.
- v0.3.18: Linked hextech system.

====================================================

Hexcraft Workshop v0.4.1 - 炼狱难度再拉高 + UI/按钮/按键修复
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **炼狱数值再拉高（高属性、高百分比）**：32 个世界海克斯全量上调
  - 通用：晶币 -30%、输出 -25%、受伤 +30%、远古衰弱 -15 血/-18% 输出、死亡收割 输出 -25% + 死亡经验惩罚 2.5 倍、创世契约 经验 +50%/-60% 抽取消耗
  - 友好：不掉落 75%、牧群阴影 -10 血、万兽之王 +14 血/+4 攻、天使眷顾 1.5 回血、牧场丰饶 +15% 移速
  - 中立：攻击 +4.5、索敌 +60%、终焉威压 +24 血/+40% 移速、野性本能 75% 反击、中立统御 +10 血/+6 攻/玩家对其 -45%
  - 敌对：暗影 +6 血、尖牙 +3 攻、迅捷 +45% 移速、护甲 +9、亡灵行军 40% 召唤、深渊觉醒 +30 血/+9 攻/+40% 移速、血腥盛宴 +18 血/晶币 +100%、恶魔契约 40% 吸血 + 40% Debuff
- **机制强度升级**：狂暴/反击概率 ≥75% 时力量与速度升级为 III 级（否则 II），Debuff 概率 ≥35% 时虚弱 II + 缓慢 III；所有 Debuff/狂暴时长 5 秒 → 7 秒
- **UI 修复（世界海克斯界面）**：区块背景固定高度（空分类同样完整显示）；条目文本与取消按钮分列（按钮不再遮挡文字）
- **按钮风格统一**：世界海克斯界面与三选一界面全部按钮改为海克斯科技风（深蓝紫渐变 + 霓虹顶边 + 悬停亮框），替代原版灰色按钮
- **按键绑定更新**：新增 K 键直达「世界海克斯」界面（可在按键绑定中修改），与 H 键并存
- **网络协议升级至 VERSION=13**：联机需全员同一版本（v0.4.0 旧客户端连新服务端握手失败属预期）
- 翻译：中英各 31 处描述同步 + 1 个新按键 key（各 788 keys）
- 自检：构建成功；服务器冒烟正常；正式客户端已替换；内容总表已同步

====================================================

Hexcraft Workshop v0.4.0 - World Hextech: Infernal Difficulty
============================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

This is a MAJOR version bump (v0.3.x -> v0.4.0): the World Hextech system enters "Infernal Difficulty".

- **World Hextech expanded from 24 to 32 augments** - 2 new high-value, high-mechanic augments per category:
  - General: Eternal Flame (all players take +20% damage), Death Harvest (-15% damage dealt + double XP loss on death)
  - Friendly: Pacifist Oath (friendly mobs drop nothing 100% of the time), Wrath of Fields (100% chance to gain Strength+Speed when hit)
  - Neutral: Wild Instinct (50% chance to counter with Strength+Speed when hit), Neutral Dominion (big HP/attack boost + players deal -30% to neutrals)
  - Hostile: Undead March (25% chance to summon a copy on death), Demon Contract (30% lifesteal + 25% chance to apply Weakness/Slowness)
- **All numbers buffed (difficulty raised)**: hostile/neutral HP, attack, speed, armor, follow range all increased; general curses worsened (-20% coins, -15% damage, Ancient Weakness -10 HP/-12% output); friendly buffs raised too (Life Echo +6, Angel Grace double regen, Genesis Pact +30% XP / -60% draw cost)
- **Higher slot cap**: max active per category 4 -> 8 (configurable, cap 32); each category panel now renders as a 2-column grid (4 rows x 2), cancel buttons follow each entry
- **Everyone sees everything**: the world hex list is save-level shared data (SavedData). When any player draws or cancels, the server broadcasts to all online players, so everyone's UI instantly shows what others activated/cancelled and the effects
- **Network protocol upgraded to VERSION=12**: all players must use the same version; old clients (v0.3.x) fail the handshake against a new server (expected, not a bug)
- **Crash-proof**: summons from Undead March carry a "no re-summon" flag, preventing chain-summon exponential blowups; all new mechanics use independent multipliers / events, no conflicts with other mods
- Lang: +16 keys per language (8 new names/descriptions) + 20 description updates -> ~769 keys
- Verified: offline build passed; server smoke test OK; content table synced to 32 world augments

Previous release notes:
- v0.3.21: World Hextech (global world augments) - 24 augments, XP draw, coin cancel, second inventory tab.
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens (all 30 options + 4 categories registered official Forge translation keys; Configured / MezzConfig follow the game language).
- v0.3.18: Linked hextech system - 7 new linked augments, triple attribute multiplier areas, PCL + Forge mod menu icons.

====================================================

Hexcraft Workshop v0.4.0 - 世界海克斯·炼狱难度
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

本次为**大版本升级（v0.3.x → v0.4.0）**：世界海克斯体系正式进入"炼狱难度"阶段。

- **世界海克斯扩充至 32 个**（原 24）：每类新增 2 个高数值高机制海克斯，难度上限大幅拉高
  - 通用新增：永燃诅咒（全体受伤 +20%）、死亡收割（输出 -15% + 死亡经验惩罚翻倍）
  - 友好新增：和平契约（友好生物 100% 不掉落）、田野之怒（受击 100% 力量+速度反扑）
  - 中立新增：野性本能（受击 50% 反击）、中立统御（血攻大加强 + 玩家对其伤害 -30%）
  - 敌对新增：亡灵行军（死亡 25% 召唤同类）、恶魔契约（30% 吸血 + 25% 施加虚弱/缓慢）
- **全量数值上调**（难度拉高）：敌对/中立生物血量、攻击、移速、护甲、索敌全部上调；通用诅咒加重（晶币 -20%、伤害 -15%、远古衰弱 -10 血/-12% 输出）；友好增益同步上调
- **装配上限提高**：每个分类最多同时激活 4 → 8 个（可配置，上限 32），界面分类区块改为双列网格展示
- **全玩家可见**：世界海克斯为存档级共享，任何人抽取/取消后全服广播，所有人界面即时显示别人抽到/取消了什么及效果
- **网络协议升级至 VERSION=12**：联机需全员同一版本，旧客户端连新服务端握手失败属预期
- **防崩溃机制**：亡灵行军召唤物带"免召唤"标记，杜绝连环召唤指数爆炸；新机制全部独立乘区，不与其它 mod 冲突
- 翻译：中英各 +16 keys + 20 处描述同步 → 约 769 keys
- 自检：构建成功（offline build）；服务器冒烟正常；内容总表同步至 32 个世界海克斯

====================================================

Hexcraft Workshop v0.3.21 - World Hextech (Global World Augments)
====================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

This update adds a brand-new global system: World Hextech.

- **World Hextech**: a whole-save global augment / curse system. One shared pool for the entire world; drawing one applies it instantly to everyone, visible to all players.
- **24 world augments** across 4 categories x 3 rarities:
  - Categories: General / Friendly Mobs / Neutral Mobs / Hostile Mobs
  - Rarity: Silver (simplest) < Gold (medium) < Prismatic (hardest, strongest buffs or heaviest curses)
  - Most effects raise difficulty (curses, monster buffs); a few are player-friendly.
- **Drawing**: pay XP (default 2000, configurable) and pick 1 of 3 options; each category can have up to 4 active simultaneously (configurable).
- **Canceling**: active world augments can be removed by spending Hex Coins (default 800, configurable); removal applies instantly to the whole server.
- **Independent math**: mob attributes use unique-UUID attribute modifiers (ADDITION for stats, MULTIPLY_TOTAL for speed/aggro), player-side global multipliers are computed separately - no conflicts with other mods' stacking.
- **UI**: new "World Hextech" tab right next to the "Hexcraft Workshop" tab in the inventory (zh: 世 / en: W). Main screen: left side first button = Creatures (more categories reserved), top = Draw button, below = 2x2 layout of the four categories, each entry has a Cancel button.
- **Config**: new "World Hextech" category in Cloth Config (master switch / draw XP / cancel coins / max per category - all adjustable and localized).
- **Network protocol upgraded to VERSION=11**: all players must use the same version online. Old clients fail the handshake against a new server (expected, not a bug).
- Lang: +60 keys per language (24 names + 24 descriptions + categories/UI/messages/config/tooltips) -> ~753 keys.
- Verified: offline build passed; jar already swapped into the client.

Previous release notes:
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens (all 30 options + 4 categories registered official Forge translation keys; Configured / MezzConfig follow the game language).
- v0.3.18: Linked hextech system - 7 new linked augments (Hextech Amplification, Weaver Master, Forge Grandmaster, Six Items, Smart Merchant, Money Flow, Exchange Expert), triple attribute multiplier areas, server-side transaction discounts/bonuses, PCL + Forge mod menu icons.
- v0.3.17: Unified mining pipeline - vein mining child blocks now share the same drop chain (fortune -> smelting -> magnet pickup), smelting recipe fallback for other mods' ores.

====================================================

Hexcraft Workshop v0.3.21 - 世界海克斯大更新
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

本次更新带来一个全新的全局玩法系统：世界海克斯。

- **世界海克斯**：作用于整个存档的全局增强 / 诅咒体系，全世界共享一份，抽到即全服即时生效、所有玩家均可见。
- **24 个世界海克斯**，按 4 大分类 × 3 稀有度设计：
  - 分类：通用 / 友好生物 / 中立生物 / 敌对生物
  - 稀有度：白银（最简单）< 黄金（中等）< 棱彩（最难，最强增益或重度诅咒）
  - 多数效果偏向提升难度（诅咒类、强化生物类），少数对玩家友好。
- **抽取规则**：花费经验（默认 2000，可配置）三选一抽取；每个分类最多同时激活 4 个（可配置）。
- **取消规则**：已激活的世界海克斯需花费晶币（默认 800，可配置）才能取消，取消后立即全服生效。
- **独立结算**：生物属性使用独立 UUID 属性修饰符（数值类 ADDITION、移速/索敌 MULTIPLY_TOTAL），玩家侧全局乘区独立计算，不与其它 mod 的数值叠加冲突。
- **界面**：背包内紧挨"海克斯工坊"页签新增"世界海克斯"页签（中文"世"/英文"W"）；主界面左侧首个按钮为"生物"分类（更多分类预留）、顶部为抽取按钮、下方四大类 2×2 排布、每条带取消按钮。
- **配置**：Cloth 配置新增「世界海克斯」分类（总开关 / 抽取经验 / 取消晶币 / 每类上限，全部可调且多语言）。
- **网络协议升级至 VERSION=11**：联机需要所有玩家同一版本，旧客户端连新服务端会握手失败（预期行为，非 bug）。
- 翻译：中英各 +60 keys（24 名称 + 24 描述 + 分类/UI/消息/配置/tooltip）→ 约 753 keys。
- 自检：构建成功，正式客户端 jar 已替换。

往期版本：
- v0.3.20：悬停查看效果 tooltip（海克斯 / 属性锻造器 / 装备）。
- v0.3.19：配置界面多语言（全部 30 配置项与 4 分类接入 Forge 官方翻译 key；Configured / MezzConfig 等第三方配置界面跟随游戏语言）。
- v0.3.18：联动海克斯体系（海克斯增幅/织装大师/锻造宗师/六神装/精明商人/财源滚滚/汇率高手），属性三乘区，交易折扣与加成全部服务端结算，PCL 与 Forge Mods 菜单图标。
- v0.3.17：统一矿物处理链路（连锁子方块共用掉落链：时运→熔炼→磁铁拾取），其它 mod 矿石熔炼配方兜底。

====================================================

Hexcraft Workshop v0.3.20 - Hover Tooltips for Effects
=======================================================

Hover over any hex augment, forge stat or piece of equipment to see exactly what it does.

Highlights:
- Hex list: hovering shows the augment name (rarity color) + description + source tag (TACZ / linked mods).
- Forge list: hovering shows the stat name with crafted count + its effect description.
- Equipment list: hovering shows the item name (with level/stacks) + effect description + sell value + source.
- Pure client-side display change: no network protocol / sync data / registry changes, fully safe for online play and mixed versions (protocol VERSION=10 unchanged).
- Added 4 lang keys per language (source_tag / sell_hint) -> 693 keys total.
- Verified: build passed, jar already swapped into the client.

Previous release notes:
- v0.3.19: Full localization for config screens (all 30 options + 4 categories registered official Forge translation keys; Configured / MezzConfig follow the game language).
- v0.3.18: Linked hextech system - 7 new linked augments (Hextech Amplification, Weaver Master, Forge Grandmaster, Six Items, Smart Merchant, Money Flow, Exchange Expert), triple attribute multiplier areas, server-side transaction discounts/bonuses, PCL + Forge mod menu icons.
- v0.3.17: Unified mining pipeline - vein mining child blocks now share the same drop chain (fortune -> smelting -> magnet pickup), smelting recipe fallback for other mods' ores.

====================================================

Hexcraft Workshop v0.3.20 - 悬停查看效果 tooltip
====================================================

把鼠标悬停到海克斯、属性锻造器、装备条目上，即可查看它们的具体效果。

更新内容：
- 海克斯页：悬停显示海克斯名称（稀有度配色）+ 效果描述 + 来源角标（TACZ / 联动内容）。
- 锻造器页：悬停显示锻造词条名称与已锻造次数 + 词条效果描述（如"攻击点燃目标 2 秒；每级燃烧伤害 +4%（重复锻造可叠加）"）。
- 装备页：悬停显示装备名称（含等级 / 叠层）+ 效果描述 + 卖出价 + 来源角标。
- 纯客户端显示层改动：不动网络协议（VERSION=10）/ 同步数据 / 注册表，联机无冲突，新老版本混连安全。
- 翻译：中英各 +4 keys（source_tag / sell_hint）→ 693 keys。
- 自检：构建成功，正式客户端 jar 已替换。

往期版本：
- v0.3.19：配置界面多语言（全部 30 配置项与 4 分类接入 Forge 官方翻译 key；Configured / MezzConfig 等第三方配置界面跟随游戏语言）。
- v0.3.18：联动海克斯体系（海克斯增幅/织装大师/锻造宗师/六神装/精明商人/财源滚滚/汇率高手），属性三乘区，交易折扣与加成全部服务端结算，PCL 与 Forge Mods 菜单图标。
- v0.3.17：统一矿物处理链路（连锁子方块共用掉落链：时运→熔炼→磁铁拾取），其它 mod 矿石熔炼配方兜底。

====================================================

Hexcraft Workshop v0.3.19 - Localized Config Screens
====================================================

This release brings full localization to the config screens, no matter which config mod you use.

Highlights:
- All 30 config options and 4 categories now register official Forge translation keys.
- Third-party config GUIs (Configured, MezzConfig, etc.) now display in the current game language (Chinese / English): option names, category names and tooltips are fully translated. No more raw English key names.
- The Cloth Config screen shares the same translation keys, so it keeps working exactly as before.
- Added Configured-compatible tooltip keys (+34 per language, 689 keys total in each lang file).
- Verified: build passed and every config key is aligned with the lang files.

How to test:
1. Set the game language to Chinese (简体中文) or English in Options > Language.
2. Open Mods > Configured (or any config mod) > Hexcraft Workshop.
3. Categories "海克斯抽取 / 经济系统 / 联动内容" and every option now show localized names and tooltips.

Previous release notes:
- v0.3.18: Linked hextech system - 7 new linked augments (Hextech Amplification, Weaver Master, Forge Grandmaster, Six Items, Smart Merchant, Money Flow, Exchange Expert), triple attribute multiplier areas, server-side transaction discounts/bonuses, PCL + Forge mod menu icons.
- v0.3.17: Unified mining pipeline - vein mining child blocks now share the same drop chain (fortune -> smelting -> magnet pickup), smelting recipe fallback for other mods' ores.

====================================================

Hexcraft Workshop v0.3.19 - 配置界面多语言
====================================================

本次更新为配置界面接入完整本地化，无论使用哪种配置 mod 都能显示当前游戏语言。

更新内容：
- 全部 30 个配置项与 4 个分类接入 Forge 官方翻译 key。
- Configured / MezzConfig 等第三方配置界面跟随游戏语言（中文 / English）显示：条目名、分类名、说明 tooltip 全部翻译，不再显示英文 key 名。
- Cloth Config 界面共用同一套翻译 key，不受影响。
- 补全 Configured 所需 tooltip 镜像 key（中英各 +34，共 689 keys）。
- 自检：构建通过，配置 key 与 lang 文件全量对齐。

测试方式：
1. 游戏选项 > 语言 切换为简体中文或 English。
2. Mods > Configured（或其它配置 mod）> 海克斯工坊。
3. 「海克斯抽取 / 经济系统 / 联动内容」分类及全部条目、说明均显示本地化文本。

往期版本：
- v0.3.18：联动海克斯体系 - 新增 7 个联动海克斯（海克斯增幅/织装大师/锻造宗师/六神装/精明商人/财源滚滚/汇率高手），属性三乘区，交易折扣与加成全部服务端结算，PCL 与 Forge Mods 菜单图标。
- v0.3.17：统一矿物处理链路 - 连锁子方块共用掉落链（时运→熔炼→磁铁拾取），其它 mod 矿石熔炼配方兜底。
