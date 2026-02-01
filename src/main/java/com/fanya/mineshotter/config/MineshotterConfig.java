package com.fanya.mineshotter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MineshotterConfig {
    private static final File CONFIG_FILE = new File("config/mineshotter.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum UploaderMode {
        KAPPA_LOL,
        NOIKCLOUD_XYZ,
        HAPKA_LOL,
        CUSTOM
    }

    public UploaderMode mode = UploaderMode.KAPPA_LOL;

    public UploaderProfile customProfile = new UploaderProfile(
            "",
            "file",
            new HashMap<>(),
            "{json:url}"
    );

    // Transient so GSON doesn't save/load it - it's hardcoded logic
    private final transient Map<UploaderMode, UploaderProfile> presets = new HashMap<>();

    public MineshotterConfig() {
        initPresets();
    }

    private void initPresets() {
        presets.put(UploaderMode.KAPPA_LOL, new UploaderProfile(
                "https://kappa.lol/api/upload",
                "file",
                null,
                "{json:link}"
        ));
        presets.put(UploaderMode.NOIKCLOUD_XYZ, new UploaderProfile(
                "https://noikcloud.xyz/upload",
                "file",
                null,
                "{json:url}"
        ));

        Map<String, String> hapkaArgs = new HashMap<>();
        hapkaArgs.put("key", "dac5f11c-728d-402c-86ea-0d7d84d3e372");
        presets.put(UploaderMode.HAPKA_LOL, new UploaderProfile(
                "https://hapka.lol/api/1/upload.php",
                "source",
                hapkaArgs,
                "{json:success.url}"
        ));
    }

    public UploaderProfile getActiveProfile() {
        if (mode == UploaderMode.CUSTOM) {
            return customProfile;
        }
        // Fallback for safety
        if (!presets.containsKey(mode) || presets.isEmpty()) {
             initPresets();
        }
        return presets.get(mode);
    }

    public static class UploaderProfile {
        public String requestUrl;
        public String fileFormName;
        public Map<String, String> arguments;
        public String urlResultPath;

        public UploaderProfile(String requestUrl, String fileFormName, Map<String, String> arguments, String urlResultPath) {
            this.requestUrl = requestUrl;
            this.fileFormName = fileFormName;
            this.arguments = arguments != null ? arguments : new HashMap<>();
            this.urlResultPath = urlResultPath;
        }
    }

    private static MineshotterConfig instance;

    public static MineshotterConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, MineshotterConfig.class);
                if (instance == null) instance = new MineshotterConfig();
                instance.initPresets();
            } catch (Exception e) {
                e.printStackTrace();
                instance = new MineshotterConfig();
                instance.initPresets();
            }
        } else {
            instance = new MineshotterConfig();
            save();
        }
    }

    public static void save() {
        if (instance == null) return;

        File configDir = CONFIG_FILE.getParentFile();
        if (!configDir.exists()) configDir.mkdirs();

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
