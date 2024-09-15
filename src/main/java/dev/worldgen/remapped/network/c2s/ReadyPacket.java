package dev.worldgen.remapped.network.c2s;

import dev.worldgen.remapped.Remapped;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ReadyPacket implements CustomPayload {
    public static final ReadyPacket INSTANCE = new ReadyPacket();
    public static final PacketCodec<PacketByteBuf, ReadyPacket> CODEC = PacketCodec.unit(INSTANCE);
    public static final Id<ReadyPacket> ID = new Id<>(Identifier.of(Remapped.MOD_ID, "ready"));

    private ReadyPacket() {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
