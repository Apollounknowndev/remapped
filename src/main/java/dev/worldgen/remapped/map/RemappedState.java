package dev.worldgen.remapped.map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.remapped.color.RemappedColor;
import dev.worldgen.remapped.network.s2c.MapUpdatePacket;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapDecorationsComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapBannerMarker;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapFrameMarker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class RemappedState extends PersistentState {
    private static final int SIZE = 128;
    private static final int SIZE_HALF = 64;
    private static final int MAX_DECORATIONS = 256;
    private static final byte MAX_SCALE = 4;

    private static final Codec<Byte> SCALE_CODEC = Codec.BYTE.validate(value -> value >= 0 && value <= MAX_SCALE ? DataResult.success(value) : DataResult.error(() -> "Value must be within range [0, 4]: " + value));
    private static final Codec<List<Integer>> PIXEL_CODEC = Codec.INT.listOf().validate(list -> list.size() == 16384 ? DataResult.success(list) : DataResult.error(() -> "List should have 16384 entries, got "+list.size()));
    private static final Codec<MapFrameMarker> FRAME_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(MapFrameMarker::getPos),
        Codec.INT.fieldOf("rotation").forGetter(MapFrameMarker::getRotation),
        Codec.INT.fieldOf("entity_id").forGetter(MapFrameMarker::getEntityId)
    ).apply(instance, MapFrameMarker::new));
    private static final Codec<RemappedState> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        World.CODEC.fieldOf("dimension").forGetter(RemappedState::dimension),
        Codec.INT.fieldOf("center_x").orElse(0).forGetter(RemappedState::centerX),
        Codec.INT.fieldOf("center_z").orElse(0).forGetter(RemappedState::centerZ),
        SCALE_CODEC.fieldOf("scale").orElse((byte)0).forGetter(RemappedState::scale),
        Codec.BOOL.fieldOf("show_decorations").orElse(true).forGetter(RemappedState::showDecorations),
        Codec.BOOL.fieldOf("unlimited_tracking").orElse(true).forGetter(RemappedState::unlimitedTracking),
        Codec.BOOL.fieldOf("locked").orElse(false).forGetter(RemappedState::locked),
        PIXEL_CODEC.fieldOf("colors").orElse(getDefaultPixels()).forGetter(RemappedState::colors),
        MapBannerMarker.LIST_CODEC.fieldOf("banners").orElse(List.of()).forGetter(RemappedState::banners),
        FRAME_CODEC.listOf().fieldOf("frames").forGetter(RemappedState::frames)
    ).apply(instance, RemappedState::new));

    private static final Codec<RemappedState> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        World.CODEC.fieldOf("dimension").forGetter(RemappedState::dimension),
        Codec.INT.fieldOf("xCenter").orElse(0).forGetter(RemappedState::centerX),
        Codec.INT.fieldOf("zCenter").orElse(0).forGetter(RemappedState::centerZ),
        SCALE_CODEC.fieldOf("scale").orElse((byte)0).forGetter(RemappedState::scale),
        Codec.BOOL.fieldOf("trackingPosition").orElse(true).forGetter(RemappedState::showDecorations),
        Codec.BOOL.fieldOf("unlimitedTracking").orElse(true).forGetter(RemappedState::unlimitedTracking),
        Codec.BOOL.fieldOf("locked").orElse(false).forGetter(RemappedState::locked)
    ).apply(instance, RemappedState::new));

    private static final Codec<RemappedState> CODEC = Codec.withAlternative(BASE_CODEC, LEGACY_CODEC);

    private final RegistryKey<World> dimension;
    private final int centerX;
    private final int centerZ;
    private final byte scale;
    private final boolean showDecorations;
    private final boolean unlimitedTracking;
    private final boolean locked;
    public List<Integer> colors;
    private final Map<String, MapBannerMarker> trackedBanners = Maps.newHashMap();
    private final Map<String, MapFrameMarker> trackedFrames = Maps.newHashMap();

    final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
    private int decorationCount;

    private final List<RemappedState.PlayerUpdateTracker> updateTrackers = Lists.newArrayList();
    private final Map<PlayerEntity, RemappedState.PlayerUpdateTracker> updateTrackersByPlayer = Maps.newHashMap();

    public static PersistentState.Type<RemappedState> getPersistentStateType() {
        return new PersistentState.Type<>(() -> {
            throw new IllegalStateException("Should never create an empty map saved data");
        }, RemappedState::fromNbt, null);
    }

    private RemappedState(RegistryKey<World> dimension, int centerX, int centerZ, byte scale, boolean showDecorations, boolean unlimitedTracking, boolean locked) {
        this(dimension, centerX, centerZ, scale, showDecorations, unlimitedTracking, locked, getDefaultPixels(), List.of(), List.of());
    }

    private RemappedState(RegistryKey<World> dimension, int centerX, int centerZ, byte scale, boolean showDecorations, boolean unlimitedTracking, boolean locked, List<Integer> colors, List<MapBannerMarker> banners, List<MapFrameMarker> frames) {
        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.scale = scale;
        this.showDecorations = showDecorations;
        this.unlimitedTracking = unlimitedTracking;
        this.locked = locked;
        this.colors = new ArrayList<>(colors);

        for (MapBannerMarker marker : banners) {
            this.trackedBanners.put(marker.getKey(), marker);
            this.addDecoration(marker.getDecorationType(), null, marker.getKey(), marker.pos().getX(), marker.pos().getZ(), 180.0, marker.name().orElse(null));
        }

        for (MapFrameMarker marker : frames) {
            this.trackedFrames.put(marker.getKey(), marker);
            this.addDecoration(MapDecorationTypes.FRAME, null, getFrameDecorationKey(marker.getEntityId()), marker.getPos().getX(), marker.getPos().getZ(), marker.getRotation(), null);
        }

        this.markDirty();
    }

    public int getPixelColor(int index) {
        return this.colors.get(index);
    }

    public int centerX() {
        return this.centerX;
    }

    public int centerZ() {
        return this.centerZ;
    }

    public byte scale() {
        return this.scale;
    }

    public boolean showDecorations() {
        return this.showDecorations;
    }

    public boolean unlimitedTracking() {
        return this.unlimitedTracking;
    }

    public boolean locked() {
        return this.locked;
    }

    public RegistryKey<World> dimension() {
        return this.dimension;
    }
    public List<Integer> colors() {
        return this.colors;
    }

    public List<MapBannerMarker> banners() {
        return this.trackedBanners.values().stream().toList();
    }

    public List<MapFrameMarker> frames() {
        return this.trackedFrames.values().stream().toList();
    }

    private static List<Integer> getDefaultPixels() {
        Integer[] pixels = new Integer[16384];
        Arrays.fill(pixels, 0);
        return Arrays.asList(pixels);
    }

    public static RemappedState of(int centerX, int centerZ, byte scale, boolean showDecorations, boolean unlimitedTracking, RegistryKey<World> dimension, boolean adjustCenter) {
        int i = SIZE * (1 << scale);
        int j = MathHelper.floor((double) (centerX + SIZE_HALF) / i);
        int k = MathHelper.floor((double) (centerZ + SIZE_HALF) / i);
        int l = j * i + i / 2 - SIZE_HALF;
        int m = k * i + i / 2 - SIZE_HALF;

        int newX = adjustCenter ? l : centerX;
        int newZ = adjustCenter ? m : centerZ;
        return new RemappedState(dimension, newX, newZ, scale, showDecorations, unlimitedTracking, false, getDefaultPixels(), List.of(), List.of());
    }

    public static RemappedState of(byte scale, boolean locked, RegistryKey<World> dimension) {
        return new RemappedState(dimension, 0, 0, scale, false, false, locked, getDefaultPixels(), List.of(), List.of());
    }

    public static RemappedState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        return CODEC.parse(registryLookup.getOps(NbtOps.INSTANCE), nbt).getOrThrow();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtElement element = BASE_CODEC.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), this).result().orElseThrow();
        return element instanceof NbtCompound compound ? compound : nbt;
    }


    public RemappedState copy() {
        RemappedState remappedState = new RemappedState(this.dimension, this.centerX, this.centerZ, this.scale, this.showDecorations, this.unlimitedTracking, true, this.colors, this.trackedBanners.values().stream().toList(), this.trackedFrames.values().stream().toList());
        remappedState.markDirty();
        return remappedState;
    }

    public RemappedState zoomOut(boolean scaleFromCenter) {
        return of(this.centerX, this.centerZ, (byte)MathHelper.clamp(this.scale + 1, 0, MAX_SCALE), this.showDecorations, this.unlimitedTracking, this.dimension, !scaleFromCenter);
    }

    private static Predicate<ItemStack> getEqualPredicate(ItemStack stack) {
        MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
        return (other) -> {
            if (other == stack) {
                return true;
            } else {
                return other.isOf(stack.getItem()) && Objects.equals(mapIdComponent, other.get(DataComponentTypes.MAP_ID));
            }
        };
    }

    public void update(PlayerEntity player, ItemStack stack) {
        if (!this.updateTrackersByPlayer.containsKey(player)) {
            RemappedState.PlayerUpdateTracker playerUpdateTracker = new RemappedState.PlayerUpdateTracker(player);
            this.updateTrackersByPlayer.put(player, playerUpdateTracker);
            this.updateTrackers.add(playerUpdateTracker);
        }

        Predicate<ItemStack> predicate = getEqualPredicate(stack);
        if (!player.getInventory().contains(predicate)) {
            this.removeDecoration(player.getName().getString());
        }

        for(int i = 0; i < this.updateTrackers.size(); ++i) {
            RemappedState.PlayerUpdateTracker playerUpdateTracker2 = this.updateTrackers.get(i);
            String string = playerUpdateTracker2.player.getName().getString();
            if (playerUpdateTracker2.player.isRemoved() || !playerUpdateTracker2.player.getInventory().contains(predicate) && !stack.isInFrame()) {
                this.updateTrackersByPlayer.remove(playerUpdateTracker2.player);
                this.updateTrackers.remove(playerUpdateTracker2);
                this.removeDecoration(string);
            } else if (!stack.isInFrame() && playerUpdateTracker2.player.getWorld().getRegistryKey() == this.dimension && this.showDecorations) {
                this.addDecoration(MapDecorationTypes.PLAYER, playerUpdateTracker2.player.getWorld(), string, playerUpdateTracker2.player.getX(), playerUpdateTracker2.player.getZ(), playerUpdateTracker2.player.getYaw(), null);
            }
        }

        if (stack.isInFrame() && this.showDecorations) {
            ItemFrameEntity itemFrameEntity = stack.getFrame();
            BlockPos blockPos = itemFrameEntity.getAttachedBlockPos();
            MapFrameMarker mapFrameMarker = this.trackedFrames.get(MapFrameMarker.getKey(blockPos));
            if (mapFrameMarker != null && itemFrameEntity.getId() != mapFrameMarker.getEntityId() && this.trackedFrames.containsKey(mapFrameMarker.getKey())) {
                this.removeDecoration(getFrameDecorationKey(mapFrameMarker.getEntityId()));
            }

            MapFrameMarker mapFrameMarker2 = new MapFrameMarker(blockPos, itemFrameEntity.getHorizontalFacing().getHorizontal() * 90, itemFrameEntity.getId());
            this.addDecoration(MapDecorationTypes.FRAME, player.getWorld(), getFrameDecorationKey(itemFrameEntity.getId()), blockPos.getX(), blockPos.getZ(), itemFrameEntity.getHorizontalFacing().getHorizontal() * 90, null);
            this.trackedFrames.put(mapFrameMarker2.getKey(), mapFrameMarker2);
        }

        MapDecorationsComponent mapDecorationsComponent = stack.getOrDefault(DataComponentTypes.MAP_DECORATIONS, MapDecorationsComponent.DEFAULT);
        if (!this.decorations.keySet().containsAll(mapDecorationsComponent.decorations().keySet())) {
            mapDecorationsComponent.decorations().forEach((id, decoration) -> {
                if (!this.decorations.containsKey(id)) {
                    this.addDecoration(decoration.type(), player.getWorld(), id, decoration.x(), decoration.z(), decoration.rotation(), null);
                }

            });
        }

    }

    private void removeDecoration(String id) {
        MapDecoration mapDecoration = this.decorations.remove(id);
        if (mapDecoration != null && mapDecoration.type().value().trackCount()) {
            --this.decorationCount;
        }

        this.markDecorationsDirty();
    }

    private void addDecoration(RegistryEntry<MapDecorationType> type, @Nullable WorldAccess world, String key, double x, double z, double rotation, @Nullable Text text) {
        int i = 1 << this.scale;
        float f = (float)(x - (double)this.centerX) / (float)i;
        float g = (float)(z - (double)this.centerZ) / (float)i;
        byte b = (byte)((int)((double)(f * 2.0F) + 0.5));
        byte c = (byte)((int)((double)(g * 2.0F) + 0.5));
        byte d;
        if (f >= -63.0F && g >= -63.0F && f <= 63.0F && g <= 63.0F) {
            rotation += rotation < 0.0 ? -8.0 : 8.0;
            d = (byte)((int)(rotation * 16.0 / 360.0));
            if (this.dimension == World.NETHER && world != null) {
                int k = (int)(world.getLevelProperties().getTimeOfDay() / 10L);
                d = (byte)(k * k * 34187121 + k * 121 >> 15 & 15);
            }
        } else {
            if (!type.matches(MapDecorationTypes.PLAYER)) {
                this.removeDecoration(key);
                return;
            }

            if (Math.abs(f) < 320.0F && Math.abs(g) < 320.0F) {
                type = MapDecorationTypes.PLAYER_OFF_MAP;
            } else {
                if (!this.unlimitedTracking) {
                    this.removeDecoration(key);
                    return;
                }

                type = MapDecorationTypes.PLAYER_OFF_LIMITS;
            }

            d = 0;
            if (f <= -63.0F) {
                b = -128;
            }

            if (g <= -63.0F) {
                c = -128;
            }

            if (f >= 63.0F) {
                b = 127;
            }

            if (g >= 63.0F) {
                c = 127;
            }
        }

        MapDecoration mapDecoration = new MapDecoration(type, b, c, d, Optional.ofNullable(text));
        MapDecoration mapDecoration2 = this.decorations.put(key, mapDecoration);
        if (!mapDecoration.equals(mapDecoration2)) {
            if (mapDecoration2 != null && mapDecoration2.type().value().trackCount()) {
                --this.decorationCount;
            }

            if (type.value().trackCount()) {
                ++this.decorationCount;
            }

            this.markDecorationsDirty();
        }

    }

    @Nullable
    public MapUpdatePacket getPlayerMarkerPacket(MapIdComponent mapId, PlayerEntity player) {
        RemappedState.PlayerUpdateTracker playerUpdateTracker = this.updateTrackersByPlayer.get(player);
        return playerUpdateTracker == null ? null : playerUpdateTracker.getPacket(mapId);
    }

    private void markDirty(int x, int z) {
        this.markDirty();

        for (PlayerUpdateTracker playerUpdateTracker : this.updateTrackers) {
            playerUpdateTracker.markDirty(x, z);
        }
    }

    private void markDecorationsDirty() {
        this.markDirty();
        this.updateTrackers.forEach(RemappedState.PlayerUpdateTracker::markDecorationsDirty);
    }

    public RemappedState.PlayerUpdateTracker getPlayerSyncData(PlayerEntity player) {
        RemappedState.PlayerUpdateTracker playerUpdateTracker = this.updateTrackersByPlayer.get(player);
        if (playerUpdateTracker == null) {
            playerUpdateTracker = new RemappedState.PlayerUpdateTracker(player);
            this.updateTrackersByPlayer.put(player, playerUpdateTracker);
            this.updateTrackers.add(playerUpdateTracker);
        }

        return playerUpdateTracker;
    }

    public boolean addBanner(WorldAccess world, BlockPos pos) {
        double d = pos.getX() + 0.5;
        double e = pos.getZ() + 0.5;
        int i = 1 << this.scale;
        double f = (d - this.centerX) / i;
        double g = (e - this.centerZ) / i;
        if (f >= -63.0 && g >= -63.0 && f <= 63.0 && g <= 63.0) {
            MapBannerMarker mapBannerMarker = MapBannerMarker.fromWorldBlock(world, pos);
            if (mapBannerMarker == null) {
                return false;
            }

            if (this.trackedBanners.remove(mapBannerMarker.getKey(), mapBannerMarker)) {
                this.removeDecoration(mapBannerMarker.getKey());
                return true;
            }

            if (!this.decorationCountNotLessThan(MAX_DECORATIONS)) {
                this.trackedBanners.put(mapBannerMarker.getKey(), mapBannerMarker);
                this.addDecoration(mapBannerMarker.getDecorationType(), world, mapBannerMarker.getKey(), d, e, 180.0, mapBannerMarker.name().orElse(null));
                return true;
            }
        }

        return false;
    }

    public void removeBanner(BlockView world, int x, int z) {
        Iterator<MapBannerMarker> iterator = this.trackedBanners.values().iterator();

        while(iterator.hasNext()) {
            MapBannerMarker mapBannerMarker = iterator.next();
            if (mapBannerMarker.pos().getX() == x && mapBannerMarker.pos().getZ() == z) {
                MapBannerMarker mapBannerMarker2 = MapBannerMarker.fromWorldBlock(world, mapBannerMarker.pos());
                if (!mapBannerMarker.equals(mapBannerMarker2)) {
                    iterator.remove();
                    this.removeDecoration(mapBannerMarker.getKey());
                }
            }
        }

    }

    public void removeFrame(BlockPos pos, int id) {
        this.removeDecoration(getFrameDecorationKey(id));
        this.trackedFrames.remove(MapFrameMarker.getKey(pos));
    }

    public boolean putPixel(int x, int z, RegistryEntry<RemappedColor> entry, int brightness) {
        int color = applyBrightness(entry.value().color(), brightness);

        int index = x + z * 128;
        if (this.colors.get(index) != color) {
            this.setPixel(x, z, color);
            return true;
        } else {
            return false;
        }
    }

    public void setPixel(int x, int z, int color) {
        this.colors.set(x + z * 128, color);
        this.markDirty(x, z);
    }

    private static int applyBrightness(int color, int brightness) {
        int i = switch (brightness) {
            case 0 -> 180;
            case 1 -> 220;
            case 2 -> 255;
            case 3 -> 135;
            default -> 0;
        };

        int j = (color >> 16 & 255) * i / 255;
        int k = (color >> 8 & 255) * i / 255;
        int l = (color & 255) * i / 255;

        return -16777216 | l << 16 | k << 8 | j;
    }

    public boolean hasExplorationMapDecoration() {
        Iterator<MapDecoration> var1 = this.decorations.values().iterator();

        MapDecoration mapDecoration;
        do {
            if (!var1.hasNext()) {
                return false;
            }

            mapDecoration =var1.next();
        } while(!mapDecoration.type().value().explorationMapElement());

        return true;
    }

    public void replaceDecorations(List<MapDecoration> decorations) {
        this.decorations.clear();
        this.decorationCount = 0;

        for(int i = 0; i < decorations.size(); ++i) {
            MapDecoration mapDecoration = decorations.get(i);
            this.decorations.put("icon-" + i, mapDecoration);
            if (mapDecoration.type().value().trackCount()) {
                ++this.decorationCount;
            }
        }

    }

    public Iterable<MapDecoration> getDecorations() {
        return this.decorations.values();
    }

    public boolean decorationCountNotLessThan(int decorationCount) {
        return this.decorationCount >= decorationCount;
    }

    private static String getFrameDecorationKey(int id) {
        return "frame-" + id;
    }

    public class PlayerUpdateTracker {
        public final PlayerEntity player;
        private boolean dirty = true;
        private int startX;
        private int startZ;
        private int endX = 127;
        private int endZ = 127;
        private boolean decorationsDirty = true;
        private int emptyPacketsRequested;
        public int tick;

        PlayerUpdateTracker(final PlayerEntity player) {
            this.player = player;
        }

        private RemappedState.UpdateData getMapUpdateData() {
            int i = this.startX;
            int j = this.startZ;
            int k = this.endX + 1 - this.startX;
            int l = this.endZ + 1 - this.startZ;
            List<Integer> colors = fillList(k, l);

            for(int m = 0; m < k; ++m) {
                for(int n = 0; n < l; ++n) {
                    int index = i + m + (j + n) * 128;

                    colors.set(m + n * k, RemappedState.this.colors.get(index));
                }
            }

            return new RemappedState.UpdateData(i, j, k, l, colors);
        }

        private static List<Integer> fillList(int x, int y) {
            Integer[] pixels = new Integer[x * y];
            Arrays.fill(pixels, 0);
            return Arrays.asList(pixels);
        }

        @Nullable
        MapUpdatePacket getPacket(MapIdComponent mapId) {
            RemappedState.UpdateData updateData;
            if (this.dirty) {
                this.dirty = false;
                updateData = this.getMapUpdateData();
            } else {
                updateData = null;
            }

            Collection<MapDecoration> collection;
            if (this.decorationsDirty && this.emptyPacketsRequested++ % 5 == 0) {
                this.decorationsDirty = false;
                collection = RemappedState.this.decorations.values();
            } else {
                collection = null;
            }

            return collection == null && updateData == null ? null : new MapUpdatePacket(mapId, RemappedState.this.scale, RemappedState.this.locked, collection, updateData);
        }

        void markDirty(int startX, int startZ) {
            if (this.dirty) {
                this.startX = Math.min(this.startX, startX);
                this.startZ = Math.min(this.startZ, startZ);
                this.endX = Math.max(this.endX, startX);
                this.endZ = Math.max(this.endZ, startZ);
            } else {
                this.dirty = true;
                this.startX = startX;
                this.startZ = startZ;
                this.endX = startX;
                this.endZ = startZ;
            }

        }

        private void markDecorationsDirty() {
            this.decorationsDirty = true;
        }
    }

    public record UpdateData(int startX, int startZ, int width, int height, List<Integer> colors) {
        private static final PacketCodec<RegistryByteBuf, RemappedState.UpdateData> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, UpdateData::startX,
            PacketCodecs.VAR_INT, UpdateData::startZ,
            PacketCodecs.VAR_INT, UpdateData::width,
            PacketCodecs.VAR_INT, UpdateData::height,
            PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), UpdateData::colors,
            UpdateData::new
        );

        public static final PacketCodec<RegistryByteBuf, Optional<UpdateData>> OPTIONAL_CODEC = new PacketCodec<>() {
            @Override
            public Optional<UpdateData> decode(RegistryByteBuf buf) {
                boolean bl = buf.readBoolean();
                return bl ? Optional.of(CODEC.decode(buf)) : Optional.empty();
            }

            @Override
            public void encode(RegistryByteBuf buf, Optional<UpdateData> value) {
                buf.writeBoolean(value.isPresent());
                value.ifPresent(updateData -> CODEC.encode(buf, updateData));
            }
        };

        public void setColorsTo(RemappedState state) {
            for(int i = 0; i < this.width; ++i) {
                for(int j = 0; j < this.height; ++j) {
                    state.setPixel(this.startX + i, this.startZ + j, this.colors.get(i + j * this.width));
                }
            }
        }
    }
}
