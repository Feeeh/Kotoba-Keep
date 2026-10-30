package id.glitchkid.kotobakeep.client;

import id.glitchkid.kotobakeep.client.VoskNative.Model;
import id.glitchkid.kotobakeep.client.VoskNative.Recognizer;
import com.google.gson.JsonParser;
import id.glitchkid.kotobakeep.language.Japanese;
import java.nio.file.Path;

/** Build-time native smoke check; not a Fabric entrypoint. */
public final class SpeechSmoke {
    public static void main(String[] args) throws Exception {
        var japanese=new Japanese();var grammar=new SpeechGrammar(Path.of(args[0]),japanese);
        int checked=0;var missing=new java.util.ArrayList<String>();
        for(var entry:japanese.translations().entrySet()) {
            if(!entry.getKey().matches("(?:item|block|entity)\\.minecraft\\.[^.]+")||entry.getValue().contains("%"))continue;
            if(grammar.phrase(japanese.word(entry.getValue()))==null)missing.add(entry.getKey()+" = "+entry.getValue()+" / "+japanese.word(entry.getValue()).reading());
            checked++;
        }
        java.nio.file.Files.write(Path.of("build/speech-vocabulary-missing.txt"),missing,java.nio.charset.StandardCharsets.UTF_8);
        if(!missing.isEmpty())throw new AssertionError("Missing speech vocabulary for "+missing.size()+" translations; see build/speech-vocabulary-missing.txt");
        System.out.println("PASS: speech vocabulary covers "+checked+" official Japanese item/block/mob translations, including Minecraft-only names");
        try(Model model=new Model(args[0]);Recognizer recognizer=new Recognizer(model,48000,grammar.json(japanese.word("土")))) {
            if(model.wordId("つち")<0 || model.wordId("オーク")<0)throw new AssertionError("Japanese UTF-8 did not reach the native recognizer intact");
            for(var phrase:JsonParser.parseString(grammar.json(japanese.word("土"))).getAsJsonArray()) {
                for(String token:phrase.getAsString().split(" "))if(model.wordId(token)<0)throw new AssertionError("Native vocabulary rejected UTF-8 token: "+token);
            }
            System.out.println("PASS: Japanese strings survive the Windows UTF-8 native boundary");
            short[] silence=new short[960];
            for(int i=0;i<150;i++)recognizer.acceptWaveForm(silence,silence.length);
            var result=JsonParser.parseString(recognizer.getFinalResult()).getAsJsonObject();
            if(!result.get("text").getAsString().isBlank())throw new AssertionError("Silence was recognized as speech: "+result);
            recognizer.reset();
            System.out.println("PASS: Japanese model, native library, 48 kHz PCM, silence rejection, reset and cleanup");
        }
    }
}
