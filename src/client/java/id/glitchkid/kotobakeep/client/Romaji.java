package id.glitchkid.kotobakeep.client;

import id.glitchkid.kotobakeep.language.Japanese;
import java.util.*;

public final class Romaji {
    private static final Map<String,String> TABLE = new HashMap<>();
    static {
        String[] rows = {"あいうえお:a i u e o", "かきくけこ:ka ki ku ke ko", "がぎぐげご:ga gi gu ge go", "さしすせそ:sa shi su se so", "ざじずぜぞ:za ji zu ze zo", "たちつてと:ta chi tsu te to", "だぢづでど:da ji zu de do", "なにぬねの:na ni nu ne no", "はひふへほ:ha hi fu he ho", "ばびぶべぼ:ba bi bu be bo", "ぱぴぷぺぽ:pa pi pu pe po", "まみむめも:ma mi mu me mo", "やゆよ:ya yu yo", "らりるれろ:ra ri ru re ro", "わをん:wa o n", "ぁぃぅぇぉゃゅょゔ:a i u e o ya yu yo vu"};
        for (String row : rows) { var pair=row.split(":"); var latin=pair[1].split(" "); for (int i=0;i<pair[0].length();i++) TABLE.put(pair[0].substring(i,i+1),latin[i]); }
        String[] compounds = {"きゃ:kya","きゅ:kyu","きょ:kyo","ぎゃ:gya","ぎゅ:gyu","ぎょ:gyo","しゃ:sha","しゅ:shu","しょ:sho","じゃ:ja","じゅ:ju","じょ:jo","ちゃ:cha","ちゅ:chu","ちょ:cho","にゃ:nya","にゅ:nyu","にょ:nyo","ひゃ:hya","ひゅ:hyu","ひょ:hyo","びゃ:bya","びゅ:byu","びょ:byo","ぴゃ:pya","ぴゅ:pyu","ぴょ:pyo","みゃ:mya","みゅ:myu","みょ:myo","りゃ:rya","りゅ:ryu","りょ:ryo","ふぁ:fa","ふぃ:fi","ふぇ:fe","ふぉ:fo","てぃ:ti","でぃ:di","とぅ:tu","どぅ:du","うぃ:wi","うぇ:we","うぉ:wo","しぇ:she","じぇ:je","ちぇ:che"};
        for (String compound : compounds) { var pair = compound.split(":"); TABLE.put(pair[0],pair[1]); }
    }
    public static String of(String source) {
        String text = Japanese.kana(source);
        StringBuilder result = new StringBuilder(); boolean doubleNext = false;
        for (int i=0;i<text.length();) {
            char c=text.charAt(i);
            if(c=='っ'){doubleNext=true;i++;continue;}
            if(c=='ー'){ if(!result.isEmpty()) result.append(result.charAt(result.length()-1)); i++;continue; }
            String unit = i+1<text.length() ? TABLE.get(text.substring(i,i+2)) : null;
            if(unit!=null)i+=2;else{unit=TABLE.getOrDefault(text.substring(i,i+1),text.substring(i,i+1));i++;}
            if(doubleNext&&!unit.isEmpty()){result.append(unit.startsWith("ch")?'t':unit.charAt(0));doubleNext=false;}
            result.append(unit);
        }
        return result.toString();
    }
}
