package dev.worldgen.remapped.mixin;

import dev.worldgen.remapped.Remapped;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.screen.CartographyTableScreenHandler$4"})
public class CartographyScreenHandlerSlotMixin {
    @Inject(
        method = "canInsert",
        at = @At("HEAD"),
        cancellable = true
    )
    private void remapped$fixMapCloning(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isOf(Remapped.EMPTY_MAP_ITEM)) {
            cir.setReturnValue(true);
        }
    }
}
