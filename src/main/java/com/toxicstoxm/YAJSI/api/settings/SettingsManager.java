package com.toxicstoxm.YAJSI.api.settings;

import com.toxicstoxm.YAJSI.api.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.api.logging.Logger;
import com.toxicstoxm.YAJSI.api.yaml.ConfigurationSection;
import com.toxicstoxm.YAJSI.api.yaml.InvalidConfigurationException;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

public class SettingsManager {

    private final HashMap<Object, YamlConfiguration> registeredSettings = new HashMap<>();

    private static SettingsManager instance;

    private static final Queue<String> logMessageQueue = new ArrayDeque<>();

    private static String defaultAppName = "YAJSI";

    private static final Logger defaultLogger = message -> {
        if (logMessageQueue.size() >= 100) {
            logMessageQueue.poll();
        }
        logMessageQueue.add(message);
    };


    private SettingsManagerConfig config = SettingsManagerConfig.builder().build();

    public ManualAdjustmentHelper makeManualAdjustmentsTo(Object yamlConfig) {
        if (!registeredSettings.containsKey(yamlConfig)) {
            registerYAMLConfiguration(yamlConfig);
        }
        YamlConfiguration yaml = registeredSettings.get(yamlConfig);
        File configFile = getConfigFile(yamlConfig);

        return new ManualAdjustmentHelper() {
            @Override
            public YamlConfiguration getYAML() {
                return yaml;
            }

            @Override
            public void returnResponsibility() {
                try {
                    yaml.save(configFile);
                    registerYAMLConfiguration(yamlConfig);
                } catch (IOException e) {
                    throw new RuntimeException("Manual adjustment failed! Error message: " + e.getMessage(), e);
                }
            }
        };
    }

    public void configure(@NotNull SettingsManagerConfig customConfig) {
        this.config = customConfig;
        if (customConfig.logger != defaultLogger) {
            while (!logMessageQueue.isEmpty()) {
                customConfig.logger.log(logMessageQueue.poll());
            }
        }
    }

    @Builder
    public static class SettingsManagerConfig {

        @Builder.Default
        public String configDirectory = getAppDir();

        @Builder.Default
        public String appName = defaultAppName;

        @Builder.Default
        public YAMLUpdatingBehaviour updatingBehaviour = YAMLUpdatingBehaviour.MARK_UNUSED;

        @Builder.Default
        public Logger logger = defaultLogger;

        private static @NotNull String getAppDir() {
            String confHome = java.lang.System.getenv("XDG_CONFIG_HOME");
            return confHome == null
                    ? java.lang.System.getProperty("user.home") + "/.config/" + (instance != null ? getInstance().config.appName : defaultAppName) + "/"
                    : confHome + "/";
        }
    }

