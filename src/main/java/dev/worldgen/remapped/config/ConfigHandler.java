package dev.worldgen.remapped.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.worldgen.remapped.Remapped;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigHandler {
    private static final Codec<Boolean> CODEC = Codec.BOOL.fieldOf("scale_maps_from_center").codec();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("remapped.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean scaleMapsFromCenter;

    public static void load() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            writeDefault();
        }
        try {
            JsonElement json = JsonParser.parseString(new String(Files.readAllBytes(CONFIG_PATH)));
            var dataResult = CODEC.parse(JsonOps.INSTANCE, json);
            dataResult.ifError(error -> {
                Remapped.LOGGER.error("Config file has missing or invalid data: "+error.message());
                scaleMapsFromCenter = true;
            });
            if (dataResult.result().isPresent()) {
                scaleMapsFromCenter = dataResult.result().get();
            }
        } catch (IOException e) {
            Remapped.LOGGER.error("Malformed json in config file found, default config will be used");
            scaleMapsFromCenter = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean scaleMapsFromCenter() {
        return scaleMapsFromCenter;
    }

    private static void writeDefault() {
        try(BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
            JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, true).getOrThrow();
            writer.write(GSON.toJson(json));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        scaleMapsFromCenter = true;
    }
}
