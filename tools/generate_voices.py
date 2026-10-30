"""Pre-records every Japanese item name with VOICEVOX:冥鳴ひまり as Minecraft OGG sounds.

Each clip must say exactly the reading the mod shows as furigana and expects from the player.
VOICEVOX's own reading is used when it agrees (best pitch accent). Otherwise the misread kanji
get the mod's reading (e.g. 金床 is read キンユカ by Open JTalk, the mod says かなとこ), first as
katakana, then through a user dictionary; plain katakana for the whole name is the last resort.

Usage (setup in README, "Regenerating the voice clips"):
    gradlew voiceLines
    vendor/voicevox/venv/Scripts/python.exe -I -X utf8 tools/generate_voices.py
"""
import hashlib
import io
import json
from pathlib import Path

import soundfile
from voicevox_core import UserDictWord
from voicevox_core.blocking import Onnxruntime, OpenJtalk, Synthesizer, UserDict, VoiceModelFile

PROJECT = Path(__file__).resolve().parent.parent
CORE = PROJECT / "vendor/voicevox/core"
LINES = PROJECT / "build/voice-lines.json"
ASSETS = PROJECT / "src/client/resources/assets/kotobakeep"
STYLE = 14  # 冥鳴ひまり ノーマル, models/vvms/1.vvm
VOWELS = {"あ": "あかがさざただなはばぱまゃやらわぁ", "い": "いきぎしじちぢにひびぴみりぃ", "う": "うくぐすずつづぬふぶぷむゅゆるぅゔ",
          "え": "えけげせぜてでねへべぺめれぇ", "お": "おこごそぞとどのほぼぽもょよろをぉ"}


def hiragana(text):
    return "".join(chr(ord(c) - 0x60) if "ァ" <= c <= "ヶ" else c for c in text)


def katakana(text):
    return "".join(chr(ord(c) + 0x60) if "ぁ" <= c <= "ゖ" else c for c in text)


def has_kanji(text):
    return any("㐀" <= c <= "鿿" or c == "々" for c in text)


def phonetic(text):
    """Spelling-independent sound: オーク/おおく/おうく, クォ/クオ, ヴェ/ブエ and づ/ず compare equal."""
    text = hiragana(text).translate(str.maketrans("をぁぃぅぇぉゔづぢ", "おあいうえおぶずじ"))
    for split, joined in (("ぶあ", "ば"), ("ぶい", "び"), ("ぶえ", "べ"), ("ぶお", "ぼ")):
        text = text.replace(split, joined)
    out, last = [], ""
    for c in text:
        if c == "ー":
            out.append(last)
            continue
        if not "ぁ" <= c <= "ゖ":
            continue
        if (c == "う" and last == "お") or (c == "い" and last == "え"):
            c = last
        out.append(c)
        last = next((v for v, row in VOWELS.items() if c in row), "")
    return "".join(out)


def spoken(synth, text):
    query = synth.create_audio_query(text, STYLE)
    return query, phonetic("".join(m.text for phrase in query.accent_phrases for m in phrase.moras))


def corrected_text(synth, line):
    """Keeps correctly read segments as written (keeps their accent), spells misread kanji in katakana."""
    parts = []
    for ruby in line["ruby"]:
        misread = has_kanji(ruby["text"]) and spoken(synth, ruby["text"])[1] != phonetic(ruby["reading"])
        parts.append(katakana(ruby["reading"]) if misread else ruby["text"])
    return "".join(parts)


def user_words(synth, line):
    """The mod's reading for every kanji segment, with VOICEVOX's own accent estimate for that segment."""
    words = {}
    for ruby in line["ruby"]:
        if has_kanji(ruby["text"]):
            phrases = synth.create_audio_query(katakana(ruby["reading"]), STYLE).accent_phrases
            words[ruby["text"]] = (katakana(ruby["reading"]), phrases[0].accent if len(phrases) == 1 else 0)
    return words


def best_query(synth, scratch, line):
    """Returns (query, how the reading was obtained, identity of what VOICEVOX was given)."""
    target = phonetic(line["reading"])
    for source, text in (("text", line["text"]), ("corrected", corrected_text(synth, line))):
        query, sound = spoken(synth, text)
        if sound == target:
            return query, source, text
    # Context misreads (ガラス板 is ガラスバン even though 板 alone is イタ): keep VOICEVOX's phrasing and
    # pitch for the whole name, but force the mod's readings through a per-word user dictionary.
    words = user_words(synth, line)
    user_dict = UserDict()
    for surface, (pronunciation, accent) in words.items():
        user_dict.add_word(UserDictWord(surface, pronunciation, accent, priority=10))
    scratch.open_jtalk.use_user_dict(user_dict)
    query, sound = spoken(scratch, line["text"])
    if sound == target:
        return query, "user-dict", f"{line['text']}\t{sorted(words.items())}"
    text = katakana(line["reading"])
    query, sound = spoken(synth, text)
    return query, "katakana" if sound == target else "UNVERIFIED", text


def clip_name(tts_text):
    """Content-addressed, so a rerun only records words whose spoken text changed."""
    return hashlib.sha1(f"{STYLE}\t{tts_text}".encode()).hexdigest()[:16]


def synthesizer():
    synth = Synthesizer(Onnxruntime.load_once(filename=str(CORE / "onnxruntime/lib/voicevox_onnxruntime.dll")),
                        OpenJtalk(str(CORE / "dict/open_jtalk_dic_utf_8-1.11")))
    with VoiceModelFile.open(str(CORE / "models/vvms/1.vvm")) as model:
        synth.load_voice_model(model)
    return synth


def main():
    lines = json.loads(LINES.read_text(encoding="utf-8"))
    synth, scratch = synthesizer(), synthesizer()  # scratch gets a per-word user dictionary
    clips_dir = ASSETS / "sounds/say"
    clips_dir.mkdir(parents=True, exist_ok=True)
    sounds, report, recorded = {}, [], 0
    for key, line in sorted(lines.items()):
        query, source, tts_text = best_query(synth, scratch, line)
        name = clip_name(tts_text)
        clip = clips_dir / f"{name}.ogg"
        if source != "text":
            report.append(f"{source}\t{key}\t{line['text']}\t{line['reading']}")
        if not clip.exists():
            audio, rate = soundfile.read(io.BytesIO(synth.synthesis(query, STYLE)))
            soundfile.write(clip, audio, rate, format="OGG", subtype="VORBIS")
            recorded += 1
        sounds[f"say.{key}"] = {"sounds": [f"kotobakeep:say/{name}"]}
    used = {entry["sounds"][0].split("/")[-1] for entry in sounds.values()}
    stale = [old for old in clips_dir.glob("*.ogg") if old.stem not in used]
    for old in stale:
        old.unlink()
    (ASSETS / "sounds.json").write_text(json.dumps(sounds, ensure_ascii=False, indent=1), encoding="utf-8")
    (PROJECT / "build/voice-report.txt").write_text("\n".join(report), encoding="utf-8")
    print(f"{len(used)} clips for {len(sounds)} item names: {recorded} recorded, {len(stale)} stale removed; "
          f"{len(report)} needed a reading fix, {sum('UNVERIFIED' in r for r in report)} UNVERIFIED (build/voice-report.txt)")


if __name__ == "__main__":
    main()
