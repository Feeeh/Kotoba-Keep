package id.glitchkid.kotobakeep.client;

import com.google.gson.Gson;
import id.glitchkid.kotobakeep.language.Japanese;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Builds real model vocabulary phrases, including kana fragments for Minecraft-only words. */
public final class SpeechGrammar {
    private final Set<String> vocabulary=new HashSet<>();
    private final Set<String> kanaVocabulary=new HashSet<>();
    private final Japanese japanese;
    public SpeechGrammar(Path modelPath, Japanese japanese) throws Exception {
        this.japanese=japanese;
        try(var lines=Files.lines(modelPath.resolve("graph/words.txt"), StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                int split=line.lastIndexOf(' ');
                if(split<1)return;
                String word=line.substring(0,split);
                vocabulary.add(word);
                if(word.matches("[ぁ-ゖァ-ヶー]+"))kanaVocabulary.add(word);
            });
        }
    }
    public String phrase(Japanese.Word word) {
        var words=new ArrayList<String>();
        for(var ruby:word.ruby()) {
            if(ruby.text().isBlank())continue;
            // Kana spelling is deliberate: kanji such as 石 have multiple dictionary pronunciations.
            List<String> fragment=segment(ruby.reading());
            if(fragment==null && vocabulary.contains(ruby.text()))fragment=List.of(ruby.text());
            if(fragment==null)return null;
            words.addAll(fragment);
        }
        return words.isEmpty()?null:String.join(" ",words);
    }
    private List<String> segment(String reading) {
        if(reading.isBlank())return List.of();
        String kana=Japanese.normalize(reading).replace("ゔぁ","ば").replace("ゔぃ","び").replace("ゔぇ","べ").replace("ゔぉ","ぼ").replace("ゔ","ぶ");
        List<List<String>> best=new ArrayList<>(Collections.nCopies(kana.length()+1,null));best.set(0,List.of());
        for(int end=1;end<=kana.length();end++) {
            for(int start=0;start<end;start++) {
                if(best.get(start)==null)continue;
                String surface=kana.substring(start,end);
                String katakana=toKatakana(surface);
                String candidate=kanaVocabulary.contains(surface)?surface:kanaVocabulary.contains(katakana)?katakana:null;
                if(candidate==null)continue;
                if(best.get(end)==null || best.get(start).size()+1<best.get(end).size()) {
                    var list=new ArrayList<>(best.get(start));list.add(candidate);best.set(end,List.copyOf(list));
                }
            }
        }
        return best.get(kana.length());
    }
    private static String toKatakana(String kana) {
        var result=new StringBuilder();kana.codePoints().forEach(cp->result.appendCodePoint(cp>=0x3041&&cp<=0x3096?cp+0x60:cp));return result.toString();
    }
    public String json(Japanese.Word expected) {
        String target=phrase(expected);
        if(target==null)throw new IllegalArgumentException("No speech vocabulary for "+expected.text());
        var phrases=new LinkedHashSet<String>();phrases.add(target);
        // Distractors and unknown speech prevent a one-answer grammar from accepting everything.
        for(String alternative:List.of("こんにちは","わかりません","いいえ","違います","土","石","砂","水","木","鉄","銅","金","ダイヤモンド","オークの原木")) {
            String phrase=phrase(japanese.word(alternative));if(phrase!=null)phrases.add(phrase);
        }
        phrases.add("[unk]");
        return new Gson().toJson(phrases);
    }
}
