package dev.worldgen.remapped.mixin;

import dev.worldgen.remapped.Remapped;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(
        method = "getName(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/text/Text;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void remapped$fixVanillaEmptyMapName(ItemStack stack, CallbackInfoReturnable<Text> cir) {
        if (stack.getItem() == Items.MAP && Remapped.enabled()) {
            cir.setReturnValue(Text.translatable("item.remapped.empty_locator_map"));
        }
    }
}
