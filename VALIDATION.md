# Kotoba Keep 1.3.0 verification

- New: killing a mob whose name is due asks for its Japanese name. Its drops lock (can't be picked up, don't merge) with a purple box drawn around each one for the killer; a correct answer unlocks them, a timeout destroys them, and setup problems release them.
- End-to-end check with a one-off Fabric client gametest (not kept in the build; microphone faked, answer sent as a transcript):
  - Cow killed with a player attack: every drop locked, the client got the ウシ challenge with a cow spawn egg icon and the locked-drop list, and standing on the drops for 1.5 s picked nothing up.
  - Answer うし: drops unlocked, and the ウシ voice clip is registered.
  - Pig left unanswered: after the timeout and retry window, its drop (3 porkchops) was destroyed and the client showed "removed 3".
  - Screenshot confirmed the see-through purple box around a dropped item.
  - Each box shows a red 🚫 (circle and slash) on every side facing the camera and on top; a second screenshot run confirmed it.
- Mob names: 168 entity names added to the furigana, speech-vocabulary and voice-clip checks (2,027 names total); 行商人 now reads ぎょうしょうにん. 121 new voice clips, 0 unverified.
- `gradlew build`: 21 tests passed. `gradlew speechSmoke`: passed for 2,027 names.
- Not verified: a live spoken answer, and multiplayer with several killers.

# Kotoba Keep 1.2.1 verification

- Menus no longer hide the challenge. Minecraft draws open menus over the HUD, so the panel was dimmed and mostly covered. It is now drawn on top of every menu as a compact strip. Simple Voice Chat push-to-talk already worked inside menus (its key hook runs before menus handle keys).
- R (retry) now works in item menus and is ignored while a text box has focus (chat, signs, creative search, anvil).
- Checked with a one-off Fabric client gametest (not kept in the build): screenshots of the normal panel, the strip over the inventory and over a large chest, and the strip hiding while the mouse hovers it.
- `gradlew build`: 21 tests passed.
- Not verified: a live spoken answer with a menu open.

# Kotoba Keep 1.2.0 verification

- `gradlew build`: **21 tests passed**. New: every one of the 1,859 vanilla item/block names has a bundled voice clip, and compound readings are no longer broken by single-kanji overrides (本棚 ほんだな, 森林 しんりん, 芽生えた めばえた, …入り いり, and others).
- `gradlew speechSmoke`: passed. The speech vocabulary still covers all 1,859 names after the reading fixes.
- `tools/generate_voices.py`: 1,769 unique clips, **0 unverified**. Each clip's VOICEVOX reading was compared against the mod's furigana reading. 1,724 use VOICEVOX's own reading; 108 needed misread kanji spelled in katakana and 27 needed a per-word user dictionary (counts are per item name).
- Dev client (`runClient`) reloaded resources with the 1,769-entry `sounds.json` and no missing-file or sound warnings.
- 1.2.0 is installed in the CurseForge Kotoba Keep profile; build, release and installed JAR hashes match.
- Not verified: hearing the clip in-game after a spoken answer. Use `/kotobakeep demo` to hear オークの原木.

# Kotoba Keep 1.1.2 verification

- `gradlew build`: **19 tests passed**, including the exact reported transcript `[unk] せ き たん` for **石炭**. Surrounding speech, unknown markers before/after the word, and long-vowel spelling variants are accepted.
- `[unk]` markers are stripped before the speech result reaches the HUD and answer packet; the server also cleans transcripts before grading them.
- Unknown-only audio, incomplete words, and a different pronunciation sharing a kanji character remain rejected (for example, **石炭** does not satisfy **石**).
- This matching change was verified with transcript regression tests and compilation. No new live spoken microphone test is claimed.
- Version 1.1.2 is installed in the CurseForge Kotoba Keep profile after Minecraft exited. The previous JAR was backed up outside `mods/`, and installed/build/release JAR hashes were compared.

## Previous 1.1.1 verification

