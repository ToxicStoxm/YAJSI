package com.toxicstoxm.YAJSI.api.settings;

import com.toxicstoxm.YAJSI.api.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.api.logging.Logger;
import com.toxicstoxm.YAJSI.api.yaml.ConfigurationSection;
import com.toxicstoxm.YAJSI.api.yaml.InvalidConfigurationException;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Singleton class for managing your configurations using the provided functions.
 * @see #registerYAMLConfiguration(Object)
 * @see #unregisterYAMLConfiguration(Object)
 * @see #configure(SettingsManagerConfig)
 * @see #save()
 * @author ToxicStoxm
 */
public class SettingsManager {

    /**
     * Singleton instance of this class.
     */
    private static SettingsManager instance;

    /**
     * This log message queue used as a sort of buffer to eliminate the need for providing a logger on initialization.
     * If the logger implementation is unset, messages will be added to this queue by default.
     * Once the user sets a logger implementation, all stored messages will be logged at once.
     * Going forward, the new logger implementation is used directly without the need for this intermediate buffer.
     * Should the log implementation ever return to the default one, this buffer will be reactivated.
     */
    private static final Queue<String> logMessageQueue = new ArrayDeque<>();

    /**
     * This name will be used by default, if left unset.
     */
    private static final String defaultAppName = "YAJSI";

