package id.glitchkid.kotobakeep.server;

import id.glitchkid.kotobakeep.KotobaKeep;
import id.glitchkid.kotobakeep.language.Japanese;
import id.glitchkid.kotobakeep.network.Packets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class LearningServer {
    /** Marks a mob drop that can't be picked up until its killer names the mob. Saved with the entity. */
    private static final String LOCK_TAG = "kotobakeep_locked";
    private static final class Session {
        Map<Item, Integer> previous;
        /** Keyed by the Item picked up or the EntityType killed. */
        final LinkedHashMap<Object, Pending> queue = new LinkedHashMap<>();
        Pending active;
        long readyAt = -1000, gapUntil;
        int[] sentLocked = new int[0];
    }
    private static final class Pending {
        UUID id = UUID.randomUUID();
        final Object key;
        final String icon, name, translationKey, progressKey;
        final int duration;
        final ChallengeTimer timer;
        long sentAt;
        int count;
        // Item pickup: the penalty takes the newly gained items back out of the inventory.
        final ItemStack stack;
        final int protectedCount;
        // Mob kill: the penalty destroys the locked drops. Null for item pickups.
        final Map<UUID, ItemEntity> drops;

        private Pending(Object key, String icon, Japanese.Word word, String translationKey, String progressKey, ItemStack stack, int count, int protectedCount, Map<UUID, ItemEntity> drops) {
            this.key = key; this.icon = icon; this.name = word.text(); this.translationKey = translationKey; this.progressKey = progressKey;
            this.stack = stack; this.count = count; this.protectedCount = protectedCount; this.drops = drops;
            var config = KotobaKeep.CONFIG;
            this.duration = config.durationTicks(word.reading());
            this.timer = new ChallengeTimer(duration, config.recognitionGraceSeconds * 20, config.retryWindowSeconds * 20, config.maxRetries);
        }
        static Pending pickup(Item item, Japanese.Word word, ItemStack sample, int gained, int before) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            return new Pending(item, id, word, item.getDescriptionId(), id, sample.copyWithCount(1), gained, before, null);
        }
        static Pending kill(EntityType<?> type, Japanese.Word word, String progressKey) {
            String icon = SpawnEggItem.byId(type).map(egg -> BuiltInRegistries.ITEM.getKey(egg.value()).toString()).orElse("minecraft:air");
            return new Pending(type, icon, word, type.getDescriptionId(), progressKey, ItemStack.EMPTY, 0, 0, new LinkedHashMap<>());
        }
    }
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Random random = new Random();
    private LearningProgress progress = new LearningProgress();
    private final Path progressPath;
    private long tick;
    private boolean dirty;
    // A mob's drops spawn between ALLOW_DEATH and AFTER_DEATH; items loaded in that window are its loot.
    private LivingEntity dying;
    private final List<ItemEntity> dyingDrops = new ArrayList<>();

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
    private boolean available(ServerPlayer player, Session s) {
        return KotobaKeep.CONFIG.enabled && tick - s.readyAt <= 60 && player.isAlive() && !player.isSpectator();
    }
    private void schedule(ServerPlayer player, String progressKey) {
        var config = KotobaKeep.CONFIG;
        progress.recordSuccess(player.getUUID(), progressKey, LearningRules.nextDue(now(player), config.repeatMinSeconds, config.repeatMaxSeconds, random));
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
        if (s == null || s.active == null || !s.active.id.equals(packet.id()) || !available(player, s)) return;
        if (!s.active.timer.retry(tick)) return;
        // A fresh ID rejects duplicate retry packets and transcripts from the previous attempt.
        s.active.id = UUID.randomUUID();
        sendChallenge(player, s.active);
    }
    private void sendChallenge(ServerPlayer player, Pending p) {
        p.sentAt = tick;
        ServerPlayNetworking.send(player, new Packets.Challenge(p.id, p.icon, p.name, p.translationKey, p.count, p.duration, p.timer.retriesLeft()));
    }

    public void beforeDeath(LivingEntity entity, DamageSource source) {
        dying = entity instanceof Mob && source.getEntity() instanceof ServerPlayer ? entity : null;
        dyingDrops.clear();
    }
    public void entityLoaded(Entity entity) {
        if (!(entity instanceof ItemEntity item)) return;
        if (dying != null) dyingDrops.add(item);
        else if (item.entityTags().contains(LOCK_TAG)) relink(item);
    }
    public void afterDeath(LivingEntity entity, DamageSource source) {
        if (entity != dying) return;
        var drops = List.copyOf(dyingDrops);
        dying = null; dyingDrops.clear();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        Session s = sessions.get(player.getUUID());
        if (s == null || !available(player, s)) return;
        EntityType<?> type = entity.getType();
        Pending pending = s.active != null && s.active.key == type ? s.active : s.queue.get(type);
        if (pending == null) {
            String progressKey = "entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!progress.isDue(player.getUUID(), progressKey, now(player))) return;
            var word = KotobaKeep.JAPANESE.item(type.getDescriptionId());
            if (word == null || !word.pronounceable()) return;
            pending = Pending.kill(type, word, progressKey);
            s.queue.put(type, pending);
        }
        // Killing more of the same mob while it is asked adds those drops to the same challenge.
        for (ItemEntity drop : drops) {
            drop.setNeverPickUp(); drop.addTag(LOCK_TAG);
            pending.drops.put(drop.getUUID(), drop);
            pending.count += drop.getItem().getCount();
        }
    }
    /** A locked drop came back from an unloaded chunk as a new entity: track it again, or release it if nobody waits on it. */
    private void relink(ItemEntity item) {
        for (Session s : sessions.values()) {
            for (Pending p : pendings(s).toList()) {
                if (p.drops != null && p.drops.containsKey(item.getUUID())) { p.drops.put(item.getUUID(), item); return; }
            }
        }
        unlock(item);
    }
    private static void unlock(ItemEntity item) { item.removeTag(LOCK_TAG); item.setDefaultPickUpDelay(); }
    private static void release(Pending p) {
        if (p.drops == null) return;
        for (ItemEntity drop : p.drops.values()) if (!drop.isRemoved()) unlock(drop);
    }
    private static Stream<Pending> pendings(Session s) {
        return Stream.concat(Stream.ofNullable(s.active), s.queue.values().stream());
    }

    public void tick(MinecraftServer server) {
        tick++;
        dying = null; dyingDrops.clear(); // drop a death window another mod cancelled
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!ServerPlayNetworking.canSend(player, Packets.Challenge.TYPE)) continue;
            Session s = sessions.computeIfAbsent(player.getUUID(), k -> new Session());
            tickPlayer(player, s);
            syncLockedDrops(player, s);
        }
        if (dirty && tick % 200 == 0) save();
    }
    private void tickPlayer(ServerPlayer player, Session s) {
        Map<Item, Integer> inventory = snapshot(player);
        if (s.previous == null) s.previous = inventory;
        if (!available(player, s)) { cancel(player, s); s.previous = inventory; return; }
        for (var entry : inventory.entrySet()) {
            Item item = entry.getKey();
            int before = s.previous.getOrDefault(item, 0), gained = entry.getValue() - before;
            if (gained <= 0) continue;
            Pending joined = s.active != null && s.active.key == item ? s.active : s.queue.get(item);
            if (joined != null) { joined.count += gained; continue; }
            if (!progress.isDue(player.getUUID(), BuiltInRegistries.ITEM.getKey(item).toString(), now(player))) continue;
            var word = KotobaKeep.JAPANESE.item(item.getDescriptionId());
            if (word == null || !word.pronounceable()) continue;
            ItemStack sample = ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(item)) { sample = player.getInventory().getItem(i); break; }
            if (sample.isEmpty()) continue;
            s.queue.put(item, Pending.pickup(item, word, sample, gained, before));
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
    /** Tells the killer which drops to draw a lock box around, whenever that set changes. */
    private static void syncLockedDrops(ServerPlayer player, Session s) {
        int[] ids = pendings(s).filter(p -> p.drops != null).flatMap(p -> p.drops.values().stream())
            .filter(drop -> !drop.isRemoved()).mapToInt(Entity::getId).toArray();
        if (Arrays.equals(ids, s.sentLocked)) return;
        s.sentLocked = ids;
        ServerPlayNetworking.send(player, new Packets.LockedDrops(ids));
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
        int removed = outcome == 2 ? (p.drops != null ? destroyDrops(p) : takeBack(player, s, p)) : 0;
        if (outcome != 2) release(p);
        if (outcome == 1) schedule(player, p.progressKey);
        else if (outcome == 2) dirty |= progress.recordFailure(player.getUUID(), p.progressKey);
        ServerPlayNetworking.send(player, new Packets.Result(p.id, outcome, removed));
        s.active = null; s.gapUntil = tick + 30;
    }
    private static int destroyDrops(Pending p) {
        // ponytail: drops in chunks unloaded at this moment are released when they reload, not destroyed.
        int removed = 0;
        for (ItemEntity drop : p.drops.values()) {
            if (drop.isRemoved()) continue;
            removed += drop.getItem().getCount();
            drop.discard();
        }
        return removed;
    }
    private static int takeBack(ServerPlayer player, Session s, Pending p) {
        int removed = 0;
        int remaining = LearningRules.penalty(snapshot(player).getOrDefault(p.stack.getItem(), 0), p.protectedCount, p.count);
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!ItemStack.isSameItemSameComponents(stack, p.stack)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take); removed += take; remaining -= take;
        }
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        s.previous = snapshot(player);
        return removed;
    }
    private void cancel(ServerPlayer player, Session s) {
        if (s.active != null) finish(player, s, 0);
        s.queue.values().forEach(LearningServer::release);
        s.queue.clear();
    }
    public void disconnect(ServerPlayer player) {
        Session s = sessions.remove(player.getUUID());
        if (s != null) pendings(s).forEach(LearningServer::release);
        save();
    }
    /** Server stopping: unlock every drop before the world is saved. */
    public void stop() {
        sessions.values().forEach(s -> pendings(s).forEach(LearningServer::release));
        save();
    }
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
