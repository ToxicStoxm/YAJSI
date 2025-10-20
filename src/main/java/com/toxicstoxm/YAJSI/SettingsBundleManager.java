package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.InvalidConfigurationException;
import com.toxicstoxm.YAJSI.upgrading.UpgradeCallback;
import com.toxicstoxm.YAJSI.upgrading.Version;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;

public class SettingsBundleManager {
    private final HashMap<Version, UpgradeCallback> upgradeCallbacks = new HashMap<>();
    private final HashMap<SettingsBundle, YamlConfiguration> registeredConfigs = new HashMap<>();

    public YamlConfiguration upgrade(@NotNull SettingsBundle bundle, @NotNull YamlConfiguration yaml) {
        Version old = bundle.getVersion().fromString(yaml.getString("Version"));
        int cmp = bundle.getVersion().compareTo(old);
        if (cmp > 0) {
            UpgradeCallback cb = upgradeCallbacks.get(old);
            if (cb == null) {
                throw new IllegalStateException("Unable to find upgradeCallback!");
            }
            return upgrade(bundle, cb.process(yaml, bundle.getId()));
        } else if (cmp == 0) {
            return yaml;
        } else {
            throw new IllegalArgumentException("Downgrading configs is not supported!");
        }
    }

    public void registerUpgradeCallback(UpgradeCallback cb, Version base) {
        upgradeCallbacks.put(base, cb);
    }

    public void registerConfig(SettingsBundle config, YamlConfiguration yaml) {
        registeredConfigs.put(config, yaml);
        loadConfig(config, upgrade(config, yaml));
    }

    public void loadConfig(@NotNull SettingsBundle config, YamlConfiguration yaml) throws InvalidConfigurationException {
        Class<? extends SettingsBundle> clazz = config.getClass();
        HashMap<String, Field> discoveredFields = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(YAMLSetting.class)) {
                YAMLSetting setting = field.getAnnotation(YAMLSetting.class);
                if (discoveredFields.containsKey(setting.path())) {
                    switch (SettingsManager.getInstance().settings.getDuplicatedSettingsStrategy()) {
                        case ERROR -> throw new InvalidConfigurationException("Config cannot have duplicate keys!");
                        case USE_DEFAULTS -> System.out.println("TODO USE_DEFAULTS");
                        case PICK_FIRST -> System.out.println("TODO PICK_FIRST");
                    }
                } else {
                    discoveredFields.put(setting.path(), field);
                }
            }
        }
        System.out.println("Loading " + clazz + " " + config.getId() + " " + config.getVersion() + " " + config.getFile());
    }
}
