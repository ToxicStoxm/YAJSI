package com.toxicstoxm.YAJSI;


import com.toxicstoxm.YAJSI.yaml.InvalidConfigurationException;
import com.toxicstoxm.YAJSI.yaml.file.YamlConfiguration;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LEDSuiteSettingsManager {
    private final ConcurrentHashMap<String, Setting<Object>> settings = new ConcurrentHashMap<>();
    public record ConfigFile(String file, Class<? extends SettingsBundle> settingsBundle) {}
    private final List<ConfigFile> configFiles;


    public static void main(String[] args) {
        System.out.println("YAJSI (Yet another Java settings implementation) is a library and can't be used as a standalone!");
    }

    public LEDSuiteSettingsManager(Collection<ConfigFile> configFiles) {
        if (configFiles == null) this.configFiles = new ArrayList<>();
        else this.configFiles = configFiles.stream().toList();
        load();
    }

    public static LEDSuiteSettingsManager withConfigFile(String file, Class<? extends SettingsBundle> settingsBundle) {
        return new LEDSuiteSettingsManager(Collections.singleton(new ConfigFile(file, settingsBundle)));
    }

    public static LEDSuiteSettingsManager withConfigFile(ConfigFile configFile) {
        return new LEDSuiteSettingsManager((Collections.singleton(configFile)));
    }

    @SafeVarargs
    public static LEDSuiteSettingsManager withConfigFile(String file, Class<? extends SettingsBundle>... settingsBundles) {
        if (file != null && settingsBundles != null) {
            return withConfigFile(file, settingsBundles);
        } return new LEDSuiteSettingsManager(null);
    }

    public static LEDSuiteSettingsManager withConfigFile(String file, Collection<Class<? extends SettingsBundle>> configFiles) {
        List<ConfigFile> configFileList = new ArrayList<>();
        for (Class<? extends SettingsBundle> settingsBundle : configFiles) {
            configFileList.add(new ConfigFile(file, settingsBundle));
        }
        return new LEDSuiteSettingsManager(configFileList);
    }

    public static LEDSuiteSettingsManager withConfigFiles(Map<String, Class<? extends SettingsBundle>> configFiles) {
        List<ConfigFile> configFileList = new ArrayList<>();
        for (Map.Entry<String, Class<? extends SettingsBundle>> configFile : configFiles.entrySet()) {
            configFileList.add(new ConfigFile(configFile.getKey(), configFile.getValue()));
        }
        return new LEDSuiteSettingsManager(configFileList);
    }

    public static LEDSuiteSettingsManager withConfigFiles(Collection<ConfigFile> configFiles) {
        return new LEDSuiteSettingsManager(configFiles);
    }

    @SneakyThrows
    public void load() {
        for (ConfigFile configFile : configFiles) {
            YamlConfiguration yaml = new YamlConfiguration();
            try {
                yaml.load(configFile.file);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }

            for (String key : yaml.getKeys(true)) {
                settings.put(key, new LEDSuiteSetting<>(yaml.get(key)));
            }
            new SettingsHandler<>().loadSettings(configFile.settingsBundle, settings::get);
        }
    }

    public <T> T getSetting(Class<? extends LEDSuiteSetting<T>> settingClass) {
        try {
            // Find the inner class by name
            String path = settingClass.getAnnotation(YAMLSetting.class).path();
            // Access the setting from the map and cast it
            return settingClass.cast(settingClass.getConstructor(Setting.class).newInstance(get(path))).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get setting for: " + settingClass.getName(), e);
        }
    }

    public Setting<Object> get(String s) {
        return settings.get(s);
    }

    @SneakyThrows
    public void save() {
        for (ConfigFile configFile : configFiles) {
            YamlConfiguration yaml = new YamlConfiguration();
            try {
                yaml.load(configFile.file);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
            // Save settings via SettingsManager
            boolean shouldSave = new SettingsHandler<>().saveSettings(configFile.settingsBundle, yaml);

            if (shouldSave) {
                try {
                    yaml.save(configFile.file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
