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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import java.util.UUID;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class KotobaClient implements ClientModInitializer {
    public static KotobaClient INSTANCE;
    final UUID zero = new UUID(0,0);
    SpeechRecognizer speech;
    Packets.Challenge challenge;
    Japanese.Word word;
    int remaining, resultTicks, outcome = -1, removed, retryRemaining, retryTotalTicks;
    boolean started, submitted, flushed, pausedByUser, retryRequested;
    KeyMapping retryKey;
    String heard = "";
    private int[] lockedDrops = new int[0];
    private int tick;
    public void onInitializeClient() {
        INSTANCE = this; speech = new SpeechRecognizer();
        retryKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.kotobakeep.retry", InputConstants.KEY_R,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kotobakeep", "controls"))));
        VoiceBridge.audio = speech::audio;
        ClientPlayNetworking.registerGlobalReceiver(Packets.Challenge.TYPE, (packet, context) -> begin(packet));
        ClientPlayNetworking.registerGlobalReceiver(Packets.Result.TYPE, (packet, context) -> {
            if (challenge == null || !challenge.id().equals(packet.id())) return;
            outcome=packet.outcome(); removed=packet.count(); resultTicks=30; retryRemaining=0; started=false; speech.cancel();
            if (outcome == 1) sayName(challenge.key());
        });
        ClientPlayNetworking.registerGlobalReceiver(Packets.RetryOffer.TYPE, (packet, context) -> {
            if (challenge == null || !challenge.id().equals(packet.id()) || outcome != -1) return;
            retryRemaining=packet.ticks(); retryTotalTicks=packet.ticks(); remaining=0; started=false; retryRequested=false; speech.cancel();
        });
        ClientPlayNetworking.registerGlobalReceiver(Packets.LockedDrops.TYPE, (packet, context) -> lockedDrops = packet.ids());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { speech.cancel(); challenge=null; word=null; started=false; lockedDrops=new int[0]; VoiceBridge.connected=false; VoiceBridge.clientApi=null; });
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> LockBoxes.draw(context.levelRenderer(), lockedDrops));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> speech.close());
        var hud = new ChallengeHud(this);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("kotobakeep","challenge"), hud);
        // Menus are drawn over the HUD and swallow key bindings, so hook every menu while it is open.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> hud.drawOverScreen(g, mouseX, mouseY));
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, key) -> { if (retryKey.matches(key) && acceptsRetryKey(s)) requestRetry(); });
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
        speech.start(packet.id(), word, text -> Minecraft.getInstance().execute(() -> receiveTranscript(packet.id(),text)), () -> Minecraft.getInstance().execute(() -> {
            if(challenge!=null && challenge.id().equals(packet.id()) && available()){started=true;sendReady(2,packet.id());}
        }));
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
        if(tick%20==0)sendReady(available()?1:0,zero);
        if(client.isPaused())return;
        if(challenge!=null) {
            if(resultTicks>0){if(--resultTicks==0){challenge=null;word=null;}} 
            else if(retryRemaining>0) retryRemaining--;
            else if(started && remaining>0) {
                remaining--;
                if(remaining==0 && !flushed){flushed=true;speech.flush(challenge.id());}
            }
        }
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
    private void sendReady(int state, UUID id) {
        if(ClientPlayNetworking.canSend(Packets.Ready.TYPE))ClientPlayNetworking.send(new Packets.Ready(state,id));
    }
    private void demo() {
        // A local visual preview. It never creates a server challenge or removes items.
        word=KotobaKeep.JAPANESE.word("オークの原木");
        challenge=new Packets.Challenge(zero,"minecraft:oak_log",word.text(),"block.minecraft.oak_log",1,200,1);
        remaining=200;resultTicks=200;outcome=3;started=false;
        sayName(challenge.key());
    }
    /** Plays the bundled VOICEVOX recording of the item or mob name (Voice/Speech volume). */
    private static void sayName(String translationKey) {
        var sounds = Minecraft.getInstance().getSoundManager();
        var clip = Identifier.tryBuild("kotobakeep", "say." + translationKey);
        if (clip == null || sounds.getSoundEvent(clip) == null) return; // no recording for modded names
        sounds.play(new SimpleSoundInstance(clip, SoundSource.VOICE, 1f, 1f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }
}
