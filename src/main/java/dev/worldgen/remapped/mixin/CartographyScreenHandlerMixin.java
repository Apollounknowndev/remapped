package dev.worldgen.remapped.mixin;

import dev.worldgen.remapped.Remapped;
import dev.worldgen.remapped.config.ConfigHandler;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CartographyTableScreenHandler.class)
public abstract class CartographyScreenHandlerMixin extends ScreenHandler {

    @Shadow
    @Final
    private ScreenHandlerContext context;

    @Shadow
    @Final
    private CraftingResultInventory resultInventory;

    protected CartographyScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(
        method = "updateResult",
        at = @At("TAIL")
    )
    private void remapped$fixCartographyTable(ItemStack first, ItemStack second, ItemStack oldResult, CallbackInfo ci) {
        this.context.run((world, pos) -> {
            RemappedState state = RemappedUtils.getState(first, world);
            if (state != null) {
                ItemStack output;
                if (second.isOf(Items.PAPER) && !state.locked() && state.scale() < 4) {
                    output = first.copyWithCount(1);
                    output.set(DataComponentTypes.MAP_POST_PROCESSING, MapPostProcessingComponent.SCALE);
                    if (ConfigHandler.scaleMapsFromCenter()) {
                        output.set(Remapped.SCALE_FROM_CENTER, Unit.INSTANCE);
                    }
                    this.sendContentUpdates();
                } else if (second.isOf(Items.GLASS_PANE) && !state.locked()) {
                    output = first.copyWithCount(1);
                    output.set(DataComponentTypes.MAP_POST_PROCESSING, MapPostProcessingComponent.LOCK);
                    this.sendContentUpdates();
                } else {
                    if (!RemappedUtils.canZoom(first, second, world)) {
                        this.resultInventory.removeStack(2);
                        this.sendContentUpdates();
                        return;
                    }

                    output = first.copyWithCount(2);
                    this.sendContentUpdates();
                }

                if (!ItemStack.areEqual(output, oldResult)) {
                    this.resultInventory.setStack(2, output);
                    this.sendContentUpdates();
                }

            }
        });
    }
}
