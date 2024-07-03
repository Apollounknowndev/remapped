package dev.worldgen.remapped.mixin.client;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.worldgen.remapped.duck.RemappedClientWorldAccess;
import dev.worldgen.remapped.map.RemappedState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Shadow
    private ClientWorld world;

    @Inject(
        method = "onPlayerRespawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getMapStates()Ljava/util/Map;"
        )
    )
    private void remapped$getMapsOnRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci, @Share("states") LocalRef<Map<MapIdComponent, RemappedState>> states) {
        RemappedClientWorldAccess access = (RemappedClientWorldAccess)this.world;
        states.set(access.remapped$getStates());
    }

    @Inject(
        method = "onPlayerRespawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;joinWorld(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/gui/screen/DownloadingTerrainScreen$WorldEntryReason;)V"
        )
    )
    private void remapped$setMapsOnRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci, @Share("states") LocalRef<Map<MapIdComponent, RemappedState>> states) {
        RemappedClientWorldAccess access = (RemappedClientWorldAccess)this.world;
        access.remapped$setStates(states.get());
    }
}
