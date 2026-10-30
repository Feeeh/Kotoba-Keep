package id.glitchkid.kotobakeep;

import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.config.ConfigAccessor;
import id.glitchkid.kotobakeep.voice.VoiceBridge;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class VoiceReadinessTest {
    @Test void pushToTalkIgnoresTheSavedVoiceActivationMuteFlagAndRefreshesAfterOnboarding() {
        var disabled=new AtomicBoolean(true);var activation=new AtomicReference<>("PTT");
        ConfigAccessor config=new ConfigAccessor() {
            public boolean hasKey(String key){return key.equals("microphone_activation_type");}
            public String getValue(String key){return hasKey(key)?activation.get():null;}
        };
        VoiceBridge.clientApi=(VoicechatClientApi)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{VoicechatClientApi.class},(proxy,method,args)->switch(method.getName()) {
            case "isDisconnected" -> false;
            case "isDisabled" -> disabled.get();
            case "isMuted" -> true;
            case "getClientConfig" -> config;
            default -> null;
        });
        try {
            VoiceBridge.refresh();assertTrue(VoiceBridge.disabled);
            disabled.set(false);VoiceBridge.refresh();
            assertTrue(VoiceBridge.connected);assertFalse(VoiceBridge.disabled);assertFalse(VoiceBridge.muted);
            activation.set("VOICE");VoiceBridge.refresh();assertTrue(VoiceBridge.muted);
        } finally { VoiceBridge.clientApi=null; }
    }
}
