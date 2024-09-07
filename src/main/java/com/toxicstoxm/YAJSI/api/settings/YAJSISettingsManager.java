package com.toxicstoxm.YAJSI.api.settings;


import com.toxicstoxm.YAJSI.api.logging.Logger;
import com.toxicstoxm.YAJSI.api.yaml.InvalidConfigurationException;
import com.toxicstoxm.YAJSI.api.file.YamlConfiguration;
import lombok.Getter;
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
    @Getter
    private boolean autoupdate = true;
    @Getter
    private boolean error = false;
    @Getter
    private final List<String> messages = new ArrayList<>();
    @Getter
    private Logger logger = messages::add;
    private List<YAMLConfig> yamlConfigs = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("YAJSI (Yet another Java settings implementation) is a library and can't be used as a standalone!");
    }

    private YAJSISettingsManager() {
        error = true;
    }
    
    public YAJSISettingsManager(Collection<YAMLConfig> yamlConfigs) {
        init(yamlConfigs);
    }

    private void init(Collection<YAMLConfig> yamlConfigs) {
        this.yamlConfigs = new ArrayList<>();
        if (yamlConfigs != null) this.yamlConfigs.addAll(yamlConfigs);
        if (!load()) error = true;
    }

    // static init methods
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

    // factory methods

    public static YAJSISettingsManager builder() {
        return new YAJSISettingsManager();
    }

    public YAJSISettingsManager build(Collection<YAMLConfig> yamlConfigs) {
        if (yamlConfigs != null) {
            error = false;
            init(yamlConfigs);
        }
        return this;
    }
    
    public YAJSISettingsManager buildWithConfigFile(ConfigFile configFile, Class<? extends SettingsBundle> settingsBundle) {
        return build(Collections.singleton(new YAMLConfig(configFile, settingsBundle)));
    }

    public YAJSISettingsManager buildWithConfigFile(YAMLConfig YAMLConfig) {
        return build((Collections.singleton(YAMLConfig)));
    }
    
    @SafeVarargs
    public final YAJSISettingsManager buildWithConfigFile(ConfigFile configFile, Class<? extends SettingsBundle>... settingsBundles) {
        if (configFile != null && settingsBundles != null) {
            return buildWithConfigFile(configFile, Arrays.stream(settingsBundles).toList());
        }
        return build(null);
    }

    public YAJSISettingsManager buildWithConfigFile(ConfigFile configFile, Collection<Class<? extends SettingsBundle>> configFiles) {
        List<YAMLConfig> YAMLConfigList = new ArrayList<>();
        for (Class<? extends SettingsBundle> settingsBundle : configFiles) {
            YAMLConfigList.add(new YAMLConfig(configFile, settingsBundle));
        }
        return build(YAMLConfigList);
    }

    public YAJSISettingsManager buildWithConfigFiles(Map<ConfigFile, Class<? extends SettingsBundle>> configFiles) {
        List<YAMLConfig> YAMLConfigList = new ArrayList<>();
        for (Map.Entry<ConfigFile, Class<? extends SettingsBundle>> configFile : configFiles.entrySet()) {
            YAMLConfigList.add(new YAMLConfig(configFile.getKey(), configFile.getValue()));
        }
        return build(YAMLConfigList);
    }

    public YAJSISettingsManager buildWithConfigFiles(Collection<YAMLConfig> YAMLConfigs) {
        return build(YAMLConfigs);
    }
    
    public YAJSISettingsManager setAutoUpdate(boolean autoUpdate) {
        this.autoupdate = autoUpdate;
        return this;
    }

    public YAJSISettingsManager setLoggingImplementation(Logger loggingImplementation) {
        this.logger = loggingImplementation;
        return this;
    }

    public YAJSISettingsManager addConfigFile(YAMLConfig yamlConfig) {
        if (yamlConfigs != null) {
            this.yamlConfigs.add(yamlConfig);
            LoadResult result = loadOnly(yamlConfig);
            if (result.successful) this.settings.putAll(result.tempSettings);
        }
        return this;
    }
    
    // loading all settings from all bundles

    public boolean load() {
        HashMap<String, Setting<Object>> tempSettingsAll = new HashMap<>();
        boolean successful = true;
        for (YAMLConfig yamlConfig : yamlConfigs) {
            LoadResult result = loadOnly(yamlConfig);
            successful = result.successful;
            tempSettingsAll.putAll(result.tempSettings);
        }
        if (successful) settings.putAll(tempSettingsAll);
        // returns success state to allow for error handling elsewhere
        return successful;
    }

    public record LoadResult(HashMap<String, Setting<Object>> tempSettings, boolean successful) {}

    public LoadResult loadOnly(YAMLConfig yamlConfig) {
        HashMap<String, Setting<Object>> tempSettingsAll = new HashMap<>();
        boolean successful = true;
        String prefix = "[" + Arrays.stream(yamlConfig.settingsBundle.getName().split("\\.")).toList().getLast() + "]: ";
        try {
            YamlConfiguration yaml = getYamlConfiguration(yamlConfig);
            HashMap<String, Setting<Object>> tempSettings = new HashMap<>();

            for (String key : yaml.getKeys(true)) {
                tempSettings.put(key, new YAJSISetting<>(yaml.get(key)));
            }

            // loads values from config and checks if an error occurs
            if (new YAJSISettingsHandler<>(logger).loadSettings(yamlConfig.settingsBundle, tempSettings::get)) {
                logger.log(prefix + "Something went wrong while loading config values! Possible cause: outdated config file");

                // tries to automatically update the config file if enabled
                if (autoupdate) {
                    boolean error = false;
                    logger.log(prefix + "Trying to auto update outdated config!");
                    // backs up loaded settings
                    YamlConfiguration backup = yaml;
                    File file = new File(yamlConfig.configFile.path);
                    // tries to delete the old config file and loads the new defaults
                    // saves the backed-up settings to the new config file
                    // loads the new config file again
                    if (file.delete()) {
                        yaml = getYamlConfiguration(yamlConfig);
                        new YAJSISettingsHandler<>(logger).saveSettings(yamlConfig.settingsBundle, yaml, true);
                        yaml.save(yamlConfig.configFile.path);
                        tempSettings.clear();
                        for (String key : yaml.getKeys(true)) {
                            tempSettings.put(key, new YAJSISetting<>(yaml.get(key)));
                        }
                        if (new YAJSISettingsHandler<>(logger).loadSettings(yamlConfig.settingsBundle, tempSettings::get)) {
                            error = true;
                        }
                    } else error = true;
                    // if an error occurred in the previous step, the backup is restored and the user is informed
                    if (error) {
                        logger.log(prefix + "Something went wrong while trying to auto update outdated config!");
                        logger.log(prefix + "Restoring outdated config file...");
                        backup.save(yamlConfig.configFile.path);
                        successful = false;
                    } else tempSettingsAll.putAll(tempSettings);
                } else logger.log(prefix + "Please update the config file manually! because auto updates are disabled!");
            } else tempSettingsAll.putAll(tempSettings);
        } catch (IOException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            logger.log(prefix + "Couldn't load settings from file " + yamlConfig.configFile.path + "! Error: " + e.getMessage());
        }
        return new LoadResult(tempSettingsAll, successful);
    }

    // saving all settings from all bundles
    
    public void save() {
        for (YAMLConfig yamlConfig : yamlConfigs) {
            String prefix = "[" + Arrays.stream(yamlConfig.settingsBundle.getName().split("\\.")).toList().getLast() + "]: ";
            try {
                YamlConfiguration yaml = getYamlConfiguration(yamlConfig);

                // Save settings via SettingsManager and check if the file should be saved
                boolean shouldSave = new YAJSISettingsHandler<>(logger).saveSettings(yamlConfig.settingsBundle, yaml);

                // only save if required
                if (shouldSave) {
                    yaml.save(yamlConfig.configFile.path);
                }

            } catch (IOException e) {
                logger.log(prefix + "Couldn't save settings to file " + yamlConfig.configFile.path + "! Error: " + e.getMessage());
            }

        }
    }

    private @NotNull YamlConfiguration getYamlConfiguration(YAMLConfig yamlConfig) throws IOException {
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
                logger.log("Couldn't load configFile " + configFile.getPath() + ". No resource was found!");
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
        if (error) throw new RuntimeException(Arrays.stream(this.getClass().getName().split("\\.")).toList().getLast() + " was not initialized properly or failed to load!");
        return settings.get(s);
    }
}
