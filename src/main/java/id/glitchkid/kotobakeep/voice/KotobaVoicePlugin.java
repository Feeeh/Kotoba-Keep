package id.glitchkid.kotobakeep.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.*;

public final class KotobaVoicePlugin implements VoicechatPlugin {
    public String getPluginId() { return "kotobakeep"; }
    public void initialize(VoicechatApi api) { VoiceBridge.pluginLoaded = true; }
    public void registerEvents(EventRegistration events) {
        events.registerEvent(ClientVoicechatConnectionEvent.class, event -> {
            VoiceBridge.clientApi=event.getVoicechat();VoiceBridge.refresh();
        });
        events.registerEvent(MicrophoneMuteEvent.class, event -> VoiceBridge.muted = event.isDisabled());
        events.registerEvent(VoicechatDisableEvent.class, event -> VoiceBridge.disabled = event.isDisabled());
        events.registerEvent(ClientSoundEvent.class, event -> {
            VoiceBridge.clientApi=event.getVoicechat();VoiceBridge.refresh();
            if (!VoiceBridge.muted && !VoiceBridge.disabled) VoiceBridge.audio.accept(event.getRawAudio());
        });
    }
}