- `gradlew build`: **16 tests passed**, including four new learned-word history tests. They cover ten consecutive failed pickups without a cooldown, the first correct answer starting the cooldown, failed reviews becoming immediately due again, persistence across reconnects, and separate history for each item/player.
- The server only records a learned-word cooldown on a correct answer. Queuing an item, joining with items already held, failing, and cancelling no longer teach a word or grant a cooldown.
- Learned-word history uses `data/kotobakeep-mastery.json`. Earlier pickup history files remain untouched because those records did not distinguish success from failure.
- Version 1.1.1 is installed in CurseForge; the previous JAR was backed up outside `mods/`. Installed/build/release JAR hashes are checked when packaging.
- This update was verified by automated tests and compilation; no live repeated-pickup playtest was performed for 1.1.1. The existing larger purple HUD, R retry, speech integration, and packet formats were not changed.

## Previous 1.1.0 verification

Updated locally on 2026-10-29 with larger Japanese text and furigana, the `#c888e8` accent theme, and one manual retry with an eight-second decision window.

- `gradlew build`: **12 tests passed**. Five new server timer tests cover the first-timeout retry offer, no penalty before the decision deadline, a complete timer reset, duplicate retry rejection, successful answers after a retry, repeated start acknowledgements, preparation time, and disabling retries.
- The 1.1.0 JAR is installed in the CurseForge Kotoba Keep profile. The old 1.0.0 JAR was backed up outside `mods/` under `config/kotobakeep/backups/`.
- Minecraft 26.3 launched successfully with Kotoba Keep **1.1.0**, the Simple Voice Chat plugin registered, and the Japanese speech model loaded.
- The existing profile config migrated with `hudAccentColor: "#c888e8"`, `maxRetries: 1`, and `retryWindowSeconds: 8`. The retry key saved correctly as `key.keyboard.r` in Minecraft options.
- Interactive retry testing was not completed: Windows automation twice reported user input in the Minecraft window. App input was stopped to leave control with the user. No 1.1.0 live retry or visual verification is claimed.
- Final installed, build, and release JAR hashes are checked by the packaging workflow.

The following checks were performed on the original 1.0.0 implementation and remain useful background; its screenshot documents the earlier UI, not the larger purple 1.1.0 HUD.

## Original 1.0.0 verification

Verified locally on Windows on 2026-10-29 with Java 25 and the user's CurseForge Minecraft installation.

- `gradlew build`: **7 tests passed**, covering original Japanese names, Minecraft-specific readings, complete kana reading coverage, full-word matching, cooldown bounds, protected stock and penalty limits, deadline scaling, and Simple Voice Chat push-to-talk/onboarding readiness.
- `gradlew speechSmoke`: **passed**. Speech vocabulary covers **1,859** base Minecraft 26.3 item/block translation keys. The actual Japanese Vosk model loads, native UTF-8 word lookups work on Windows, 48 kHz PCM silence does not produce a correct answer, and native recognizer reset/cleanup work.
- Packaged JAR launched successfully through **CurseForge → Kotoba Keep → Minecraft Launcher**, using Fabric Loader 0.20.1, Fabric API 0.165.0+26.3, and Simple Voice Chat 2.6.27+26.3.
- Simple Voice Chat loaded the Kotoba Keep plugin and connected to the integrated server. The packaged Japanese speech model loaded from the CurseForge profile.
- In an isolated `Kotoba Keep - Verification` world, acquiring three dirt items displayed **土**, **つち**, **tsuchi**, the item icon and a six-second countdown. With no spoken answer, all three newly acquired dirt items were removed.
- Reacquiring three dirt items inside the cooldown kept the items and did not display an immediate prompt.
- Acquiring an oak log during the dirt cooldown immediately displayed **オークの原木**, furigana **げんぼく**, romaji and a longer countdown. Its timeout removed the oak log while the three dirt items remained.
- The HUD was visually inspected in-game and captured with Minecraft's F2 screenshot function; see `release/kotoba-keep-hud.png`.
- Learning history persisted per item in the test world's `data/kotobakeep-progress.json`.
- The installed JAR and delivered release JAR have matching SHA-256 hashes.

An end-to-end spoken pronunciation test with the user's voice was not performed. Recognition quality depends on microphone input and pronunciation. Remote dedicated-server multiplayer was not run; singleplayer client/server networking and authoritative inventory removal were exercised in the integrated server.
