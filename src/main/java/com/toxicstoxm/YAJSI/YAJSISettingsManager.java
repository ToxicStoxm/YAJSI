package com.toxicstoxm.YAJSI;


import com.toxicstoxm.YAJSI.yaml.InvalidConfigurationException;
import com.toxicstoxm.YAJSI.yaml.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YAJSISettingsManager {
    private final ConcurrentHashMap<String, Setting<Object>> settings = new ConcurrentHashMap<>();
    public record ConfigFile(String path, URL defaultFile) {}
    public record YAMLConfig(ConfigFile configFile, Class<? extends SettingsBundle> settingsBundle) {}
    private final List<YAMLConfig> YAMLConfigs;

    public static void main(String[] args) {
        System.out.println("YAJSI (Yet another Java settings implementation) is a library and can't be used as a standalone!");
    }

    public YAJSISettingsManager(Collection<YAMLConfig> YAMLConfigs) {
        if (YAMLConfigs == null) this.YAMLConfigs = new ArrayList<>();
        else this.YAMLConfigs = YAMLConfigs.stream().toList();
        load();
    }

    public static YAJSISettingsManager withConfigFile(ConfigFile configFile, Class<? extends SettingsBundle> settingsBundle) {
        return new YAJSISettingsManager(Collections.singleton(new YAMLConfig(configFile, settingsBundle)));
    }

    public static YAJSISettingsManager withConfigFile(YAMLConfig YAMLConfig) {
        return new YAJSISettingsManager((Collections.singleton(YAMLConfig)));
    }

    @SafeVarargs
    public static YAJSISettingsManager withConfigFile(ConfigFile configFile, Class<? extends SettingsBundle>... settingsBundles) {
        if (configFile != null && settingsBundles != null) {
            return withConfigFile(configFile, Arrays.stream(settingsBundles).toList());
        }
        return new YAJSISettingsManager(null);
    }

    public static YAJSISettingsManager withConfigFile(ConfigFile configFile, Collection<Class<? extends SettingsBundle>> configFiles) {
        List<YAMLConfig> YAMLConfigList = new ArrayList<>();
        for (Class<? extends SettingsBundle> settingsBundle : configFiles) {
            YAMLConfigList.add(new YAMLConfig(configFile, settingsBundle));
        }
        return new YAJSISettingsManager(YAMLConfigList);
    }

    public static YAJSISettingsManager withConfigFiles(Map<ConfigFile, Class<? extends SettingsBundle>> configFiles) {
        List<YAMLConfig> YAMLConfigList = new ArrayList<>();
        for (Map.Entry<ConfigFile, Class<? extends SettingsBundle>> configFile : configFiles.entrySet()) {
            YAMLConfigList.add(new YAMLConfig(configFile.getKey(), configFile.getValue()));
        }
        return new YAJSISettingsManager(YAMLConfigList);
    }

    public static YAJSISettingsManager withConfigFiles(Collection<YAMLConfig> YAMLConfigs) {
        return new YAJSISettingsManager(YAMLConfigs);
    }

    public void load() {
        for (YAMLConfig yamlConfig : YAMLConfigs) {
            try {
                YamlConfiguration yaml = getYamlConfiguration(yamlConfig);

                for (String key : yaml.getKeys(true)) {
                    settings.put(key, new YAJSISetting<>(yaml.get(key)));
                }

                new YAJSISettingsHandler<>().loadSettings(yamlConfig.settingsBundle, settings::get);
            } catch (IOException | NoSuchMethodException | InvocationTargetException |
                     InstantiationException | IllegalAccessException e) {
                System.out.println("Couldn't load settings from file " + yamlConfig.configFile.path + "! Error: " + e.getMessage());
            }
        }
    }

    public void save() {
        for (YAMLConfig yamlConfig : YAMLConfigs) {
            try {
                YamlConfiguration yaml = getYamlConfiguration(yamlConfig);
                // Save settings via SettingsManager
                boolean shouldSave = new YAJSISettingsHandler<>().saveSettings(yamlConfig.settingsBundle, yaml);

                if (shouldSave) {
                    yaml.save(yamlConfig.configFile.path);
                }

            } catch (IOException e) {
                System.out.println("Couldn't save settings to file " + yamlConfig.configFile.path + "! Error: " + e.getMessage());
            }

        }
    }

    private static @NotNull YamlConfiguration getYamlConfiguration(YAMLConfig yamlConfig) throws IOException {
        File configFile = new File(yamlConfig.configFile.path);
        YamlConfiguration yaml = new YamlConfiguration();
        if (!configFile.exists()) {
            if (!configFile.createNewFile()) throw new NullPointerException("Failed to create config file!");
            // Get the internal resource folder and default config values
            URL url = yamlConfig.configFile.defaultFile;

            // If the path is null or not found, an exception is thrown
            if (url == null) {
                throw new NullPointerException();
            }

            // Try to open a new input stream to read the default values
            try (InputStream inputStream = url.openStream()) {

                // Try to open a new output stream to save the values to the new config configFile
                try (OutputStream outputStream = new FileOutputStream(configFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    // Write the read bytes using the stored length in bytesRead
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            } catch (NullPointerException e) {
                System.out.println("Couldn't load configFile " + configFile.getPath() + ". No resource was found!");
            }
        }
        try {
            yaml.load(configFile);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        return yaml;
    }

    public <T> T getSetting(Class<? extends YAJSISetting<T>> settingClass) {
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
}
