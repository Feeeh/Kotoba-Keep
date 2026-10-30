package id.glitchkid.kotobakeep.server;

import id.glitchkid.kotobakeep.KotobaKeep;
import id.glitchkid.kotobakeep.network.Packets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

public final class LearningServer {
    private static final class Session {
        Map<Item, Integer> previous;
        final LinkedHashMap<Item, Pending> queue = new LinkedHashMap<>();
        Pending active;
        long readyAt = -1000, gapUntil;
    }
    private static final class Pending {
        UUID id = UUID.randomUUID();
        final ItemStack stack;
        final String name;
        final int protectedCount, duration;
        int count;
        final ChallengeTimer timer;
        long sentAt;
        Pending(ItemStack stack, String name, int count, int protectedCount, int duration) {
            this.stack = stack.copyWithCount(1); this.name = name; this.count = count;
            this.protectedCount = protectedCount; this.duration = duration;
            var config = KotobaKeep.CONFIG;
            this.timer = new ChallengeTimer(duration, config.recognitionGraceSeconds * 20, config.retryWindowSeconds * 20, config.maxRetries);
        }
    }
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Random random = new Random();
    private LearningProgress progress = new LearningProgress();
    private final Path progressPath;
    private long tick;
    private boolean dirty;
    public LearningServer(MinecraftServer server) {
        progressPath = server.getWorldPath(LevelResource.ROOT).resolve("data/kotobakeep-mastery.json");
        try {
            if (Files.exists(progressPath)) {
                progress = LearningProgress.fromJson(Files.readString(progressPath));
            } else if (Files.exists(progressPath.resolveSibling("kotobakeep-progress.json"))) {
                KotobaKeep.LOG.info("Legacy pickup history did not record correct answers. Words will be checked again; the old history file is preserved.");
            }
        } catch (Exception e) { KotobaKeep.LOG.error("Could not read learned-word history", e); }
    }
    private long now(ServerPlayer player) { return player.level().getServer().overworld().getGameTime(); }
    private void schedule(ServerPlayer player, Item item) {
        var config = KotobaKeep.CONFIG;
        progress.recordSuccess(player.getUUID(), BuiltInRegistries.ITEM.getKey(item).toString(), LearningRules.nextDue(now(player), config.repeatMinSeconds, config.repeatMaxSeconds, random));
        dirty = true;
    }
    public void ready(ServerPlayer player, Packets.Ready packet) {
        Session s = sessions.computeIfAbsent(player.getUUID(), k -> new Session());
        if (packet.state() == 0) { s.readyAt = -1000; cancel(player, s); return; }
        if (packet.state() != 1 && packet.state() != 2) return;
        s.readyAt = tick;
        if (packet.state() == 2 && s.active != null && s.active.id.equals(packet.id())) {
            s.active.timer.start(tick);
        }
    }
    public void answer(ServerPlayer player, Packets.Answer packet) {
        Session s = sessions.get(player.getUUID());
        if (s == null || s.active == null || !s.active.id.equals(packet.id()) || !s.active.timer.acceptsAnswer(tick)) return;
        if (KotobaKeep.JAPANESE.matches(s.active.name, packet.text())) finish(player, s, 1);
    }
    public void retry(ServerPlayer player, Packets.Retry packet) {
        Session s = sessions.get(player.getUUID());
        if (s == null || s.active == null || !s.active.id.equals(packet.id())) return;
        if (!KotobaKeep.CONFIG.enabled || tick - s.readyAt > 60 || !player.isAlive() || player.isSpectator()) return;
        if (!s.active.timer.retry(tick)) return;
        // A fresh ID rejects duplicate retry packets and transcripts from the previous attempt.
        s.active.id = UUID.randomUUID();
        sendChallenge(player, s.active);
    }
    private void sendChallenge(ServerPlayer player, Pending p) {
        p.sentAt = tick;
        ServerPlayNetworking.send(player, new Packets.Challenge(p.id, BuiltInRegistries.ITEM.getKey(p.stack.getItem()).toString(), p.name, p.count, p.duration, p.timer.retriesLeft()));
    }
    public void tick(MinecraftServer server) {
        tick++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ServerPlayNetworking.canSend(player, Packets.Challenge.TYPE)) continue;
            Session s = sessions.computeIfAbsent(player.getUUID(), k -> new Session());
            Map<Item, Integer> inventory = snapshot(player);
            if (s.previous == null) {
                s.previous = inventory;
            }
            boolean available = KotobaKeep.CONFIG.enabled && tick - s.readyAt <= 60 && player.isAlive() && !player.isSpectator();
            if (!available) { cancel(player, s); s.previous = inventory; continue; }
            for (var entry : inventory.entrySet()) {
                Item item = entry.getKey();
                int before = s.previous.getOrDefault(item, 0), gained = entry.getValue() - before;
                if (gained <= 0) continue;
                Pending queued = s.queue.get(item);
                if (s.active != null && s.active.stack.is(item)) { s.active.count += gained; continue; }
                if (queued != null) { queued.count += gained; continue; }
                if (!progress.isDue(player.getUUID(), BuiltInRegistries.ITEM.getKey(item).toString(), now(player))) continue;
                var word = KotobaKeep.JAPANESE.item(item.getDescriptionId());
                if (word == null || !word.pronounceable()) continue;
                ItemStack sample = ItemStack.EMPTY;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(item)) { sample = player.getInventory().getItem(i); break; }
                if (sample.isEmpty()) continue;
                s.queue.put(item, new Pending(sample, word.text(), gained, before, KotobaKeep.CONFIG.durationTicks(word.reading())));
            }
            s.previous = inventory;
            if (s.active != null) {
                if (s.active.timer.waitingForStart() && tick - s.active.sentAt > 200) finish(player, s, 0);
                else {
                    var transition = s.active.timer.advance(tick);
                    if (transition == ChallengeTimer.Transition.OFFER_RETRY)
                        ServerPlayNetworking.send(player, new Packets.RetryOffer(s.active.id, KotobaKeep.CONFIG.retryWindowSeconds * 20));
                    else if (transition == ChallengeTimer.Transition.FAIL) finish(player, s, 2);
                }
            }
            if (s.active == null && !s.queue.isEmpty() && tick >= s.gapUntil) {
                var iterator = s.queue.entrySet().iterator();
                s.active = iterator.next().getValue(); iterator.remove();
                sendChallenge(player, s.active);
            }
        }
        if (dirty && tick % 200 == 0) save();
    }
    private static Map<Item, Integer> snapshot(ServerPlayer player) {
        Map<Item, Integer> counts = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return counts;
    }
    private void finish(ServerPlayer player, Session s, int outcome) {
        Pending p = s.active;
        if (p == null) return;
        int removed = 0;
        if (outcome == 2) {
            int remaining = LearningRules.penalty(snapshot(player).getOrDefault(p.stack.getItem(), 0), p.protectedCount, p.count);
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!ItemStack.isSameItemSameComponents(stack, p.stack)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take); removed += take; remaining -= take;
            }
            player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
            s.previous = snapshot(player);
        }
        if (outcome == 1) schedule(player, p.stack.getItem());
        else if (outcome == 2) dirty |= progress.recordFailure(player.getUUID(), BuiltInRegistries.ITEM.getKey(p.stack.getItem()).toString());
        ServerPlayNetworking.send(player, new Packets.Result(p.id, outcome, removed));
        s.active = null; s.gapUntil = tick + 30;
    }
    private void cancel(ServerPlayer player, Session s) { if (s.active != null) finish(player, s, 0); s.queue.clear(); }
    public void disconnect(ServerPlayer player) { sessions.remove(player.getUUID()); save(); }
    public void save() {
        if (!dirty) return;
        try {
            Files.createDirectories(progressPath.getParent());
            Path temp = progressPath.resolveSibling(progressPath.getFileName() + ".tmp");
            Files.writeString(temp, progress.toJson());
            try { Files.move(temp, progressPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, progressPath, StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        } catch (Exception e) { KotobaKeep.LOG.error("Could not save Kotoba Keep learning history", e); }
    }
}
