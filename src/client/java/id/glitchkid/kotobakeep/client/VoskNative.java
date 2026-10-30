package id.glitchkid.kotobakeep.client;

import com.sun.jna.*;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import java.util.Map;

/** Explicit UTF-8 applies only to this binding, including on Windows with a legacy ANSI codepage. */
public final class VoskNative {
    public interface Api extends Library {
        void vosk_set_log_level(int level);
        Pointer vosk_model_new(String path);
        void vosk_model_free(Pointer model);
        int vosk_model_find_word(Pointer model,String word);
        Pointer vosk_recognizer_new_grm(Pointer model,float rate,String grammar);
        int vosk_recognizer_accept_waveform_s(Pointer recognizer,short[] audio,int length);
        void vosk_recognizer_set_words(Pointer recognizer,int enabled);
        String vosk_recognizer_result(Pointer recognizer);
        String vosk_recognizer_partial_result(Pointer recognizer);
        String vosk_recognizer_final_result(Pointer recognizer);
        void vosk_recognizer_reset(Pointer recognizer);
        void vosk_recognizer_free(Pointer recognizer);
    }
    private static final class Binding {
        static final Api API=load();
        private static Api load() {
            // Let upstream extract its platform libraries and their dependencies first.
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            Api api=Native.load(Platform.isWindows()?"libvosk":"vosk",Api.class,Map.of(Library.OPTION_STRING_ENCODING,"UTF-8"));
            api.vosk_set_log_level(-1);
            return api;
        }
    }
    public static final class Model implements AutoCloseable {
        private final Pointer pointer;
        public Model(String path) { pointer=Binding.API.vosk_model_new(path);if(pointer==null)throw new IllegalStateException("Could not load Japanese speech model"); }
        public int wordId(String word) { return Binding.API.vosk_model_find_word(pointer,word); }
        public void close() { Binding.API.vosk_model_free(pointer); }
    }
    public static final class Recognizer implements AutoCloseable {
        private final Pointer pointer;
        public Recognizer(Model model,float rate,String grammar) {
            pointer=Binding.API.vosk_recognizer_new_grm(model.pointer,rate,grammar);
            if(pointer==null)throw new IllegalStateException("Could not create Japanese recognizer");
        }
        public void setWords(boolean enabled) { Binding.API.vosk_recognizer_set_words(pointer,enabled?1:0); }
        public boolean acceptWaveForm(short[] audio,int length) { return Binding.API.vosk_recognizer_accept_waveform_s(pointer,audio,length)!=0; }
        public String getResult() { return Binding.API.vosk_recognizer_result(pointer); }
        public String getPartialResult() { return Binding.API.vosk_recognizer_partial_result(pointer); }
        public String getFinalResult() { return Binding.API.vosk_recognizer_final_result(pointer); }
        public void reset() { Binding.API.vosk_recognizer_reset(pointer); }
        public void close() { Binding.API.vosk_recognizer_free(pointer); }
    }
}
