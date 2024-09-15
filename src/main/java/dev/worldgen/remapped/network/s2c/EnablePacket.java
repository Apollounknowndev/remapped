package dev.worldgen.remapped.network.s2c;

import dev.worldgen.remapped.Remapped;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class EnablePacket implements CustomPayload {
    public static final EnablePacket INSTANCE = new EnablePacket();
    public static final PacketCodec<PacketByteBuf, EnablePacket> CODEC = PacketCodec.unit(INSTANCE);
    public static final Id<EnablePacket> ID = new Id<>(Identifier.of(Remapped.MOD_ID, "enable"));

    private EnablePacket() {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class RemappedEnableTask implements ServerPlayerConfigurationTask {
        public static final Key KEY = new Key(Identifier.of(Remapped.MOD_ID, "enable").toString());

        public RemappedEnableTask() {

        }

        @Override
        public void sendPacket(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(EnablePacket.INSTANCE));
        }

        @Override
        public Key getKey() {
            return KEY;
        }
    }
}
