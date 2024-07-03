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

public class RemappedEnablePacket implements CustomPayload {
    public static final RemappedEnablePacket INSTANCE = new RemappedEnablePacket();
    public static final PacketCodec<PacketByteBuf, RemappedEnablePacket> CODEC = PacketCodec.unit(INSTANCE);
    public static final Id<RemappedEnablePacket> ID = new Id<>(Identifier.of(Remapped.MOD_ID, "remapped_enable"));

    private RemappedEnablePacket() {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class RemappedEnableTask implements ServerPlayerConfigurationTask {
        public static final Key KEY = new Key(Identifier.of(Remapped.MOD_ID, "remapped_enable").toString());

        public RemappedEnableTask() {

        }

        @Override
        public void sendPacket(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(RemappedEnablePacket.INSTANCE));
        }

        @Override
        public Key getKey() {
            return KEY;
        }
    }
}
