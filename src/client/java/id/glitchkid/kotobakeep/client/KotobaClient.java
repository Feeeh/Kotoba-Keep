package id.glitchkid.kotobakeep.client;

import id.glitchkid.kotobakeep.KotobaKeep;
import id.glitchkid.kotobakeep.language.Japanese;
import id.glitchkid.kotobakeep.network.Packets;
import id.glitchkid.kotobakeep.voice.VoiceBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import java.util.UUID;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class KotobaClient implements ClientModInitializer {
    private static final int REPLAY_FLASH_TICKS = 30;
    // Mic gating around our clips. The sound engine reports a clip active from play() until it stops (at least 20 ticks).
    private static final int MUTE_MIN_TICKS = 10; // never unmute sooner, even if the clip is slow to start
    private static final int MUTE_TAIL_TICKS = 6; // 0.3 s of echo and voice chat latency after the clip ends
    private static final int MUTE_MAX_TICKS = 100; // 5 s cap, so a stuck sound can't mute forever; also caps listening
    public static KotobaClient INSTANCE;
    final UUID zero = new UUID(0,0);
    SpeechRecognizer speech;
    Packets.Challenge challenge;
    Japanese.Word word;
    int remaining, resultTicks, outcome = -1, removed, retryRemaining, retryTotalTicks;
    boolean started, submitted, flushed, pausedByUser, retryRequested;
    KeyMapping retryKey, replayKey, settingsKey;
    String heard = "";
    // Read by ChallengeHud. listening: a new word's clip is playing before the timer starts.
    // replayFlash: ticks left to show the replay cost on the timer after a replay.
    boolean listening;
    int replayFlash;
    private boolean recognizerReady, replayRequested;
    private SoundInstance clip; // our clip the mic is muted for
    private int clipTicks, tailTicks;
    private int[] lockedDrops = new int[0];
    private int tick;
    public void onInitializeClient() {
        INSTANCE = this; speech = new SpeechRecognizer();
        var keys = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kotobakeep", "controls"));
        retryKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kotobakeep.retry", InputConstants.KEY_R, keys));
        replayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kotobakeep.replay", InputConstants.KEY_J, keys));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kotobakeep.settings", InputConstants.KEY_K, keys));
        VoiceBridge.audio = speech::audio;
        ClientPlayNetworking.registerGlobalReceiver(Packets.Challenge.TYPE, (packet, context) -> begin(packet));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Result.TYPE, (packet, context) -> {
            if (challenge == null || !challenge.id().equals(packet.id())) return;
            outcome=packet.outcome(); removed=packet.count(); resultTicks=30; retryRemaining=0; stopAttempt();
            if (outcome == 1 || outcome == 2) playClip(challenge.key()); // kept or failed: hear the right name
        });
        ClientPlayNetworking.registerGlobalReceiver(Packets.RetryOffer.TYPE, (packet, context) -> {
            if (challenge == null || !challenge.id().equals(packet.id()) || outcome != -1) return;
            retryRemaining=packet.ticks(); retryTotalTicks=packet.ticks(); remaining=0; retryRequested=false; stopAttempt();
        });
        ClientPlayNetworking.registerGlobalReceiver(Packets.ReplayGranted.TYPE, (packet, context) -> {
            if (challenge == null || !challenge.id().equals(packet.id()) || outcome != -1) return;
            replayRequested=false; replayFlash=REPLAY_FLASH_TICKS; playClip(challenge.key());
            remaining=Math.max(0, remaining-packet.cost());
            if (remaining == 0) flush();
        });
        ClientPlayNetworking.registerGlobalReceiver(Packets.LockedDrops.TYPE, (packet, context) -> lockedDrops = packet.ids());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { stopAttempt(); endClip(); challenge=null; word=null; lockedDrops=new int[0]; VoiceBridge.connected=false; VoiceBridge.clientApi=null; });
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> LockBoxes.draw(context.levelRenderer(), lockedDrops));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> speech.close());
        var hud = new ChallengeHud(this);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("kotobakeep","challenge"), hud);
        // Menus are drawn over the HUD and swallow key bindings, so hook every menu while it is open.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> hud.drawOverScreen(g, mouseX, mouseY));
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, key) -> {
                if (!acceptsRetryKey(s)) return;
                if (retryKey.matches(key)) requestRetry();
                if (replayKey.matches(key)) requestReplay();
            });
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(literal("kotobakeep")
            .executes(ctx -> { ctx.getSource().sendFeedback(Component.literal("Kotoba Keep: " + status() + " | /kotobakeep pause, resume, retry, demo")); return 1; })
            .then(literal("pause").executes(ctx -> { pausedByUser=true; sendReady(0,zero); ctx.getSource().sendFeedback(Component.literal("Kotoba Keep paused. Items are safe.")); return 1; }))
            .then(literal("resume").executes(ctx -> { pausedByUser=false; ctx.getSource().sendFeedback(Component.literal("Kotoba Keep resumed. " + status())); return 1; }))
            .then(literal("retry").executes(ctx -> { if (!requestRetry()) ctx.getSource().sendFeedback(Component.literal("No retry is available for the current challenge.")); return 1; }))
            .then(literal("demo").executes(ctx -> { if(challenge==null) demo(); return 1; }))));
    }
    boolean available() { return !pausedByUser && speech.ready() && VoiceBridge.connected && !VoiceBridge.muted && !VoiceBridge.disabled; }
    String status() {
        if(pausedByUser)return "Paused — /kotobakeep resume";
        if(!speech.ready())return speech.status();
        if(!VoiceBridge.pluginLoaded)return "Waiting for Simple Voice Chat";
        if(!VoiceBridge.connected)return "Connect Simple Voice Chat (V)";
        if(VoiceBridge.disabled)return "Enable Simple Voice Chat (V)";
        if(VoiceBridge.muted)return "Unmute your microphone (V)";
        return "Hold your voice chat talk key and say the Japanese name";
    }
    private void begin(Packets.Challenge packet) {
        if(!available()){sendReady(0,zero);return;}
        challenge=packet; word=KotobaKeep.JAPANESE.word(packet.name()); remaining=packet.ticks();
        started=false; submitted=false; flushed=false; retryRequested=false; retryRemaining=0; resultTicks=0; outcome=-1; heard="";
        recognizerReady=false; replayRequested=false; replayFlash=0;
        listening=packet.streak()==0 && playClip(packet.key()); // a word not learned yet is heard before its timer starts
        speech.start(packet.id(), word, text -> Minecraft.getInstance().execute(() -> receiveTranscript(packet.id(),text)), () -> Minecraft.getInstance().execute(() -> {
            if(challenge!=null && challenge.id().equals(packet.id())){recognizerReady=true;startTimer();}
        }));
    }
    /** Starts the server timer once the recognizer is ready and a new word's clip has been heard. */
    private void startTimer() {
        if(!recognizerReady || listening || started || challenge==null || outcome!=-1 || !available())return;
        started=true; sendReady(2,challenge.id());
    }
    /** Ends the running attempt on this client: result, retry offer or disconnect. */
    private void stopAttempt() {
        started=false; listening=false; recognizerReady=false; replayRequested=false; replayFlash=0; speech.cancel();
    }
    private void flush() {
        if(!flushed){flushed=true;speech.flush(challenge.id());}
    }
    private void receiveTranscript(UUID id, String text) {
        if(challenge==null || !challenge.id().equals(id) || outcome!=-1 || submitted)return;
        heard=text;
        if(KotobaKeep.JAPANESE.matches(word.text(),text)){submitted=true;ClientPlayNetworking.send(new Packets.Answer(id,text));}
    }
    private void tick(Minecraft client) {
        tick++;
        if(client.player==null)return;
        VoiceBridge.refresh();
        while (retryKey.consumeClick()) requestRetry();
        while (replayKey.consumeClick()) requestReplay();
        while (settingsKey.consumeClick()) client.gui.setScreen(new SettingsScreen(null));
        if(tick%20==0)sendReady(available()?1:0,zero);
        if(client.isPaused())return; // the sound engine pauses our clip too, so the mute waits with it
        tickClip(client);
        if(replayFlash>0)replayFlash--;
        if(challenge!=null) {
            if(resultTicks>0){if(--resultTicks==0){challenge=null;word=null;}}
            else if(retryRemaining>0) retryRemaining--;
            else if(started && remaining>0 && --remaining==0) flush();
        }
    }
    /** Unmutes the mic once our clip and its tail are over, or at the cap. */
    private void tickClip(Minecraft client) {
        if(clip==null)return;
        clipTicks++;
        if(clipTicks>=MUTE_MIN_TICKS && !client.getSoundManager().isActive(clip))tailTicks--;
        if(tailTicks<=0 || clipTicks>=MUTE_MAX_TICKS)endClip();
    }
    private void endClip() {
        clip=null; speech.mute(false);
        if(listening){listening=false;startTimer();}
    }
    /** Plays a name clip with the mic muted until it ends. False when there's no clip to hear. */
    private boolean playClip(String translationKey) {
        var played = sayName(translationKey);
        if(played==null)return false;
        clip=played; clipTicks=0; tailTicks=MUTE_TAIL_TICKS; speech.mute(true);
        return true;
    }
    private void requestReplay() {
        if(!canReplay())return;
        replayRequested=true; // key repeat must not pay twice
        ClientPlayNetworking.send(new Packets.Replay(challenge.id()));
    }
    private boolean requestRetry() {
        if (challenge == null || outcome != -1 || challenge.retriesLeft() <= 0 || retryRequested || !available()) return false;
        if (!started && retryRemaining <= 0) return false;
        retryRequested=true; speech.cancel();
        ClientPlayNetworking.send(new Packets.Retry(challenge.id()));
        return true;
    }
    /** Item menus only, and never while typing (chat, signs, creative search, anvil names). */
    private static boolean acceptsRetryKey(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> && !screen.isInputCaptured();
    }
    String retryKeyName() { return retryKey.getTranslatedKeyMessage().getString(); }
    /** True when the replay key would be accepted now. */
    boolean canReplay() {
        return challenge!=null && outcome==-1 && started && !submitted && retryRemaining==0 && challenge.replayCost()>0
            && remaining>challenge.replayCost() && clip==null && !replayRequested && available();
    }
    String replayKeyName() { return replayKey.getTranslatedKeyMessage().getString(); }
    private void sendReady(int state, UUID id) {
        if(ClientPlayNetworking.canSend(Packets.Ready.TYPE))ClientPlayNetworking.send(new Packets.Ready(state,id));
    }
    private void demo() {
        // A local visual preview. It never creates a server challenge or removes items.
        word=KotobaKeep.JAPANESE.word("オークの原木");
        challenge=new Packets.Challenge(zero,"minecraft:oak_log",word.text(),"block.minecraft.oak_log",1,200,1,0,0,false);
        remaining=200;resultTicks=200;outcome=3;started=false;
        playClip(challenge.key());
    }
    /**
     * Plays the bundled VOICEVOX recording of the item or mob name (Voice/Speech volume).
     * Null when there is no recording (modded names) or it didn't start (no audio device, volume off).
     */
    private static SoundInstance sayName(String translationKey) {
        var sounds = Minecraft.getInstance().getSoundManager();
        var id = Identifier.tryBuild("kotobakeep", "say." + translationKey);
        if (id == null || sounds.getSoundEvent(id) == null) return null;
        var sound = new SimpleSoundInstance(id, SoundSource.VOICE, 1f, 1f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        return sounds.play(sound) == SoundEngine.PlayResult.NOT_STARTED ? null : sound;
    }
}