    private SettingsManager() {

    }

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }

        return instance;
    }

    public void registerYAMLConfiguration(@NotNull Object yamlConfig) {
        registerYAMLConfiguration(yamlConfig, false);
    }

    public YamlConfiguration unregisterYAMLConfiguration(@NotNull Object yamlConfig) {
        return registeredSettings.remove(yamlConfig);
    }

    public void restoreDefaultsFor(@NotNull Object yamlConfig) {
        registerYAMLConfiguration(yamlConfig, true);
    }

    public void registerYAMLConfiguration(@NotNull Object yamlConfig, boolean overwrite) {

        File configFile = getConfigFile(yamlConfig);
        String configPath = configFile.getAbsolutePath();

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file '" + configPath + "'!", e);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException("Invalid YAML config file '" + configPath + "'!", e);
        }

        // Create a Set<Object> to track processed objects and avoid infinite recursion
        Set<Object> processedObjects = new HashSet<>();

        // Recursively process the configuration object, passing the processedObjects set
        processYamlFields(yamlConfig, yaml, "", processedObjects, overwrite);

        // Save updated configuration back to the file
        try {
            yaml.save(configFile);
            registeredSettings.put(yamlConfig, yaml);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save updated config file '" + configPath + "'", e);
        }
    }

    public File getConfigLocation(Object yamlConfig) {
        if (registeredSettings.containsKey(yamlConfig)) {
            registerYAMLConfiguration(yamlConfig);
        }
        return getConfigFile(yamlConfig);
    }

    private @NotNull File getConfigFile(Object yamlConfig) {

        Class<?> clazz = yamlConfig.getClass();
        if (!clazz.isAnnotationPresent(YAMLConfiguration.class)) {
            throw new IllegalArgumentException("Class must be annotated with @YAMLConfiguration");
        }

        String configPath = !clazz.getAnnotation(YAMLConfiguration.class).filePath().isBlank()
                ? clazz.getAnnotation(YAMLConfiguration.class).filePath()
                : config.configDirectory + clazz.getSimpleName();

        File config = new File(configPath);

        if (!config.exists()) {
            try {
                if (!config.getParentFile().exists()) {
                    if (!config.getParentFile().mkdirs()) {
                        throw new IOException("Failed to create parent directory '" + config.getParentFile().getAbsolutePath() + "' for config file '" + configPath + "'!");
                    }
                }
                if (!config.createNewFile()) {
                    throw new IOException("File '" + configPath + "' already exists!");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create config file '" + configPath + "'!", e);
            }
        } else if (!config.isFile()) {
            throw new RuntimeException("File '" + configPath + "' is a directory!");
        }
        return config;
    }

    private void processYamlFields(@NotNull Object yamlConfig, @NotNull ConfigurationSection yaml, @NotNull String parentPath, @NotNull Set<Object> processedObjects, boolean overwrite) {
        if (processedObjects.contains(yamlConfig)) {
            return; // Prevent infinite recursion
        }
        processedObjects.add(yamlConfig);

        Class<?> clazz = yamlConfig.getClass();
        String sectionPath = parentPath.isEmpty() ? "" : parentPath + ".";
        Set<String> yamlKeys = yaml.getKeys(false);

        for (Field field : clazz.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (field.isAnnotationPresent(YAMLSetting.Ignore.class)
                    || Modifier.isFinal(modifiers)
                    || Modifier.isStatic(modifiers)
            ) continue;
            field.setAccessible(true);

            try {
                String yamlKey;
                String fullKey;

                Object fieldValue = field.get(yamlConfig);

                if (field.isAnnotationPresent(YAMLSetting.class)) {

                    String tmp = field.getAnnotation(YAMLSetting.class).name();
                    yamlKey = tmp.isBlank()
                            ? field.getName()
                            : tmp;

                    fullKey =  sectionPath + yamlKey;

                    String[] comments = field.getAnnotation(YAMLSetting.class).comments();
                    if (comments.length > 0) {
                        yaml.setComments(fullKey, List.of(comments));  // Set comments for the key
                    }
                } else {
                    yamlKey = field.getName();
                }

                fullKey =  sectionPath + yamlKey;

                if (isCustomObject(fieldValue)) {
                    // Handle nested objects
                    Object nestedObject = fieldValue != null
                            ? fieldValue
                            : field.getType().getConstructor().newInstance();
                    field.set(yamlConfig, nestedObject);

                    // Only recurse if the section is not already created
                    processYamlFields(nestedObject, yaml, fullKey, processedObjects, overwrite);
                } else if (isListOfPrimitives(fieldValue)) {
                    // Handle lists of primitives
                    if (yaml.contains(fullKey) && !overwrite) {
                        List<?> yamlList = yaml.getList(fullKey);
                        field.set(yamlConfig, yamlList);
                    } else {
                        yaml.set(fullKey, fieldValue);
                    }
                } else {
                    // Handle primitive or basic types
                    if (yaml.contains(fullKey) && !overwrite) {
                        field.set(yamlConfig, getValue(field.getType(), yaml.get(fullKey)));
                    } else {
                        yaml.set(fullKey, fieldValue);
                    }
                }

                yamlKeys.remove(yamlKey);
            } catch (Exception e) {
                throw new RuntimeException("Failed to process field '" + field.getName() + "'", e);
            }
        }

        // Handle unused keys and add comments to unused keys
        for (String unusedKey : yamlKeys) {
            String unusedPath = sectionPath + unusedKey;
            switch (config.updatingBehaviour) {
                case MARK_UNUSED -> yaml.setComments(unusedPath, List.of("This setting is currently unused"));
                case REMOVE -> yaml.set(unusedPath, null);
                case IGNORE -> {/* TODO log */}
            }
        }
    }


    private boolean isCustomObject(Object object) {
        if (object == null) {
            return true;
        }
        Class<?> clazz = object.getClass();
        return !(clazz.isPrimitive() || clazz.equals(String.class) || clazz.equals(Boolean.class) || Number.class.isAssignableFrom(clazz) || clazz.isArray() || isListOfPrimitives(object));
    }

    private boolean isListOfPrimitives(Object object) {
        if (object instanceof List<?> list) {
            if (list.isEmpty()) {
                return true; // Consider empty lists as lists of primitives
            }
            // Check if all elements are primitives or simple types
            return list.stream().allMatch(item ->
                    item instanceof String || item instanceof Number || item instanceof Boolean
            );
        }
        return false;
    }

    private Object getValue(@NotNull Type desired, Object current) {
        if (desired.equals(float.class)) {
            return ((Double) current).floatValue();
        }
        return current;
    }

    public void save() {
        registeredSettings.keySet().forEach(this::save);
    }

    public void save(Object yamlConfig) {
        if (!registeredSettings.containsKey(yamlConfig)) {
            registerYAMLConfiguration(yamlConfig);
            return;
        }

        File configFile = getConfigFile(yamlConfig);

        YamlConfiguration yaml = registeredSettings.get(yamlConfig);

        HashSet<Object> processedObjects = new HashSet<>();

        processYamlFields(yamlConfig, yaml, "", processedObjects, true);

        try {
            yaml.save(configFile);
        } catch (IOException e) {
            // TODO log
        }
    }
}
