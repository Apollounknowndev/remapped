package dev.worldgen.remapped.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import dev.worldgen.remapped.network.s2c.MapUpdatePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(
        method = "playerTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/NetworkSyncedItem;createSyncPacket(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/network/packet/Packet;",
            shift = At.Shift.BEFORE
        )
    )
    private void remapped$fixFilledMapPacket(CallbackInfo ci, @Local(ordinal = 0) ItemStack stack) {
        MapIdComponent id = stack.get(DataComponentTypes.MAP_ID);
        RemappedState state = RemappedUtils.getState(id, this.getWorld());

        if (state != null) {
            MapUpdatePacket packet = state.getPlayerMarkerPacket(id, this);
            if (packet != null) {
                ServerPlayNetworking.send(((ServerPlayerEntity)(Object)this), packet);
            }
        }
    }
}
