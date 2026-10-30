package id.glitchkid.kotobakeep.voice;

import java.util.function.Consumer;
import de.maxhenkel.voicechat.api.VoicechatClientApi;

/** This common bridge never loads client-only classes on a dedicated server. */
public final class VoiceBridge {
    public static volatile Consumer<short[]> audio = samples -> {};
    public static volatile boolean connected, muted, disabled;
    public static volatile boolean pluginLoaded;
    public static volatile VoicechatClientApi clientApi;
    public static void refresh() {
        var api=clientApi;
        if(api==null)return;
        connected=!api.isDisconnected();disabled=api.isDisabled();
        // Simple Voice Chat retains the voice-activation mute flag in PTT mode, where it is ignored.
        boolean pushToTalk="PTT".equalsIgnoreCase(api.getClientConfig().getString("microphone_activation_type","PTT"));
        muted=!pushToTalk && api.isMuted();
    }
    private VoiceBridge() {}
}
