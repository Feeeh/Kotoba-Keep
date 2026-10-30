package id.glitchkid.kotobakeep.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public final class Packets {
    public record Challenge(UUID id, String item, String name, int count, int ticks, int retriesLeft) implements CustomPacketPayload {
        public static final Type<Challenge> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kotobakeep", "challenge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Challenge> CODEC = new StreamCodec<>() {
            public Challenge decode(RegistryFriendlyByteBuf b) { return new Challenge(b.readUUID(), b.readUtf(256), b.readUtf(256), b.readVarInt(), b.readVarInt(), b.readVarInt()); }
            public void encode(RegistryFriendlyByteBuf b, Challenge p) { b.writeUUID(p.id); b.writeUtf(p.item,256); b.writeUtf(p.name,256); b.writeVarInt(p.count); b.writeVarInt(p.ticks); b.writeVarInt(p.retriesLeft); }
        };
        public Type<Challenge> type() { return TYPE; }
    }
    public record Retry(UUID id) implements CustomPacketPayload {
        public static final Type<Retry> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kotobakeep", "retry"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Retry> CODEC = new StreamCodec<>() {
            public Retry decode(RegistryFriendlyByteBuf b) { return new Retry(b.readUUID()); }
            public void encode(RegistryFriendlyByteBuf b, Retry p) { b.writeUUID(p.id); }
        };
        public Type<Retry> type() { return TYPE; }
    }
    public record RetryOffer(UUID id, int ticks) implements CustomPacketPayload {
        public static final Type<RetryOffer> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kotobakeep", "retry_offer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RetryOffer> CODEC = new StreamCodec<>() {
            public RetryOffer decode(RegistryFriendlyByteBuf b) { return new RetryOffer(b.readUUID(), b.readVarInt()); }
            public void encode(RegistryFriendlyByteBuf b, RetryOffer p) { b.writeUUID(p.id); b.writeVarInt(p.ticks); }
        };
        public Type<RetryOffer> type() { return TYPE; }
    }
    public record Answer(UUID id, String text) implements CustomPacketPayload {
        // Long enough for any utterance in an 18-second window; a too-long string would disconnect the client.
        private static final int MAX_TRANSCRIPT = 1024;
        public static final Type<Answer> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kotobakeep", "answer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Answer> CODEC = new StreamCodec<>() {
            public Answer decode(RegistryFriendlyByteBuf b) { return new Answer(b.readUUID(), b.readUtf(MAX_TRANSCRIPT)); }
            public void encode(RegistryFriendlyByteBuf b, Answer p) { b.writeUUID(p.id); b.writeUtf(p.text,MAX_TRANSCRIPT); }
        };
        public Type<Answer> type() { return TYPE; }
    }
    // state 0: not ready, 1: ready heartbeat, 2: start the supplied challenge.
    public record Ready(int state, UUID id) implements CustomPacketPayload {
        public static final Type<Ready> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kotobakeep", "ready"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ready> CODEC = new StreamCodec<>() {
            public Ready decode(RegistryFriendlyByteBuf b) { return new Ready(b.readVarInt(), b.readUUID()); }
            public void encode(RegistryFriendlyByteBuf b, Ready p) { b.writeVarInt(p.state); b.writeUUID(p.id); }
        };
        public Type<Ready> type() { return TYPE; }
    }
    // outcome: 0 canceled safely, 1 kept, 2 failed. count is the actual removed quantity.
    public record Result(UUID id, int outcome, int count) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kotobakeep", "result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = new StreamCodec<>() {
            public Result decode(RegistryFriendlyByteBuf b) { return new Result(b.readUUID(), b.readVarInt(), b.readVarInt()); }
            public void encode(RegistryFriendlyByteBuf b, Result p) { b.writeUUID(p.id); b.writeVarInt(p.outcome); b.writeVarInt(p.count); }
        };
        public Type<Result> type() { return TYPE; }
    }
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(Challenge.TYPE, Challenge.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Result.TYPE, Result.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Answer.TYPE, Answer.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Ready.TYPE, Ready.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Retry.TYPE, Retry.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RetryOffer.TYPE, RetryOffer.CODEC);
    }
}
