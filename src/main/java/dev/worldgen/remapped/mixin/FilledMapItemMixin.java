package dev.worldgen.remapped.mixin;

import dev.worldgen.remapped.Remapped;
import dev.worldgen.remapped.color.RemappedColor;
import dev.worldgen.remapped.duck.RemappedWorldAccess;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static net.minecraft.item.FilledMapItem.getIdText;

@Mixin(FilledMapItem.class)
public class FilledMapItemMixin {

    /**
     * Cancels most functionality of regular MapState
     */
    @Inject(
        method = "getMapState(Lnet/minecraft/component/type/MapIdComponent;Lnet/minecraft/world/World;)Lnet/minecraft/item/map/MapState;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void remapped$neverGetMapState(MapIdComponent id, World world, CallbackInfoReturnable<MapState> cir) {
        if (Remapped.disabled()) return;

        cir.setReturnValue(null);
    }

    /**
     * Don't create a MapState instance, redirect to RemappedState
     */
    @Inject(
        method = "allocateMapId",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void remapped$reallocateMapId(World world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, RegistryKey<World> dimension, CallbackInfoReturnable<MapIdComponent> cir) {
        if (Remapped.disabled()) return;

        RemappedState state = RemappedState.of(x, z, (byte) scale, showIcons, unlimitedTracking, dimension, true);
        MapIdComponent mapIdComponent = world.increaseAndGetMapId();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getServer().getOverworld().getPersistentStateManager().set(mapIdComponent.asString(), state);
        }
        cir.setReturnValue(mapIdComponent);
    }

    /**
     * Update colors in the RemappedState instance
     */
    @Inject(
        method = "inventoryTick",
        at = @At("HEAD")
    )
    private void remapped$updateRemappedColors(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if (!world.isClient) {
            RemappedState state = RemappedUtils.getState(stack, world);
            if (state != null) {
                if (entity instanceof PlayerEntity player) {
                    state.update(player, stack);
                }

                if (!state.locked() && (selected || entity instanceof PlayerEntity && ((PlayerEntity)entity).getOffHandStack() == stack)) {
                    RemappedUtils.updateColors((ServerWorld) world, entity, state);
                }

            }
        }
    }

    /**
     * Fix tooltip
     */
    @Inject(
        method = "appendTooltip",
        at = @At("HEAD"),
        cancellable = true
    )
    private void remapped$fixAdvancedTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
        if (Remapped.disabled()) return;

        World world = null;
        try {
            world = ((TooltipContextAccessor)context).world();
        } catch (Exception e) {
            ci.cancel();
        }
        if (world == null) ci.cancel();

        MapIdComponent id = stack.get(DataComponentTypes.MAP_ID);
        RemappedState state = id != null ? world.remapped$getState(id) : null;
        MapPostProcessingComponent processing = stack.get(DataComponentTypes.MAP_POST_PROCESSING);
        if (state != null && (state.locked() || processing == MapPostProcessingComponent.LOCK)) {
            tooltip.add(Text.translatable("filled_map.locked", id.id()).formatted(Formatting.GRAY));
        }

        if (type.isAdvanced()) {
            if (state != null) {
                if (processing == null) {
                    tooltip.add(getIdText(id));
                }

                int i = processing == MapPostProcessingComponent.SCALE ? 1 : 0;
                int j = Math.min(state.scale() + i, 4);
                tooltip.add(Text.translatable("filled_map.scale", 1 << j).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("filled_map.level", j, 4).formatted(Formatting.GRAY));
            } else {
                tooltip.add(Text.translatable("filled_map.unknown").formatted(Formatting.GRAY));
            }
        }
        ci.cancel();
    }

    @Inject(
        method = "fillExplorationMap",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void remapped$fixExplorationMap(ServerWorld world, ItemStack map, CallbackInfo ci) {
        if (Remapped.disabled()) return;

        RemappedUtils.fillExplorationMap(world, map);
        ci.cancel();
    }

    @Inject(
        method = "scale",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void remapped$fixScaling(ItemStack stack, World world, CallbackInfo ci) {
        if (Remapped.disabled()) return;

        RemappedState state = RemappedUtils.getState(stack, world);
        if (state != null) {
            MapIdComponent id = world.increaseAndGetMapId();
            world.remapped$setState(id, state.zoomOut(stack.get(Remapped.SCALE_FROM_CENTER) != null));
            stack.set(DataComponentTypes.MAP_ID, id);
        }
        ci.cancel();
    }

    @Inject(
        method = "copyMap",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void remapped$fixCopying(World world, ItemStack stack, CallbackInfo ci) {
        if (Remapped.disabled()) return;

        RemappedState state = RemappedUtils.getState(stack, world);
        if (state != null) {
            MapIdComponent id = world.increaseAndGetMapId();
            world.remapped$setState(id, state.copy());
            stack.set(DataComponentTypes.MAP_ID, id);
        }
        ci.cancel();
    }

    @Inject(
        method = "useOnBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/FilledMapItem;getMapState(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;)Lnet/minecraft/item/map/MapState;"
        ),
        cancellable = true
    )
    private void remapped$fixBannerMarkers(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        RemappedState state = RemappedUtils.getState(context.getStack(), context.getWorld());
        if (state != null && !state.addBanner(context.getWorld(), context.getBlockPos())) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