    /**
     * The default logger implementation that will be used if logger is unset.
     * If enabled, this implementation enqueues all messages into {@link #logMessageQueue} for later usage.
     */
    private static final Logger defaultLogger = message -> {
        if (instance != null && getInstance().config.enableLogBuffer) {
            // Ensure buffer size stays below the specified size.
            if (logMessageQueue.size() >= getInstance().config.logMessageBufferSize) {
                logMessageQueue.poll();
            }
            logMessageQueue.add(message);
        }
    };

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }

        return instance;
    }

    /**
     * Private constructor, because SettingsManager uses a singleton pattern.
     * @see #getInstance()
     */
    private SettingsManager() {}

    /**
     * Storage for registered config class instances and their corresponding YAML Configurations.
     */
    private final HashMap<Object, YamlConfiguration> registeredSettings = new HashMap<>();

    /**
     * Holds configuration values for this SettingsManger instance.
     * These settings are used to fine-tune the SettingsMangers behavior.
     * @see SettingsManagerConfig
     */
    private SettingsManagerConfig config = SettingsManagerConfig.builder().build();

    /**
     * Configuration class for {@link SettingsManager}.
     */
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

        @Builder.Default
        public boolean enableLogBuffer = true;

        @Builder.Default
        public int logMessageBufferSize = 100;

        @Builder.Default
        public boolean configClassesHaveNoArgsConstructor = true;

        /**
         * Tries to find either a designated system config folder, or if that fails, use the users config folder.
         * @return a config folder, where all configs will be stored by default
         */
        private static @NotNull String getAppDir() {
            String confHome = java.lang.System.getenv("XDG_CONFIG_HOME");
            return confHome == null
                    ? java.lang.System.getProperty("user.home") + "/.config/" + (instance != null ? getInstance().config.appName : defaultAppName) + "/"
                    : confHome + "/";
        }
    }

    /**
     * Fine tune the SettingsManagers behavior. <br>
     * If a custom logger implementation was provided, this also tries to log all queued log messages using it,
     * if the log message queue is enabled.
     * @param customConfig custom config, which will be used going forward.
     */
    public void configure(@NotNull SettingsManagerConfig customConfig) {
        this.config = customConfig;

        // If a custom logger implementation was provided, try to log all queued log messages using it.
        if (customConfig.logger != defaultLogger && config.enableLogBuffer) {
            while (!logMessageQueue.isEmpty()) {
                customConfig.logger.log(logMessageQueue.poll());
            }
        }
    }

    /**
     * Voids the current config. <br>
     * Restores the defaults by creating a new instance of {@link SettingsManagerConfig}.
     */
    public void voidConfig() {
        this.config = SettingsManagerConfig.builder().build();
    }

    /**
     * Clears the log buffer {@link #logMessageQueue}.
     * @return number of removed elements from the buffer.
     */
    public int clearLogBuffer() {
        int size = logMessageQueue.size();
        logMessageQueue.clear();
        return size;
    }

    /**
     * Registers a YAML config class in the SettingsManager's system.
     * <h4>Cases:</h4>
     * <ul>
     *     <li><b>Config file exists</b>: If an existing config file for this class is found, values will be loaded from it.
     *         Missing values will be added to the config file by getting the defaults from the config class. </li>
     *     <li><b>No config file exists</b>: If no existing config file for this class is found, a new one will be created
     *         in the specified config path (default path: {@linkplain SettingsManagerConfig#configDirectory})
     *         and populated with the default values from yamlConfig.</b>
     *     </li>
     * </ul>
     * From this point onward by calling {@link #save(Object)} with this YAML config class,
     * will save its values to the created / loaded config.
     * @param yamlConfig the YAML config class to register
     * @implNote Uses {@link #registerYAMLConfiguration(Object, boolean)}, overwrite = {@code false}
     * @see #unregisterYAMLConfiguration(Object)
     * @see #registerYAMLConfiguration(Object, boolean)
     * @see #restoreDefaultsFor(Object)
     * @see #reloadFromFile(Object)
     */
    public void registerYAMLConfiguration(@NotNull Object yamlConfig) {
        registerYAMLConfiguration(yamlConfig, false);
    }

    /**
     * Unregisters all yaml config classes.
     * @implNote This does not delete the actual YAML config files on disk.
     *           If you want to do that use {@link #deleteYAMLConfigurationFileFor(Object)}.
     */
    public void unregisterALLConfigurations() {
        registeredSettings.clear();
    }

    /**
     * Unregisters the specified YAML config class from the SettingsManager's system.
     * @param yamlConfig the yamlConfig class to unregister
     * @implNote This does not delete the actual YAML config files on disk.
     *           If you want to do that use {@link #deleteYAMLConfigurationFileFor(Object)}.
     */
    public void unregisterYAMLConfiguration(@NotNull Object yamlConfig) {
        registeredSettings.remove(yamlConfig);
    }

    /**
     * Writes the default values from the specified YAML config class to its YAML config file on disk.
     * This will automatically register the specified YAML config class for future usages.
     * <h1>IMPORTANT: </h1>
     * <b>If 'configClassesHaveNoArgsConstructor' is set to {@code false} in the {@link SettingsManagerConfig},
     * this will fail silently if no matching constructor was found!</b>
     * @param yamlConfig defaults will be restored for the YAML config file corresponding to this YAML config class.
     */
    public void restoreDefaultsFor(@NotNull Object yamlConfig) throws YAJSIException {
        if (!isRegistered(yamlConfig)) registerYAMLConfiguration(yamlConfig);

        Class<?> clazz = yamlConfig.getClass();
        try {
            Object defaultYamlConfig = clazz.getDeclaredConstructor().newInstance();
            save(defaultYamlConfig);
            reloadFromFile(yamlConfig);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            if (config.configClassesHaveNoArgsConstructor) {
                throw YAJSIException.builder()
                        .message("Restoring default values failed, because no matching constructor for class '"
                                + yamlConfig.getClass().getName() + "' was found! "
                                + "Make sure you either provide a no-args-constructor in your YAML config classes or"
                                + " set 'configClassesHaveNoArgsConstructor' to false in the SettingsManager config!")
                        .cause(e)
                        .build();
            }
        }

    }

    /**
     * Checks if the specified YAML config class is registered.
     * @param yamlConfig the YAML config class to check
     * @return {@code true}, if the specified YAML config class is registered, otherwise {@code false}
     */
    public boolean isRegistered(Object yamlConfig) {
        return registeredSettings.containsKey(yamlConfig);
    }

    /**
     * Registers the provided YAML config class in the SettingsManager's system.
     * @param yamlConfig the YAML config class to register
     * @param overwrite if any existing YAML config file should be overwritten with the current values of the specified YAML config class.
     */
    public void registerYAMLConfiguration(@NotNull Object yamlConfig, boolean overwrite) throws YAJSIException {

        File configFile = getConfigFile(yamlConfig);
        String configPath = configFile.getAbsolutePath();

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(configFile);
        } catch (IOException e) {
            throw YAJSIException.builder()
                    .message("Failed to load config file '" + configPath + "'!")
                    .cause(e)
                    .build();
        } catch (InvalidConfigurationException e) {
            throw YAJSIException.builder()
                    .message("Invalid YAML config file '" + configPath + "'!")
                    .cause(e)
                    .build();
        }

        // Create a Set<Object> to track processed objects and avoid infinite recursion
        Set<Object> processedObjects = new HashSet<>();

        // Recursively process the configuration object, passing the processedObjects set
        processYAMLFields(yamlConfig, yaml, "", processedObjects, overwrite);

        // Save updated configuration back to the file
        try {
            yaml.save(configFile);
            registeredSettings.put(yamlConfig, yaml);
        } catch (IOException e) {
            throw YAJSIException.builder()
                    .message("Failed to save updated config file '" + configPath + "'")
                    .cause(e)
                    .build();
        }
    }

    /**
     * Reloads all available values from the corresponding YAML config file.
     * @implNote if no config file exists, one will be created upon calling this function.
     * @param yamlConfig the YAML config class to reload the values for
     */
    public void reloadFromFile(Object yamlConfig) {
        registerYAMLConfiguration(yamlConfig, false);
    }

    /**
     * Returns a reference to the YAML config {@link File} corresponding to this YAML config class.
     * @param yamlConfig the YAML config class to get the corresponding config file for.
     * @return the {@link File} representing this YAML config classes config file
     * @implNote Uses {@link #getConfigFile(Object)}
     */
    public File getConfigLocation(Object yamlConfig) {
        if (!isRegistered(yamlConfig)) {
            registerYAMLConfiguration(yamlConfig);
        }
        return getConfigFile(yamlConfig);
    }

    /**
     * Returns the YAML config corresponding to this YAML config class in {@link String} form.
     * @param yamlConfig the YAML config class to get the YAML config for
     * @return the serialized YAML config for the specified YAML config class
     */
    public String getYAMLString(Object yamlConfig) {
        if (!isRegistered(yamlConfig)) {
            registerYAMLConfiguration(yamlConfig);
        }
        return registeredSettings.get(yamlConfig).saveToString();
    }

    /**
     * Deletes the YAML config file corresponding to this YAML config class.
     * @param yamlConfig the YAML config file to delete the YAML config file for.
     * @return {@code true} if the YAML config file was successfully deleted, {@code false} if no file was found, or the deletion failed
     */
    public boolean deleteYAMLConfigurationFileFor(Object yamlConfig) {
        File f = getConfigFile(yamlConfig);
        unregisterYAMLConfiguration(yamlConfig);
        return f.delete();
    }

    /**
     * This method is useful if you want to manually make changes to the specified YAML config class's underlying YAML config file.
     * <h3>Usage:</h3>
     * This method returns a {@link ManualAdjustmentHelper} which will simplify the process. Here's an example of how to use it:
     * <ol>
     *     <li>Get the YAML config using {@link ManualAdjustmentHelper#getYAML()}</li>
     *     <li>Make your adjustments to that {@link YamlConfiguration}</li>
     *     <li>Once your happy just call {@link ManualAdjustmentHelper#returnResponsibility()} to automatically re-register and reload the corresponding YAML config class</li>
     * </ol>
     * <h3>IMPORTANT:</h3>
     * <ul>
     *     <li>
     *         <b>After calling {@link ManualAdjustmentHelper#returnResponsibility()},
     *         further changing the {@link YamlConfiguration} obtained via {@link ManualAdjustmentHelper#getYAML()} can break the system!</b>
     *     </li>
     *     <li>
     *         <b>Only changes to values defined in the corresponding YAML config class will have any effect on that YAML config class.
     *         Other changes might be ignored or removed depending on {@link SettingsManagerConfig#updatingBehaviour}</b>
     *     </li>
     * </ul>
     * @param yamlConfig the YAML config class you want to manually modify the corresponding YAML config for
     * @return a {@link ManualAdjustmentHelper}, as described above
     */
    public ManualAdjustmentHelper makeManualAdjustmentsTo(Object yamlConfig) throws YAJSIException {
        if (!isRegistered(yamlConfig)) {
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
                    throw YAJSIException.builder()
                            .message("Manual adjustment failed! Error message: " + e.getMessage())
                            .cause(e)
                            .build();
                }
            }
        };
    }

    /**
     * Returns a reference to the YAML config {@link File} corresponding to this YAML config class.
     * @param yamlConfig the YAML config class to get the corresponding config file for.
     * @return the {@link File} representing this YAML config classes config file
     */
    private @NotNull File getConfigFile(@NotNull Object yamlConfig) throws YAJSIException {

        Class<?> clazz = yamlConfig.getClass();
        if (!clazz.isAnnotationPresent(YAMLConfiguration.class)) {
            throw YAJSIException.builder()
                    .message("Failed to get config file for '" + yamlConfig.getClass().getName() + "'!")
                    .cause(new IllegalArgumentException("Class must be annotated with @YAMLConfiguration"))
                    .build();
        }

        String configPath = !clazz.getAnnotation(YAMLConfiguration.class).filePath().isBlank()
                ? clazz.getAnnotation(YAMLConfiguration.class).filePath()
                : config.configDirectory + clazz.getSimpleName();

        File config = new File(configPath);

        try {
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
        } catch (RuntimeException e) {
            throw YAJSIException.builder()
                    .message("Failed to get config file for '" + yamlConfig.getClass().getName() + "'!")
                    .cause(e)
                    .build();
        }
        return config;
    }

    /**
     * Core function which recursively analyzes the provided YAML config class and creates a corresponding YAML config.
     * @param yamlConfig the YAML config class to analyze
     * @param yaml the current YAML config section to write values to
     * @param parentPath the current YAML config path prefix
     * @param processedObjects a {@link HashSet} of already processed objects, this prevents infinite recursion
     * @param overwrite if {@code true} any existing YAML config value will be overwritten by that inside the YAML config class.
     *                  if {@code false} existing YAML config values will be loaded to the YAML config class, and only missing ones are added from the class
     */
    private void processYAMLFields(@NotNull Object yamlConfig, @NotNull ConfigurationSection yaml, @NotNull String parentPath, @NotNull Set<Object> processedObjects, boolean overwrite) throws YAJSIException {
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
                    processYAMLFields(nestedObject, yaml, fullKey, processedObjects, overwrite);
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
                throw YAJSIException.builder()
                        .message("Failed to process field '" + field.getName() + "'")
                        .cause(e)
                        .build();
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


    /**
     * Checks if the specified object is not a primitive object.
     * @param object the object to check
     * @return {@code false} if the specified object is a primitive object, otherwise {@code true}
     */
    private boolean isCustomObject(Object object) {
        if (object == null) {
            return true;
        }
        Class<?> clazz = object.getClass();
        return !(clazz.isPrimitive() || clazz.equals(String.class) || clazz.equals(Boolean.class) || Number.class.isAssignableFrom(clazz) || clazz.isArray() || isListOfPrimitives(object));
    }

    /**
     * Checks if the specified object is a list of primitive objects.
     * @param object the object to check
     * @return {@code true} if the specified object is a list of primitive objects, otherwise {@code true}
     */
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

    /**
     * This method ensures double values from the YAML config file are converted to primitive floats if needed.
     * @param desired the desired primitive abject type, specified by the YAML config class
     * @param current the current object, that may be converted if necessary
     * @return the final object
     */
    private Object getValue(@NotNull Type desired, Object current) {
        if (desired.equals(float.class)) {
            return ((Double) current).floatValue();
        }
        return current;
    }

    /**
     * Saves all registered YAML config class values to their corresponding YAML config file.
     * @implNote Uses {@link #save(Object)}
     */
    public void save() {
        registeredSettings.keySet().forEach(this::save);
    }

    /**
     * Saves all values from the specified YAML config class to the classes corresponding YAML config file.
     * @param yamlConfig the YAML config class to save
     */
    public void save(Object yamlConfig) throws YAJSIException {
        if (!isRegistered(yamlConfig)) {
            registerYAMLConfiguration(yamlConfig);
            return;
        }

        File configFile = getConfigFile(yamlConfig);
        YamlConfiguration yaml = registeredSettings.get(yamlConfig);

        HashSet<Object> processedObjects = new HashSet<>();
        processYAMLFields(yamlConfig, yaml, "", processedObjects, true);

        try {
            yaml.save(configFile);
        } catch (IOException e) {
            throw YAJSIException.builder()
                    .message("Failed to save YAML config to file for '" + yamlConfig.getClass().getName() + "' config class!")
                    .cause(e)
                    .build();
        }
    }
}
