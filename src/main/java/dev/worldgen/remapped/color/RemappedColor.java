package dev.worldgen.remapped.color;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.remapped.Remapped;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public record RemappedColor(int color, boolean useDithering, RegistryEntryList<Block> blocks, RegistryEntryList<Biome> biomes) {
    public static final RegistryKey<Registry<RemappedColor>> REGISTRY_KEY = RegistryKey.ofRegistry(Identifier.of(Remapped.MOD_ID, "color"));
    public static final Codec<RemappedColor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("color").forGetter(RemappedColor::color),
        Codec.BOOL.fieldOf("use_dithering").orElse(false).forGetter(RemappedColor::useDithering),
        RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("blocks").forGetter(RemappedColor::blocks),
        Biome.REGISTRY_ENTRY_LIST_CODEC.fieldOf("biomes").orElse(RegistryEntryList.of()).forGetter(RemappedColor::biomes)
    ).apply(instance, RemappedColor::new));
    public static final Codec<RegistryEntry<RemappedColor>> ENTRY_CODEC = RegistryElementCodec.of(REGISTRY_KEY, CODEC, false);
}
