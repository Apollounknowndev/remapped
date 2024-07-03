package dev.worldgen.remapped.map;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.mojang.datafixers.util.Function7;
import dev.worldgen.remapped.Remapped;
import dev.worldgen.remapped.color.RemappedColor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class RemappedUtils {
    public static final RegistryKey<RemappedColor> EMPTY = of("empty");
    private static final RegistryKey<RemappedColor> DIRT_BROWN = of("dirt_brown");
    private static final RegistryKey<RemappedColor> STONE_GRAY = of("stone_gray");
    private static final RegistryKey<RemappedColor> ORANGE = of("orange");
    private static final RegistryKey<RemappedColor> BROWN = of("brown");

    private static RegistryKey<RemappedColor> of(String name) {
        return RegistryKey.of(RemappedColor.REGISTRY_KEY, Identifier.of(Remapped.MOD_ID, name));
    }

    @Nullable
    public static RemappedState getState(@Nullable MapIdComponent id, World world) {
        if (Remapped.disabled()) return null;

        return id == null ? null : world.remapped$getState(id);
    };

    @Nullable
    public static RemappedState getState(ItemStack map, World world) {
        MapIdComponent id = map.get(DataComponentTypes.MAP_ID);
        return getState(id, world);
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7> PacketCodec<B, C> tuple(final PacketCodec<? super B, T1> codec1, final Function<C, T1> from1, final PacketCodec<? super B, T2> codec2, final Function<C, T2> from2, final PacketCodec<? super B, T3> codec3, final Function<C, T3> from3, final PacketCodec<? super B, T4> codec4, final Function<C, T4> from4, final PacketCodec<? super B, T5> codec5, final Function<C, T5> from5, final PacketCodec<? super B, T6> codec6, final Function<C, T6> from6, final PacketCodec<? super B, T7> codec7, final Function<C, T7> from7, final Function7<T1, T2, T3, T4, T5, T6, T7, C> to) {
        return new PacketCodec<>() {
            public C decode(B object) {
                T1 object1 = codec1.decode(object);
                T2 object2 = codec2.decode(object);
                T3 object3 = codec3.decode(object);
                T4 object4 = codec4.decode(object);
                T5 object5 = codec5.decode(object);
                T6 object6 = codec6.decode(object);
                T7 object7 = codec7.decode(object);
                return to.apply(object1, object2, object3, object4, object5, object6, object7);
            }

            public void encode(B object, C object2) {
                codec1.encode(object, from1.apply(object2));
                codec2.encode(object, from2.apply(object2));
                codec3.encode(object, from3.apply(object2));
                codec4.encode(object, from4.apply(object2));
                codec5.encode(object, from5.apply(object2));
                codec6.encode(object, from6.apply(object2));
                codec7.encode(object, from7.apply(object2));
            }
        };
    }

    public static void updateColors(ServerWorld world, Entity entity, RemappedState state) {
        if (world.getRegistryKey() == state.dimension() && entity instanceof PlayerEntity) {
            Registry<RemappedColor> registry = world.getRegistryManager().get(RemappedColor.REGISTRY_KEY);

            int squaredScale = 1 << state.scale();
            int centerX = state.centerX();
            int centerZ = state.centerZ();
            int l = MathHelper.floor(entity.getX() - (double) centerX) / squaredScale + 64;
            int m = MathHelper.floor(entity.getZ() - (double) centerZ) / squaredScale + 64;
            int n = 128 / squaredScale;
            if (world.getDimension().hasCeiling()) {
                n /= 2;
            }

            RemappedState.PlayerUpdateTracker playerUpdateTracker = state.getPlayerSyncData((PlayerEntity) entity);
            ++playerUpdateTracker.tick;
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            BlockPos.Mutable mutable2 = new BlockPos.Mutable();
            boolean bl = false;

            for (int o = l - n + 1; o < l + n; ++o) {
                if ((o & 15) == (playerUpdateTracker.tick & 15) || bl) {
                    bl = false;
                    double d = 0.0;

                    for (int p = m - n - 1; p < m + n; ++p) {
                        if (o >= 0 && p >= -1 && o < 128 && p < 128) {
                            int q = MathHelper.square(o - l) + MathHelper.square(p - m);
                            boolean bl2 = q > (n - 2) * (n - 2);
                            int r = (centerX / squaredScale + o - 64) * squaredScale;
                            int s = (centerZ / squaredScale + p - 64) * squaredScale;
                            Multiset<RegistryEntry<RemappedColor>> colorsForPixel = LinkedHashMultiset.create();
                            WorldChunk worldChunk = world.getChunk(ChunkSectionPos.getSectionCoord(r), ChunkSectionPos.getSectionCoord(s));
                            if (!worldChunk.isEmpty()) {
                                int t = 0;
                                double e = 0.0;
                                int u;
                                if (world.getDimension().hasCeiling()) {
                                    u = r + s * 231871;
                                    u = u * u * 31287121 + u * 11;
                                    if ((u >> 20 & 1) == 0) {
                                        colorsForPixel.add(get(registry, DIRT_BROWN), 10);
                                    } else {
                                        colorsForPixel.add(get(registry, STONE_GRAY), 100);
                                    }

                                    e = 100.0;
                                } else {
                                    for (u = 0; u < squaredScale; ++u) {
                                        for (int v = 0; v < squaredScale; ++v) {
                                            mutable.set(r + u, 0, s + v);
                                            int w = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, mutable.getX(), mutable.getZ()) + 1;
                                            BlockState blockState;
                                            if (w <= world.getBottomY() + 1) {
                                                blockState = Blocks.BEDROCK.getDefaultState();
                                            } else {
                                                do {
                                                    --w;
                                                    mutable.setY(w);
                                                    blockState = worldChunk.getBlockState(mutable);
                                                } while (getMatchingColor(registry, world.getBiome(mutable), blockState) == get(registry, EMPTY) && w > world.getBottomY());

                                                if (w > world.getBottomY() && !blockState.getFluidState().isEmpty()) {
                                                    int x = w - 1;
                                                    mutable2.set(mutable);

                                                    BlockState blockState2;
                                                    do {
                                                        mutable2.setY(x--);
                                                        blockState2 = worldChunk.getBlockState(mutable2);
                                                        ++t;
                                                    } while (x > world.getBottomY() && !blockState2.getFluidState().isEmpty());

                                                    blockState = getFluidStateIfVisible(world, blockState, mutable);
                                                }
                                            }

                                            state.removeBanner(world, mutable.getX(), mutable.getZ());
                                            e += (double) w / (squaredScale * squaredScale);
                                            colorsForPixel.add(getMatchingColor(registry, world.getBiome(mutable), blockState));
                                        }
                                    }
                                }

                                t /= squaredScale * squaredScale;
                                RegistryEntry<RemappedColor> color = Iterables.getFirst(Multisets.copyHighestCountFirst(colorsForPixel), get(registry, EMPTY));
                                MapColor.Brightness brightness;
                                double f;
                                if (color.value().useDithering()) {
                                    f = (double) t * 0.1 + (double) (o + p & 1) * 0.2;
                                    if (f < 0.5) {
                                        brightness = MapColor.Brightness.HIGH;
                                    } else if (f > 0.9) {
                                        brightness = MapColor.Brightness.LOW;
                                    } else {
                                        brightness = MapColor.Brightness.NORMAL;
                                    }
                                } else {
                                    f = (e - d) * 4.0 / (double) (squaredScale + 4) + ((double) (o + p & 1) - 0.5) * 0.4;
                                    if (f > 0.6) {
                                        brightness = MapColor.Brightness.HIGH;
                                    } else if (f < -0.6) {
                                        brightness = MapColor.Brightness.LOW;
                                    } else {
                                        brightness = MapColor.Brightness.NORMAL;
                                    }
                                }

                                d = e;
                                if (p >= 0 && q < n * n && (!bl2 || (o + p & 1) != 0)) {
                                    bl |= state.putPixel(o, p, color, brightness.id);
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private static boolean isAquaticBiome(boolean[] biomes, int x, int z) {
        return biomes[z * 128 + x];
    }

    public static void fillExplorationMap(ServerWorld world, ItemStack map) {
        RemappedState state = getState(map, world);
        if (state != null) {
            if (world.getRegistryKey() == state.dimension()) {
                Registry<RemappedColor> registry = world.getRegistryManager().get(RemappedColor.REGISTRY_KEY);
                int i = 1 << state.scale();
                int j = state.centerX();
                int k = state.centerZ();
                boolean[] bls = new boolean[16384];
                int l = j / i - 64;
                int m = k / i - 64;
                BlockPos.Mutable mutable = new BlockPos.Mutable();

                int n;
                int o;
                for (n = 0; n < 128; ++n) {
                    for (o = 0; o < 128; ++o) {
                        RegistryEntry<Biome> registryEntry = world.getBiome(mutable.set((l + o) * i, 0, (m + n) * i));
                        bls[n * 128 + o] = registryEntry.isIn(BiomeTags.WATER_ON_MAP_OUTLINES);
                    }
                }

                for (n = 1; n < 127; ++n) {
                    for (o = 1; o < 127; ++o) {
                        int p = 0;

                        for (int q = -1; q < 2; ++q) {
                            for (int r = -1; r < 2; ++r) {
                                if ((q != 0 || r != 0) && isAquaticBiome(bls, n + q, o + r)) {
                                    ++p;
                                }
                            }
                        }

                        MapColor.Brightness brightness = MapColor.Brightness.LOWEST;
                        RegistryKey<RemappedColor> color = EMPTY;
                        if (isAquaticBiome(bls, n, o)) {
                            color = ORANGE;
                            if (p > 7 && o % 2 == 0) {
                                brightness = switch ((n + (int) (MathHelper.sin((float) o + 0.0F) * 7.0F)) / 8 % 5) {
                                    case 0, 4 -> MapColor.Brightness.LOW;
                                    case 1, 3 -> MapColor.Brightness.NORMAL;
                                    case 2 -> MapColor.Brightness.HIGH;
                                    default -> brightness;
                                };
                            } else if (p > 7) {
                                color = EMPTY;
                            } else if (p > 5) {
                                brightness = MapColor.Brightness.NORMAL;
                            } else if (p > 3) {
                                brightness = MapColor.Brightness.LOW;
                            } else if (p > 1) {
                                brightness = MapColor.Brightness.LOW;
                            }
                        } else if (p > 0) {
                            color = BROWN;
                            if (p > 3) {
                                brightness = MapColor.Brightness.NORMAL;
                            }
                        }

                        if (color != EMPTY) {
                            state.putPixel(n, o, registry.getEntry(color).get(), brightness.id);
                        }
                    }
                }

            }
        }
    }

    private static RegistryEntry<RemappedColor> get(Registry<RemappedColor> registry, RegistryKey<RemappedColor> key) {
        return registry.getEntry(key).get();
    }

    private static RegistryEntry<RemappedColor> getMatchingColor(Registry<RemappedColor> registry, RegistryEntry<Biome> biome, BlockState state) {
        for (RegistryEntry<RemappedColor> entry : registry.getIndexedEntries()) {
            RemappedColor color = entry.value();
            boolean blockMatch = state.isIn(color.blocks());
            boolean biomeMatch = color.biomes().size() == 0 || color.biomes().contains(biome);
            if (blockMatch && biomeMatch) {
                return entry;
            }
        }
        return get(registry, EMPTY);
    }

    private static BlockState getFluidStateIfVisible(World world, BlockState state, BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP) ? fluidState.getBlockState() : state;
    }

    public static boolean canZoom(ItemStack first, ItemStack second, World world) {
        RemappedState state = getState(first, world);
        if (state != null && (second.isOf(Items.MAP) || second.isOf(Remapped.EMPTY_MAP))) {
            boolean firstTracks = state.showDecorations();
            boolean secondTracks = second.isOf(Items.MAP);
            return firstTracks == secondTracks;
        }
        return false;
    }
}
