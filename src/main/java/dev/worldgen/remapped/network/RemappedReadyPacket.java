package dev.worldgen.remapped.network;

import dev.worldgen.remapped.Remapped;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class RemappedReadyPacket implements CustomPayload {
    public static final RemappedReadyPacket INSTANCE = new RemappedReadyPacket();
    public static final PacketCodec<PacketByteBuf, RemappedReadyPacket> CODEC = PacketCodec.unit(INSTANCE);
    public static final Id<RemappedReadyPacket> ID = new Id<>(Identifier.of(Remapped.MOD_ID, "remapped_ready"));

    private RemappedReadyPacket() {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
