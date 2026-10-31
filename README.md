# Kotoba Keep

**Say it to keep it.** A Minecraft **26.3 / Fabric** mod by **glitchk.id** that teaches you the original Japanese names of Minecraft items.

Pick up an item and a small panel appears at the top of the screen with its Japanese name, furigana above the kanji, romaji, what it's called in your own game language, and a countdown. New words are spoken to you first; say the name into your Simple Voice Chat microphone before time runs out to keep the item. Get it right and you hear the word again in natural Japanese. Run out of time and the newly picked-up items are removed. Kill a mob and you have to name it too, or its drops disappear.

- Uses the **official Japanese Minecraft names** (`ja_jp.json`) for items, blocks and mobs, whatever language your game is set to.
- **Mob kills ask too**: the drops stay locked in a purple 🚫 box until you say the mob's name.
- **Furigana** over every kanji, plus optional romaji and the name in your own game language (土 → Dirt).
- **Listen first**: a word you haven't learned yet is spoken before its timer starts. Press **J** to hear it again for 3 seconds of your timer.
- **Offline speech recognition** with [Vosk](https://alphacephei.com/vosk/). Nothing is uploaded or saved.
- **Native-style pronunciation**: every vanilla item and mob name is pre-recorded with the VOICEVOX voice **冥鳴ひまり** (Meimei Himari). You hear it before a new word, on **J**, and after every answer, right or wrong.
- **Hints fade as you learn**: romaji disappears after 2 correct answers in a row, furigana after 4, so you move on to reading the kanji.
- **New words a few at a time**: at most one brand-new word a minute; reviews of words you've met keep coming.
- Items you've already answered correctly come back for review after a random **2–5 minutes** of play time, or, with **growing review gaps** turned on, after 3 min → 15 min → 1 h → 4 h → 1 day as they stick.
- Optional **gentle first meeting**: failing a word the first time you see it takes nothing.
- One retry per word, and nothing is ever removed because of a microphone or setup problem.
- **In-game settings**: press **K** to change every option, with explanations, in Minecraft's own options style.

## Requirements

| | Version |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | 0.20.1 or newer |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.165.0+26.3 |
| [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) | 2.6.27+26.3 or newer |
| Java | 25 |
| Japanese speech model | [vosk-model-small-ja-0.22](https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip) (client only) |

## Install

1. Put `kotoba-keep-<version>.jar`, Fabric API and Simple Voice Chat into your instance's `mods/` folder.
2. Download [vosk-model-small-ja-0.22.zip](https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip) and extract its `vosk-model-small-ja-0.22` folder into `config/kotobakeep/`.

**CurseForge on Windows:** create a Minecraft 26.3 Fabric profile named `Kotoba Keep`, then run the installer from a build (see [Building](#building)). It copies the mod, downloads the pinned Fabric API and Simple Voice Chat releases (SHA-512 verified) and the speech model (SHA-256 verified):

```powershell
.\tools\Install-CurseForge.ps1 -SkipBuild
# or: .\tools\Install-CurseForge.ps1 -ProfilePath 'C:\path\to\profile'
```

It only touches that profile, and backs up older Kotoba Keep JARs to `config/kotobakeep/backups/`.

## How to play

1. Enter a world and wait for the Japanese model to load. The panel shows the status until everything is ready.
2. Press **V** to open Simple Voice Chat and select your microphone.
3. Pick up an item. Hold your voice chat **push-to-talk** key (or use voice activation) and say the whole Japanese name. You can keep moving and playing while the panel is shown.
4. For dirt, say **つち (tsuchi)**. For an oak log, say **オークの原木 (おーくのげんぼく / ooku no genboku)**.

A word you haven't learned yet is spoken first ("Listen..."), and its timer starts once the clip ends. Words you're reviewing stay silent, since they test your memory. Press **J** during the countdown to hear the word again; it costs **3 seconds** of the timer, and the timer briefly shows `-3s`. The panel shows the J hint only while a replay is possible (not while a clip is playing, and not with 3 seconds or less left).

After every answer you hear the word: as a reward when you get it right, and as the correct pronunciation when time runs out. Your microphone is ignored while a clip plays, so speakers can't answer for you. The volume is Minecraft's **Voice/Speech** slider (Options → Music & Sounds); turn it down to mute the voice. Modded items have no recording, so they skip the listening step and can't be replayed.

Under the romaji, the panel shows the name in your game language (for example *Dirt*, *Oak Log*, *Cow*) so you know what the word means. It's hidden when your game is set to Japanese.

As you learn a word, its hints fade. After **2 correct answers in a row** its romaji is hidden, and after **4** its furigana too, so you read the kanji on your own. A wrong answer brings both back. The meaning line always stays.

New words arrive a few at a time: the first time you meet a word, it's asked only if no other new word was asked in the last **60 seconds**. Other new items you pick up in that minute (and the drops of new mobs you kill) are simply yours, and the word is asked on a later pickup. Words you've already met are not limited.

With **gentle first meeting** turned on (`gentleFirstMeeting`, off by default), failing a word the very first time it's asked takes nothing; the panel says "first meeting, items are safe". From the word's second ask on, the normal rules apply.

When you kill a mob whose name is due, its drops lock where they fell. Each one sits in a see-through purple box with a red 🚫 on its sides, so you know it can't be picked up yet, by you, other players or mobs. Say the mob's name (for a cow, **ウシ (ushi)**) and the drops unlock; run out of time and they vanish. Picking up the unlocked drops can then ask for those items' names as usual.

Challenges keep working while a menu is open (inventory, chests, furnaces, and so on). The panel shrinks to a strip along the top of the screen; hover your mouse over it to see the slots underneath.

Press **R** for one fresh attempt at the current word, either while its countdown runs or during the **8-second "Need more time?"** window after it expires. A fresh attempt gives the full time again, and a new word is spoken again first. R and J also work in item menus, but not while you're typing in a text box. Items stay in your inventory while you decide. You can change these keys under Kotoba Keep in Minecraft's key bindings (Options → Controls → Key Binds).

Press **K** to open the **Kotoba Keep settings**. Every option is there, with a tooltip that explains it. Changes apply when you close the screen (Done or Esc) and are saved to `config/kotobakeep.json`. A text box turns red while its value isn't valid, and the old value is kept until you fix it. On someone else's server you only see your own display settings; that server's config decides the learning and challenge rules.

| Command | What it does |
| --- | --- |
| `/kotobakeep` | Microphone and model status |
| `/kotobakeep pause` / `resume` | Pause or resume challenges (items are safe while paused) |
| `/kotobakeep retry` | Same as pressing R |
| `/kotobakeep demo` | Ten-second preview of the panel and voice; never removes items |

These are client commands and don't need cheats.

## Rules

- Each newly encountered **item type** creates a challenge. Ground pickups, crafting, chest transfers, trading and commands all count. Rearranging slots or equipping an item you already have does not.
- Different new items queue up, and each gets its own full timer. Picking up more of the item being asked joins that challenge without restarting its timer.
- An item counts as learned only when you say its name correctly. Until then, every new pickup asks again, with no cooldown. Failing a later review makes the next pickup ask again too.
- At most one never-met word is asked per **60 seconds** (`limitNewWords`). New items picked up in between are not asked or removed.
- Items already in your inventory when you join don't trigger a popup until you acquire more of them.
- After a correct answer, that item waits a random **120–300 seconds of world play time** before it can be asked again. The timer alone doesn't create a popup; you have to pick the item up again. Offline time doesn't count.
- With `growingReviewGaps` on, each correct answer in a row waits longer instead: **3 min, 15 min, 1 h, 4 h, then 1 day** of play time (`reviewGapMinutes`). A wrong answer starts the word over.
- A challenge lasts **6–18 seconds**, depending on the length of the reading, plus two seconds to finish recognition. Pausing singleplayer freezes the timer.
- The first timeout offers **one retry** with **8 seconds** to choose it, and removes nothing. The second attempt is final.
- Saying the complete name anywhere in your speech counts, even with extra words before or after it. Kana, katakana, kanji, spacing, long-vowel spellings and v/b variants are all accepted. Matching uses the spoken reading, so **石炭 (せきたん)** does not count for **石 (いし)**. This checks that the word was recognized; it is not a pronunciation score.
- On failure, the server removes only the **newly acquired quantity**. Items you already had are protected. If the new items were used, dropped or stored, fewer may be removed; the mod never takes from containers or keeps a debt.
- If voice chat disconnects, is muted or disabled, the model fails, or you pause the mod, current challenges cancel safely and nothing is removed.
- Furigana uses Kuromoji plus Minecraft-specific reading overrides (`src/main/resources/data/kotobakeep/readings.json`). Modded items without an official Japanese name are skipped.
- Potion effects, custom names, enchantments and damage don't create separate items; the base Japanese name is used.
- **Mobs** follow the same learning rules as items: a mob you haven't named correctly yet asks on every kill, and a known mob comes back after 2–5 minutes of play. Only kills by you count (melee, arrows, tridents); deaths from lava, falls or pets don't. Killing more of the same mob while it's being asked adds those drops to the same challenge. Named mobs, baby mobs and villager professions use the base mob name (村人 for every villager).
- Locked drops are released, never destroyed, if the challenge is cancelled (microphone off, paused, disconnected, server stopping). Drops that are in an unloaded chunk when the timer runs out are released when the chunk loads again. Hoppers can still collect locked drops.

## Multiplayer

Install Kotoba Keep, Fabric API and Simple Voice Chat on the Fabric server **and** every participating client. Only clients need the speech model. Use the same Kotoba Keep version everywhere; 1.5.0 changed the network format, so older versions can't connect to it. Players without the mod are ignored. Simple Voice Chat must connect normally (default UDP port 24454).

This is a cooperative learning mod, not anti-cheat: the client reports what it heard and the server checks it against the active challenge, so a modified client can skip its own challenges.

## Configuration

Press **K** in game to change these settings on the settings screen. They apply as soon as you close it. They are stored in `config/kotobakeep.json`, created on first launch. If you edit the file by hand, restart Minecraft or the server afterwards. "Server" settings come from the server's file in multiplayer; in singleplayer and on your own LAN world they're yours.

| Setting | Default | Notes |
| --- | --- | --- |
| `enabled` | `true` | |
| `repeatMinSeconds` / `repeatMaxSeconds` | `120` / `300` | Review cooldown after a correct answer |
| `minimumChallengeSeconds` / `maximumChallengeSeconds` | `6` / `18` | |
| `secondsPerMora` | `0.55` | Extra time per syllable |
| `recognitionGraceSeconds` | `2` | |
| `maxRetries` | `1` | Server; 0 to 5 |
| `retryWindowSeconds` | `8` | Server |
| `replayCostSeconds` | `3` | Server; 0 to 10. Seconds a replay (J) takes off the timer; 0 turns replays off |
| `growingReviewGaps` | `false` | Server. Use `reviewGapMinutes` instead of `repeatMinSeconds`/`repeatMaxSeconds` |
| `reviewGapMinutes` | `[3, 15, 60, 240, 1440]` | Server. Minutes of play time after the 1st, 2nd, 3rd… correct answer in a row; the last one repeats. Up to 20 gaps, 1 to 525600 each |
| `fadeHints` | `true` | Client. Hide hints for words you keep getting right |
| `hideRomajiAfter` / `hideFuriganaAfter` | `2` / `4` | Client; 1 to 20. Correct answers in a row before romaji / furigana are hidden |
| `limitNewWords` | `true` | Server. At most one never-met word per `newWordGapSeconds` |
| `newWordGapSeconds` | `60` | Server; 1 to 3600 |
| `gentleFirstMeeting` | `false` | Server. Failing a word the first time it's asked takes nothing |
| `hudTop` | `12` | GUI pixels from the top |
| `showRomaji` | `true` | |
| `modelPath` | `config/kotobakeep/vosk-model-small-ja-0.22` | |

Learning progress is saved per world in `<world>/data/kotobakeep-words.json`: for each word, when it's next due and how many times in a row you've answered it right. Worlds from 1.4.0 or older are converted from `kotobakeep-mastery.json` on first load (each learned word starts at 1 correct answer); the old file is left in place.

## Building

Requires **JDK 25**. Dependency versions are pinned in `gradle.properties` and `build.gradle`.

```powershell
.\gradlew.bat build
```

The JAR is written to `build/libs/`. Kuromoji, Vosk and JNA are bundled inside it; the speech model is not, to keep it smaller.

`gradlew build` runs the tests: translations, furigana readings, answer matching, cooldowns, penalties, timers, and a check that every item and mob name has a voice clip. Two dev tasks need local files in `vendor/` (ignored by git):

- `gradlew runClient` needs `vendor/voicechat-fabric-2.6.27+26.3.jar` from the Simple Voice Chat releases.
- `gradlew speechSmoke` needs the extracted model in `vendor/models/vosk-model-small-ja-0.22`. It checks speech vocabulary for all 2,027 item and mob names and loads the model through the native library.

### Releasing

GitHub Actions builds and tests every push. To publish a release, set `mod_version` in `gradle.properties`, commit, and push a matching tag:

```powershell
git tag v1.5.0
git push origin v1.5.0
```

The workflow attaches `kotoba-keep-<version>+26.3.jar` and its SHA-256 checksum to a new GitHub release. It refuses tags that don't match `mod_version`. The release text comes from `.github/release-notes.md` plus the commit list.

### Regenerating the voice clips

The OGG clips in `src/client/resources/assets/kotobakeep/sounds/say/` are generated. Each clip is checked against the mod's furigana reading; when VOICEVOX would misread a Minecraft name (for example 金床 as きんゆか instead of かなとこ), the generator fixes the reading. Unchanged words are not re-recorded.

One-time setup with [VOICEVOX CORE 0.17.1](https://github.com/VOICEVOX/voicevox_core/releases/tag/0.17.1) (download the Windows wheel and `download-windows-x64.exe` from that release):

```powershell
py -m venv vendor/voicevox/venv
vendor/voicevox/venv/Scripts/python.exe -m pip install voicevox_core-0.17.1-cp310-abi3-win_amd64.whl soundfile
download-windows-x64.exe --only onnxruntime dict models --models-pattern 1.vvm --devices cpu -o vendor/voicevox/core
```

The downloader asks you to accept the VOICEVOX voice model terms. Then:

```powershell
.\gradlew.bat voiceLines
vendor/voicevox/venv/Scripts/python.exe -I -X utf8 tools/generate_voices.py
```

Words that needed a reading fix are listed in `build/voice-report.txt`.

## Credits and license

- Code: MIT, see [LICENSE](LICENSE).
- Voice: **VOICEVOX:冥鳴ひまり**. Keep this credit if you redistribute the clips.
- Japanese item names: Mojang / Microsoft and the Minecraft translation contributors.
- Speech recognition: Vosk and its small Japanese model (Apache 2.0). Furigana: Kuromoji (Apache 2.0).

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for details. This is an unofficial Minecraft mod, not approved by or associated with Mojang or Microsoft.
