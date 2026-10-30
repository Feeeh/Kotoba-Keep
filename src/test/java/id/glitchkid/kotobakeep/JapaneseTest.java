package id.glitchkid.kotobakeep;

import id.glitchkid.kotobakeep.language.Japanese;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JapaneseTest {
    private static final Japanese japanese=new Japanese();
    @Test void usesTheOriginalJapaneseMinecraftNames() {
        assertEquals("土",japanese.item("block.minecraft.dirt").text());
        assertEquals("つち",japanese.item("block.minecraft.dirt").reading());
        assertEquals("オークの原木",japanese.item("block.minecraft.oak_log").text());
        assertEquals("おーくのげんぼく",japanese.item("block.minecraft.oak_log").reading());
        assertEquals("いし",japanese.word("石").reading());
        assertEquals("かなとこ",japanese.word("金床").reading());
        assertEquals("こっぷん",japanese.word("骨粉").reading());
        assertEquals("どうのげんせき",japanese.word("銅の原石").reading());
        assertEquals("おちば",japanese.word("落ち葉").reading());
        assertEquals("かなあみ",japanese.word("金網").reading());
    }
    @Test void singleKanjiOverridesDoNotBreakCompoundReadings() {
        assertEquals("ほんだな",japanese.item("block.minecraft.bookshelf").reading());
        assertEquals("しんりんたんけんかのちず",japanese.item("item.minecraft.woodland_explorer_map").reading());
        assertEquals("しょうしんのつぼのかけら",japanese.item("item.minecraft.heartbreak_pottery_shard").reading());
        assertEquals("めばえたあめじすと",japanese.item("block.minecraft.budding_amethyst").reading());
        assertEquals("ひょうかい",japanese.item("block.minecraft.packed_ice").reading());
        assertEquals("おーくのなえぎいりうえきばち",japanese.item("block.minecraft.potted_oak_sapling").reading());
        assertEquals("ひょうひをはいだたけぶろっく",japanese.item("block.minecraft.stripped_bamboo_block").reading());
        assertEquals("てっこうせき",japanese.item("block.minecraft.iron_ore").reading());
    }
    @Test void acceptsKanaAndHomophonesButRejectsWrongOrIncompleteWords() {
        assertTrue(japanese.matches("土","つち"));
        assertTrue(japanese.matches("土","ツチ"));
        assertTrue(japanese.matches("オークの原木","オーク の 原木"));
        assertTrue(japanese.matches("オークの原木","おおくのげんぼく"));
        assertFalse(japanese.matches("土","石"));
        assertFalse(japanese.matches("オークの原木","げんぼく"));
        assertFalse(japanese.matches("土",""));
    }
    @Test void acceptsTheCompleteWordInsideSpeechEvenWithUnknownMarkers() {
        assertTrue(japanese.matches("石炭","[unk] せ き たん"));
        assertTrue(japanese.matches("石炭","せきたん [unk]"));
        assertTrue(japanese.matches("石炭","[unk] せきたん [unk]"));
        assertTrue(japanese.matches("石炭","えっと、せきたんです"));
        assertTrue(japanese.matches("石炭","これは石炭です [unk]"));
        assertTrue(japanese.matches("オークの原木","[unk] おおくのげんぼく です"));
        assertTrue(japanese.matches("土","つちじゃない"));
    }
    @Test void unknownOrIncompleteSpeechDoesNotPass() {
        assertFalse(japanese.matches("石炭","[unk]"));
        assertFalse(japanese.matches("石炭","[unk] [UNK]"));
        assertFalse(japanese.matches("石炭","[unk] せき"));
        assertFalse(japanese.matches("オークの原木","[unk] げんぼく"));
        assertFalse(japanese.matches("石","石炭"));
        assertFalse(japanese.matches("","せきたん"));
        assertFalse(japanese.matches("石炭",null));
    }
    @Test void hidesUnknownMarkersWithoutChangingRecognizedWords() {
        assertEquals("せ き たん",Japanese.cleanTranscript("[unk]  せ き たん [UNK]"));
        assertEquals("せきたん",Japanese.cleanTranscript("［unk］　せきたん　[unk]"));
        assertEquals("",Japanese.cleanTranscript("[unk] [unk]"));
        assertEquals("punk",Japanese.cleanTranscript("punk"));
    }
    @Test void everyVanillaItemBlockAndMobHasACompleteKanaReading() {
        assertEquals("ぎょうしょうにん",japanese.item("entity.minecraft.wandering_trader").reading());
        assertEquals("むらびとぞんび",japanese.item("entity.minecraft.zombie_villager").reading());
        var missing=new ArrayList<String>();
        for(var entry:japanese.translations().entrySet()) {
            if(!VoiceLines.isChallengeName(entry.getKey(),entry.getValue()))continue;
            var word=japanese.word(entry.getValue());
            if(!word.pronounceable())missing.add(entry.getKey()+" = "+word.text()+" → "+word.reading());
            assertEquals(word.text(),word.ruby().stream().map(Japanese.Ruby::text).reduce("",String::concat));
        }
        assertTrue(missing.isEmpty(),String.join("\n",missing));
    }
}
