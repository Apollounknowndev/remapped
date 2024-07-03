package dev.worldgen.remapped.network;

import dev.worldgen.remapped.Remapped;
import dev.worldgen.remapped.map.RemappedState;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RemappedMapUpdatePacket(MapIdComponent mapId, byte scale, boolean locked, Optional<List<MapDecoration>> decorations, Optional<RemappedState.UpdateData> updateData) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, RemappedMapUpdatePacket> CODEC = PacketCodec.tuple(
        MapIdComponent.PACKET_CODEC, RemappedMapUpdatePacket::mapId,
        PacketCodecs.BYTE, RemappedMapUpdatePacket::scale,
        PacketCodecs.BOOL, RemappedMapUpdatePacket::locked,
        MapDecoration.CODEC.collect(PacketCodecs.toList()).collect(PacketCodecs::optional), RemappedMapUpdatePacket::decorations,
        RemappedState.UpdateData.OPTIONAL_CODEC, RemappedMapUpdatePacket::updateData,
        RemappedMapUpdatePacket::new
    );
    public static final CustomPayload.Id<RemappedMapUpdatePacket> ID = new CustomPayload.Id<>(Identifier.of(Remapped.MOD_ID, "remapped_map_update"));

    public RemappedMapUpdatePacket(MapIdComponent mapId, byte scale, boolean locked, @Nullable Collection<MapDecoration> decorations, @Nullable RemappedState.UpdateData updateData) {
        this(mapId, scale, locked, decorations != null ? Optional.of(List.copyOf(decorations)) : Optional.empty(), Optional.ofNullable(updateData));
    }

    public void apply(RemappedState state) {
        this.decorations.ifPresent(state::replaceDecorations);
        this.updateData.ifPresent((updateData) -> updateData.setColorsTo(state));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
