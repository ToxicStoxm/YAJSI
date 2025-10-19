package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.upgrading.UpgradeCallback;
import com.toxicstoxm.YAJSI.upgrading.Version;
import org.jetbrains.annotations.NotNull;

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

    public void loadConfig(@NotNull SettingsBundle config, YamlConfiguration yaml) {
        Class<? extends SettingsBundle> clazz = config.getClass();

        System.out.println("Loading " + clazz + " " + config.getId() + " " + config.getVersion() + " " + config.getFile());
    }
}
