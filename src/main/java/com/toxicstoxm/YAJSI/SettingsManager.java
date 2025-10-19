package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.upgrading.UpgradeCallback;
import com.toxicstoxm.YAJSI.upgrading.Version;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class SettingsManager {
    private static SettingsManager instance;

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager(SettingsManagerConfig.getDefaults());
        }
        return instance;
    }

    public static SettingsManagerBlueprint configure() {
        return new SettingsManagerBlueprint();
    }

    public static class SettingsManagerBlueprint extends SettingsManagerConfig.SettingsManagerConfigBuilder {
        @Override
        public SettingsManagerConfig done() {
            SettingsManagerConfig conf = super.done();
            instance = new SettingsManager(conf);
            return conf;
        }
    }

    private final SettingsManagerConfig settings;

    private final HashMap<Class<? extends SettingsBundle>, SettingsBundleManager> registeredBundles = new HashMap<>();

    private SettingsManager(SettingsManagerConfig settings) {
        this.settings = settings;
    }

    public void registerUpgradeCallback(Class<? extends SettingsBundle> bundle, UpgradeCallback cb, Version base) {
        getBundleManager(bundle).registerUpgradeCallback(cb, base);
    }

    public UUID registerConfig(@NotNull SettingsBundle config) {
        getBundleManager(config.getClass()).registerConfig(config, getFile(config));
        return config.getId();
    }

    private @NotNull YamlConfiguration getFile(@NotNull SettingsBundle config) {
        File configFile = config.getFile();

        // Ensure parent directories exist
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);

        if (!configFile.exists()) {
            try {
                yaml.save(configFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create configuration file: " + configFile, e);
            }
        }

        return yaml;
    }


    private SettingsBundleManager getBundleManager(Class<? extends SettingsBundle> bundle) {
        if (!registeredBundles.containsKey(bundle)) {
            registeredBundles.put(bundle, new SettingsBundleManager());
        }
        return registeredBundles.get(bundle);
    }
}
