package id.glitchkid.kotobakeep;

import id.glitchkid.kotobakeep.language.Japanese;
import id.glitchkid.kotobakeep.network.Packets;
import id.glitchkid.kotobakeep.server.LearningServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KotobaKeep implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("KotobaKeep");
    public static Config CONFIG;
    public static Japanese JAPANESE;
    public static LearningServer SERVER;
    public void onInitialize() {
        CONFIG = Config.load(); JAPANESE = new Japanese(); Packets.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = new LearningServer(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { if (SERVER != null) SERVER.save(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SERVER = null);
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (SERVER != null) SERVER.tick(server); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { if (SERVER != null) SERVER.disconnect(handler.getPlayer()); });
        ServerPlayNetworking.registerGlobalReceiver(Packets.Ready.TYPE, (packet, context) -> { if (SERVER != null) SERVER.ready(context.player(), packet); });
        ServerPlayNetworking.registerGlobalReceiver(Packets.Answer.TYPE, (packet, context) -> { if (SERVER != null) SERVER.answer(context.player(), packet); });
        ServerPlayNetworking.registerGlobalReceiver(Packets.Retry.TYPE, (packet, context) -> { if (SERVER != null) SERVER.retry(context.player(), packet); });
        LOG.info("Kotoba Keep by glitchk.id — say it to keep it.");
    }
}
