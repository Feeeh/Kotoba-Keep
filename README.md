# Kotoba Keep

**Say it to keep it.** A Minecraft **26.3 / Fabric** mod by **glitchk.id** that teaches you the original Japanese names of Minecraft items.

Pick up an item and a small panel appears at the top of the screen with its Japanese name, furigana above the kanji, romaji, and a countdown. Say the name into your Simple Voice Chat microphone before time runs out to keep the item. Get it right and you hear the word spoken back in natural Japanese. Run out of time and the newly picked-up items are removed.

- Uses the **official Japanese Minecraft names** (`ja_jp.json`), whatever language your game is set to.
- **Furigana** over every kanji, plus optional romaji.
- **Offline speech recognition** with [Vosk](https://alphacephei.com/vosk/). Nothing is uploaded or saved.
- **Native-style pronunciation**: every vanilla item name is pre-recorded with the VOICEVOX voice **冥鳴ひまり** (Meimei Himari) and played after a correct answer.
- New items are always asked. Items you've already answered correctly come back for review after a random **2–5 minutes** of play time.
- One retry per word, and nothing is ever removed because of a microphone or setup problem.

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

After a correct answer you hear the word spoken. The volume is Minecraft's **Voice/Speech** slider (Options → Music & Sounds); turn it down to mute the voice. Modded items have no recording.

Press **R** for one fresh attempt at the current word, either while its countdown runs or during the **8-second "Need more time?"** window after it expires. Items stay in your inventory while you decide. You can change R under Kotoba Keep in Minecraft's key bindings.

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
- Items already in your inventory when you join don't trigger a popup until you acquire more of them.
- After a correct answer, that item waits a random **120–300 seconds of world play time** before it can be asked again. The timer alone doesn't create a popup; you have to pick the item up again. Offline time doesn't count.
- A challenge lasts **6–18 seconds**, depending on the length of the reading, plus two seconds to finish recognition. Pausing singleplayer freezes the timer.
- The first timeout offers **one retry** with **8 seconds** to choose it, and removes nothing. The second attempt is final.
- Saying the complete name anywhere in your speech counts, even with extra words before or after it. Kana, katakana, kanji, spacing, long-vowel spellings and v/b variants are all accepted. Matching uses the spoken reading, so **石炭 (せきたん)** does not count for **石 (いし)**. This checks that the word was recognized; it is not a pronunciation score.
- On failure, the server removes only the **newly acquired quantity**. Items you already had are protected. If the new items were used, dropped or stored, fewer may be removed; the mod never takes from containers or keeps a debt.
- If voice chat disconnects, is muted or disabled, the model fails, or you pause the mod, current challenges cancel safely and nothing is removed.
- Furigana uses Kuromoji plus Minecraft-specific reading overrides (`src/main/resources/data/kotobakeep/readings.json`). Modded items without an official Japanese name are skipped.
- Potion effects, custom names, enchantments and damage don't create separate items; the base Japanese name is used.

## Multiplayer

Install Kotoba Keep, Fabric API and Simple Voice Chat on the Fabric server **and** every participating client. Only clients need the speech model. Use the same Kotoba Keep version everywhere. Players without the mod are ignored. Simple Voice Chat must connect normally (default UDP port 24454).

This is a cooperative learning mod, not anti-cheat: the client reports what it heard and the server checks it against the active challenge, so a modified client can skip its own challenges.

## Configuration

`config/kotobakeep.json` is created on first launch. Restart Minecraft or the server after editing it.

| Setting | Default | Notes |
| --- | --- | --- |
| `enabled` | `true` | |
| `repeatMinSeconds` / `repeatMaxSeconds` | `120` / `300` | Review cooldown after a correct answer |
| `minimumChallengeSeconds` / `maximumChallengeSeconds` | `6` / `18` | |
| `secondsPerMora` | `0.55` | Extra time per syllable |
| `recognitionGraceSeconds` | `2` | |
| `maxRetries` | `1` | Server; 0 to 5 |
| `retryWindowSeconds` | `8` | Server |
| `hudAccentColor` | `"#c888e8"` | |
| `hudTop` | `12` | GUI pixels from the top |
| `showRomaji` | `true` | |
| `modelPath` | `config/kotobakeep/vosk-model-small-ja-0.22` | |

Learning progress is saved per world in `<world>/data/kotobakeep-mastery.json`.

## Building

Requires **JDK 25**. Dependency versions are pinned in `gradle.properties` and `build.gradle`.

```powershell
.\gradlew.bat build
```

The JAR is written to `build/libs/`. Kuromoji, Vosk and JNA are bundled inside it; the speech model is not, to keep it smaller.

`gradlew build` runs the tests: translations, furigana readings, answer matching, cooldowns, penalties, timers, and a check that every item name has a voice clip. Two dev tasks need local files in `vendor/` (ignored by git):

- `gradlew runClient` needs `vendor/voicechat-fabric-2.6.27+26.3.jar` from the Simple Voice Chat releases.
- `gradlew speechSmoke` needs the extracted model in `vendor/models/vosk-model-small-ja-0.22`. It checks speech vocabulary for all 1,859 item names and loads the model through the native library.

### Releasing

GitHub Actions builds and tests every push. To publish a release, set `mod_version` in `gradle.properties`, commit, and push a matching tag:

```powershell
git tag v1.2.1
git push origin v1.2.1
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
