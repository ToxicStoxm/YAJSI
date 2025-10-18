package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import com.toxicstoxm.StormYAML.yaml.InvalidConfigurationException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Singleton class for managing your configurations using the provided functions.
 * @see #registerYAMLConfiguration(Object)
 * @see #unregisterYAMLConfiguration(Object)
 * @see #configure()
 * @see #save()
 * @author ToxicStoxm
 */
public class SettingsManager implements SettingsManagerSettings {

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
    private SettingsManager() {
        log("Initializing SettingsManager singleton instance.");
    }

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
     * Returns the config directory used by YAJSI.
     */
    public String getConfigDirectory() {
        return config.getConfigDirectory();
    }

    /**
     * Configuration class for {@link SettingsManager}.
     */
    @Builder
    @Setter(onParam_ = @NotNull)
    @Getter
    public static class SettingsManagerConfig {

        @Builder.Default
        public String configDirectory = getAppDir();

        @Builder.Default
        public String appName = defaultAppName;

        @Builder.Default
        public YAMLUpdatingBehaviour updatingBehaviour = YAMLUpdatingBehaviour.MARK_UNUSED;

        @Builder.Default
        @Setter(AccessLevel.NONE)
        public Logger logger = defaultLogger;

        public void setLogger(Logger logger) {
            this.logger = logger;
            getInstance().checkLogFlush();
        }

        @Builder.Default
        public boolean enableLogBuffer = true;

        @Builder.Default
        public int logMessageBufferSize = 100;

        @Builder.Default
        public boolean configClassesHaveNoArgsConstructor = true;

        @Builder.Default
        public String unusedSettingWarning = "This setting is currently unused";

        /**
         * Tries to find either a designated system config folder, or if that fails, use the users config folder.
         * @return a config folder, where all configs will be stored by default
         */
        private static @NotNull String getAppDir() {
            String confHome = System.getenv("XDG_CONFIG_HOME");
            return confHome == null
                    ? System.getProperty("user.home") + "/.config/" + (instance != null ? getInstance().config.appName : defaultAppName) + "/"
                    : confHome + "/";
        }
    }

    public static SettingsManagerSettings configure() {
        return getInstance();
    }

    /**
     * Fine tune the SettingsManagers behavior. <br>
     * If a custom logger implementation was provided, this also tries to log all queued log messages using it,
     * if the log message queue is enabled.
     * @param customConfig custom config, which will be used going forward.
     */
    public void setConfig(@NotNull SettingsManagerConfig customConfig) {
        log("Configuring SettingsManager with custom settings.");
        this.config = customConfig;

        checkLogFlush();
    }

    public void checkLogFlush() {
        // If a custom logger implementation was provided, try to log all queued log messages using it.
        if (config.logger != defaultLogger && config.enableLogBuffer) {
            log("Custom logger provided. Flushing log message queue.");
            while (!logMessageQueue.isEmpty()) {
                config.logger.log(logMessageQueue.poll());
            }
        }
    }

    @Override
    public SettingsManagerSettings setConfigDirectory(String configDirectory) {
        config.setConfigDirectory(configDirectory);
        return this;
    }

    @Override
    public SettingsManagerSettings setAppName(String appName) {
        config.setAppName(appName);
        return this;
    }

    @Override
    public SettingsManagerSettings setUpdatingBehaviour(YAMLUpdatingBehaviour updatingBehaviour) {
        config.setUpdatingBehaviour(updatingBehaviour);
        return this;
    }

    @Override
    public SettingsManagerSettings setLogger(Logger logger) {
        config.setLogger(logger);
        return this;
    }

    @Override
    public SettingsManagerSettings setEnableLogBuffer(boolean enableLogBuffer) {
        config.setEnableLogBuffer(enableLogBuffer);
        return this;
    }

    @Override
    public SettingsManagerSettings setLogMessageBufferSize(int logMessageBufferSize) {
        config.setLogMessageBufferSize(logMessageBufferSize);
        return this;
    }

    @Override
    public SettingsManagerSettings setConfigClassesHaveNoArgsConstructor(boolean configClassesHaveNoArgsConstructor) {
        config.setConfigClassesHaveNoArgsConstructor(configClassesHaveNoArgsConstructor);
        return this;
    }

    @Override
    public SettingsManagerSettings setUnusedSettingWarning(String unusedSettingWarning) {
        config.setUnusedSettingWarning(unusedSettingWarning);
        return this;
    }

    private void log(String message) {
        config.logger.log(message);
    }

    /**
     * Voids the current config. <br>
     * Restores the defaults by creating a new instance of {@link SettingsManagerConfig}.
     */
    public void voidConfig() {
        log("Voiding current configuration and restoring defaults.");
        this.config = SettingsManagerConfig.builder().build();
    }

    /**
     * Clears the log buffer {@link #logMessageQueue}.
     * @return number of removed elements from the buffer.
     */
    public int clearLogBuffer() {
        int size = logMessageQueue.size();
        log("Clearing log buffer with " + size + " messages.");
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
        log("Registering YAML configuration for class: " + yamlConfig.getClass().getName());
        registerYAMLConfiguration(yamlConfig, false);
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
     * @implNote Uses {@link #registerYAMLConfiguration(Object, boolean, YamlConfiguration)}, overwrite = {@code false}
     * @see #unregisterYAMLConfiguration(Object)
     * @see #registerYAMLConfiguration(Object, boolean)
     * @see #restoreDefaultsFor(Object)
     * @see #reloadFromFile(Object)
     */
    public void registerYAMLConfiguration(@NotNull Object yamlConfig, boolean overwrite) {
        log("Registering YAML configuration for class: " + yamlConfig.getClass().getName());
        registerYAMLConfiguration(yamlConfig, overwrite, null);
    }

    /**
     * Tries to automatically detect classes annotated with {@link YAMLConfiguration} and automatically registers them
     * using {@link #registerYAMLConfiguration(Object, boolean)}
     * <h1>IMPORTANT: </h1>
     * <b>If 'configClassesHaveNoArgsConstructor' is set to {@code false} in the {@link SettingsManagerConfig},
     * this will fail silently if no matching constructor was found!
     * @param packagePath the package to search
     */
    public void autoRegister(String packagePath) throws YAJSIException {
        log("Automatically registering YAML configurations in package: " + packagePath);
        autoRegister(packagePath, false, (Object) null);
    }

    /**
     * Tries to automatically detect classes annotated with {@link YAMLConfiguration} and automatically registers them
     * using {@link #registerYAMLConfiguration(Object, boolean)}
     * <h1>IMPORTANT: </h1>
     * <b>If 'configClassesHaveNoArgsConstructor' is set to {@code false} in the {@link SettingsManagerConfig},
     * this will fail silently if no matching constructor was found!
     * @param packagePath the package to search
     */
    public void autoRegister(String packagePath, boolean overwrite, @Nullable Object... constructorArgs) throws YAJSIException {
        log("Starting auto-registration for package: " + packagePath + ", overwrite: " + overwrite);
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(packagePath)
                .scan()) {


            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(YamlConfiguration.class.getName())) {
                Class<?> loadedClass = classInfo.loadClass();
                try {
                    if (constructorArgs != null) {
                        log("Initializing class with arguments: " + loadedClass.getName());
                        registerYAMLConfiguration(tryToInit(loadedClass, constructorArgs), overwrite);
                    } else {
                        log("Initializing class without arguments: " + loadedClass.getName());
                        registerYAMLConfiguration(tryToInit(loadedClass), overwrite);
                    }
                } catch (Exception e) {
                    log("Failed to auto-register class: " + loadedClass.getName() + " due to: " + e.getMessage());
                    throw YAJSIException.builder()
                            .message("Failed to auto-register class '" + loadedClass.getName() + "'!")
                            .cause(e)
                            .build();
                }
            }


        } catch (Exception e) {
            log("Auto-registering failed due to: " + e.getMessage());
            throw YAJSIException.builder()
                    .message("Auto-registering failed due to: " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }

    /**
     * Unregisters all yaml config classes.
     * @implNote This does not delete the actual YAML config files on disk.
     *           If you want to do that use {@link #deleteYAMLConfigurationFileFor(Object)}.
     */
    public void unregisterALLConfigurations() {
        log("Unregistering all YAML configurations.");
        registeredSettings.clear();
    }

    /**
     * Unregisters the specified YAML config class from the SettingsManager's system.
     * @param yamlConfig the yamlConfig class to unregister
     * @implNote This does not delete the actual YAML config files on disk.
     *           If you want to do that use {@link #deleteYAMLConfigurationFileFor(Object)}.
     */
    public void unregisterYAMLConfiguration(@NotNull Object yamlConfig) {
        log("Unregistering YAML configuration for class: " + yamlConfig.getClass().getName());
        registeredSettings.remove(yamlConfig);
    }

    /**
     * Writes the default values from the specified YAML config class to its YAML config file on disk.
     * This will automatically register the specified YAML config class for future usages.
     * <h1>IMPORTANT: </h1>
     * <b>If 'configClassesHaveNoArgsConstructor' is set to {@code false} in the {@link SettingsManagerConfig},
     * this will fail silently if no matching constructor was found!
     * If you want to call a constructor with arguments use {@link #restoreDefaultsFor(Object, Object...)}</b>
     * @param yamlConfig defaults will be restored for the YAML config file corresponding to this YAML config class.
     */
    public void restoreDefaultsFor(@NotNull Object yamlConfig) {
        log("Restoring defaults for YAML configuration class: " + yamlConfig.getClass().getName());
        restoreDefaultsFor(yamlConfig, (Object) null);
    }

    /**
     * Writes the default values from the specified YAML config class to its YAML config file on disk.
     * This will automatically register the specified YAML config class for future usages.
     * <h1>IMPORTANT: </h1>
     * <b>If 'configClassesHaveNoArgsConstructor' is set to {@code false} in the {@link SettingsManagerConfig},
     * this will fail silently if no matching constructor was found!</b>
     * @param yamlConfig defaults will be restored for the YAML config file corresponding to this YAML config class.
     * @param constructorArgs {@code null} or, if 'configClassesHaveNoArgsConstructor' is set to {@code false} in the {@link SettingsManagerConfig}, this can be used call a constructor with arguments.
     */
    public void restoreDefaultsFor(@NotNull Object yamlConfig, @Nullable Object... constructorArgs) throws YAJSIException {
        log("Restoring defaults for YAML configuration class: " + yamlConfig.getClass().getName() + " with constructor arguments.");
        if (!isRegistered(yamlConfig)) {
            log("Class is not registered. Registering before restoring defaults.");
            registerYAMLConfiguration(yamlConfig, false);
        }

        Class<?> clazz = yamlConfig.getClass();

        log("Saving default values for class: " + clazz.getName());
        save(tryToInit(clazz, constructorArgs));
        log("Reloading configuration from file for class: " + clazz.getName());
        reloadFromFile(yamlConfig);

    }

    private Object tryToInit(@NotNull Class<?> clazz) throws YAJSIException {
        log("Initializing class: " + clazz.getName() + " without arguments.");
        return tryToInit(clazz, (Object) null);
    }

    private boolean ensureVarNotNull(@Nullable Object... objects) {
        if (objects == null) return false;
        for (Object o : objects) {
            if (o == null) return false;
        }
        return true;

    }

    private Object tryToInit(@NotNull Class<?> clazz, @Nullable Object... constructorArgs) throws YAJSIException {
        log("Initializing class: " + clazz.getName());
        Object defaultYamlConfig = null;
        try {
            if (ensureVarNotNull(constructorArgs)) {
                log("Constructor arguments are not null; attempting to find matching constructor");
                Class<?>[] constructorArgsC = new Class<?>[constructorArgs.length];
                for (int i = 0; i < constructorArgs.length; i++) {
                    Object o = constructorArgs[i];
                    if (o == null) throw new NullPointerException("Arg object can't be null!");
                    constructorArgsC[i] = o.getClass();
                }
                Constructor<?> constructor = clazz.getDeclaredConstructor(constructorArgsC);
                constructor.setAccessible(true);
                defaultYamlConfig = constructor.newInstance(constructorArgs);
                log("Successfully instantiated class with arguments");
            } else {
                log("Constructor arguments are null; using no-args constructor");
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                defaultYamlConfig = constructor.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            log("Error during instantiation: " + e.getMessage());
            if (config.configClassesHaveNoArgsConstructor) {
                throw YAJSIException.builder()
                        .message("Restoring default values failed, because no matching constructor for class '"
                                + clazz.getName() + "' was found! "
                                + "Make sure you either provide a no-args-constructor in your YAML config classes or"
                                + " set 'configClassesHaveNoArgsConstructor' to false in the SettingsManager config!")
                        .cause(e)
                        .build();
            }
        }
        return defaultYamlConfig;
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
     * Loads configuration values from a YAML file input stream and applies them to the provided YAML config object.
     * <p>
     * This method reads YAML data from an {@link InputStream}, parses it, and maps the configuration values
     * onto the specified {@code yamlConfig} object.
     * </p>
     *
     * @param yamlConfig The object into which the YAML values will be loaded.
     * @param fileInputStream The input stream containing YAML configuration data.
     * @throws IOException If an error occurs while reading from the input stream.
     * @throws InvalidConfigurationException If the YAML content is malformed or cannot be parsed.
     */
    public void loadFromFileStream(@NotNull Object yamlConfig, InputStream fileInputStream)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();

        try (Reader reader = new InputStreamReader(fileInputStream)) {
            yamlConfiguration.load(reader);
        }

        loadFromYAMLConfiguration(yamlConfig, yamlConfiguration);
    }

    /**
     * Loads configuration values from a YAML string and applies them to the provided YAML config object.
     *
     * @param yamlConfig The object into which the YAML values will be loaded.
     * @param yamlString The YAML string containing configuration data.
     * @throws InvalidConfigurationException If the provided YAML string is malformed or cannot be parsed.
     */
    public void loadFromYAMLString(@NotNull Object yamlConfig, String yamlString) throws InvalidConfigurationException {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.loadFromString(yamlString);
        loadFromYAMLConfiguration(yamlConfig, yamlConfiguration);
    }

    /**
     * Loads configuration values from a YAML file and applies them to the provided YAML config object.
     *
     * @param yamlConfig The object into which the YAML values will be loaded.
     * @param file The YAML file containing configuration data.
     */
    public void loadFromYAMLConfigFile(@NotNull Object yamlConfig, File file) {
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        loadFromYAMLConfiguration(yamlConfig, yamlConfiguration);
    }

    /**
     * Registers and loads a YAML configuration object into the settings system.
     *
     * @param yamlConfig The object into which the YAML values will be loaded.
     * @param config The parsed YAML configuration object containing the settings to be applied.
     */
    public void loadFromYAMLConfiguration(@NotNull Object yamlConfig, YamlConfiguration config) {
        registerYAMLConfiguration(yamlConfig, false, config);
    }

    /**
     * Registers the provided YAML config class in the SettingsManager's system.
     * @param yamlConfig the YAML config class to register
     * @param overwrite if any existing YAML config file should be overwritten with the current values of the specified YAML config class.
     */
    public void registerYAMLConfiguration(@NotNull Object yamlConfig, boolean overwrite, YamlConfiguration config) throws YAJSIException {
        YamlConfiguration yaml;
        File configFile = null;
        String configPath = "";

        if (config == null) {
            log("Registering YAML configuration for: " + yamlConfig.getClass().getName() + ", overwrite: " + overwrite);
            configFile = getConfigFile(yamlConfig);
            configPath = configFile.getAbsolutePath();
            yaml = new YamlConfiguration();

            try {
                yaml.load(configFile);
                log("Loaded YAML configuration from file: " + configPath);
            } catch (IOException e) {
                log("Failed to load YAML config file: " + configPath);
                throw YAJSIException.builder()
                        .message("Failed to load config file '" + configPath + "'!")
                        .cause(e)
                        .build();
            } catch (InvalidConfigurationException e) {
                log("Invalid YAML configuration: " + configPath);
                throw YAJSIException.builder()
                        .message("Invalid YAML config file '" + configPath + "'!")
                        .cause(e)
                        .build();
            }
        } else {
            yaml = config;
        }

        Set<Object> processedObjects = new HashSet<>();
        processYAMLFields(yamlConfig, yaml, "", processedObjects, overwrite);

        if (config == null) {
            try {
                yaml.save(configFile);
                log("Saved updated YAML configuration to file: " + configPath);
                registeredSettings.put(yamlConfig, yaml);
            } catch (IOException e) {
                log("Failed to save updated YAML configuration: " + configPath);
                throw YAJSIException.builder()
                        .message("Failed to save updated config file '" + configPath + "'")
                        .cause(e)
                        .build();
            }
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
    public File getConfigLocation(@NotNull Object yamlConfig) {
        log("Getting configuration file location for class: " + yamlConfig.getClass().getName());
        if (!isRegistered(yamlConfig)) {
            log("Configuration class not registered: " + yamlConfig.getClass().getName() + ". Registering now.");
            registerYAMLConfiguration(yamlConfig, false);
        }
        File configFile = getConfigFile(yamlConfig);
        log("Configuration file location resolved to: " + configFile.getPath());
        return configFile;
    }

    /**
     * Returns the YAML config corresponding to this YAML config class in {@link String} form.
     * @param yamlConfig the YAML config class to get the YAML config for
     * @return the serialized YAML config for the specified YAML config class
     */
    public String getYAMLString(@NotNull Object yamlConfig) {
        log("Getting serialized YAML string for class: " + yamlConfig.getClass().getName());
        if (!isRegistered(yamlConfig)) {
            log("Configuration class not registered: " + yamlConfig.getClass().getName() + ". Registering now.");
            registerYAMLConfiguration(yamlConfig, false);
        }
        String yamlString = registeredSettings.get(yamlConfig).saveToString();
        log("Successfully retrieved YAML string for class: " + yamlConfig.getClass().getName());
        return yamlString;
    }

    /**
     * Deletes the YAML config file corresponding to this YAML config class.
     * @param yamlConfig the YAML config file to delete the YAML config file for.
     * @return {@code true} if the YAML config file was successfully deleted, {@code false} if no file was found, or the deletion failed
     */
    public boolean deleteYAMLConfigurationFileFor(@NotNull Object yamlConfig) {
        log("Attempting to delete configuration file for class: " + yamlConfig.getClass().getName());
        File configFile = getConfigFile(yamlConfig);
        unregisterYAMLConfiguration(yamlConfig);
        boolean deleted = configFile.delete();
        if (deleted) {
            log("Successfully deleted configuration file: " + configFile.getPath());
        } else {
            log("Failed to delete configuration file: " + configFile.getPath() + ". File may not exist.");
        }
        return deleted;
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
    public ManualAdjustmentHelper makeManualAdjustmentsTo(@NotNull Object yamlConfig) throws YAJSIException {
        log("Preparing for manual adjustments to configuration for class: " + yamlConfig.getClass().getName());
        if (!isRegistered(yamlConfig)) {
            log("Configuration class not registered: " + yamlConfig.getClass().getName() + ". Registering now.");
            registerYAMLConfiguration(yamlConfig, false);
        }
        YamlConfiguration yaml = registeredSettings.get(yamlConfig);
        File configFile = getConfigFile(yamlConfig);

        return new ManualAdjustmentHelper() {
            @Override
            public YamlConfiguration getYAML() {
                log("Providing access to YAML configuration for manual adjustments for class: " + yamlConfig.getClass().getName());
                return yaml;
            }

            @Override
            public void returnResponsibility() {
                log("Returning responsibility for YAML configuration for class: " + yamlConfig.getClass().getName());
                try {
                    yaml.save(configFile);
                    log("YAML configuration saved after manual adjustments for class: " + yamlConfig.getClass().getName());
                    registerYAMLConfiguration(yamlConfig, false);
                    log("Configuration class re-registered after manual adjustments: " + yamlConfig.getClass().getName());
                } catch (IOException e) {
                    log("Failed to save YAML configuration after manual adjustments for class: " + yamlConfig.getClass().getName() + ". Exception: " + e.getMessage());
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
    public File getConfigFile(@NotNull Object yamlConfig) throws YAJSIException {
        log("Getting config file for class: " + yamlConfig.getClass().getName());
        Class<?> clazz = yamlConfig.getClass();
        if (!clazz.isAnnotationPresent(YAMLConfiguration.class)) {
            log("Class is not annotated with @YAMLConfiguration");
            throw YAJSIException.builder()
                    .message("Failed to get config file for '" + yamlConfig.getClass().getName() + "'!")
                    .cause(new IllegalArgumentException("Class must be annotated with @YAMLConfiguration"))
                    .build();
        }

        String customFilePath = clazz.getAnnotation(YAMLConfiguration.class).filePath();
        String customName = clazz.getAnnotation(YAMLConfiguration.class).name();

        String configPath = (customFilePath.isBlank()
                ? config.getConfigDirectory() + "/"
                : customFilePath)
                + (customName.isBlank()
                ? clazz.getSimpleName() + ".yaml"
                : customName
                );

        File config = new File(configPath);

        try {
            if (!config.exists()) {
                log("Config file does not exist; creating new file: " + configPath);
                if (!config.getParentFile().exists() && !config.getParentFile().mkdirs()) {
                    throw new IOException("Failed to create parent directory: " + config.getParentFile().getAbsolutePath());
                }
                if (!config.createNewFile()) {
                    throw new IOException("File already exists: " + configPath);
                }
            } else if (!config.isFile()) {
                log("Config path is not a file: " + configPath);
                throw new RuntimeException("File '" + configPath + "' is a directory!");
            }
        } catch (RuntimeException | IOException e) {
            log("Failed to create or validate config file: " + configPath + ", error: " + e.getMessage());
            throw YAJSIException.builder()
                    .message("Failed to get config file for '" + yamlConfig.getClass().getName() + "'!")
                    .cause(e)
                    .build();
        }
        log("Successfully obtained config file: " + configPath);
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
            log("Skipping already processed object to prevent infinite recursion.");
            return; // Prevent infinite recursion
        }
        processedObjects.add(yamlConfig);
        log("Processing YAML fields for object: " + yamlConfig.getClass().getName() + ", parentPath: '" + parentPath + "'.");

        Class<?> clazz = yamlConfig.getClass();
        String sectionPath = parentPath.isEmpty() ? "" : parentPath + ".";
        Set<String> yamlKeys = yaml.getKeys(false);
        log("Found YAML keys: " + yamlKeys);

        for (Field field : clazz.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (field.isAnnotationPresent(YAMLSetting.Ignore.class)
                    || Modifier.isFinal(modifiers)
                    || Modifier.isStatic(modifiers)) {
                log("Skipping field: " + field.getName() + " due to annotations or modifiers.");
                continue;
            }
            try {
                field.setAccessible(true);
            } catch (InaccessibleObjectException e) {
                log("Skipping field: " + field.getName() + " due to: " + e.getMessage());
                log("Skipping current object: " + clazz.getName());
                log("To ignore object annotate with @YAMLSetting.Ignore.");
                log("To include object initialize it!");
                return;
            }

            log("Processing field: " + field.getName());

            try {
                String yamlKey;
                String fullKey;
                Object fieldValue = field.get(yamlConfig);

                if (field.isAnnotationPresent(YAMLSetting.class)) {
                    String tmp = field.getAnnotation(YAMLSetting.class).name();
                    yamlKey = tmp.isBlank() ? field.getName() : tmp;
                    fullKey = sectionPath + yamlKey;
                    log("Field has YAMLSetting annotation. Using key: '" + yamlKey + "'.");

                    String[] comments = field.getAnnotation(YAMLSetting.class).comments();
                    if (comments.length > 0) {
                        yaml.setComments(fullKey, null);
                        yaml.setComments(fullKey, List.of(comments));  // Set comments for the key
                        log("Set comments for key: '" + fullKey + "'.");
                    }
                } else {
                    yamlKey = field.getName();
                    fullKey = sectionPath + yamlKey;
                }

                if (fieldValue == null) {
                    field.set(yamlConfig, field.getType().getConstructor().newInstance());
                    fieldValue = field.get(yamlConfig);
                }
                if (fieldValue == null) {
                    return;
                }

                if (isCustomObject(fieldValue)) {
                    log("Detected custom object in field: " + field.getName());
                    field.set(yamlConfig, fieldValue);
                    processYAMLFields(fieldValue, yaml, fullKey, processedObjects, overwrite);
                } else if (isListOfPrimitives(fieldValue)) {
                    log("Detected list of primitives in field: " + field.getName());
                    if (yaml.contains(fullKey) && !overwrite) {
                        List<?> yamlList = yaml.getList(fullKey);
                        field.set(yamlConfig, yamlList);
                        log("Set field value from existing YAML list for key: '" + fullKey + "'.");
                    } else {
                        yaml.set(fullKey, fieldValue);
                        log("Set YAML key: '" + fullKey + "' with field value.");
                    }
                } else {
                    log("Detected primitive or basic type in field: " + field.getName());
                    if (yaml.contains(fullKey) && !overwrite) {
                        field.set(yamlConfig, getValue(field.getType(), yaml.get(fullKey)));
                        log("Set field value from existing YAML value for key: '" + fullKey + "'.");
                    } else {
                        yaml.set(fullKey, fieldValue);
                        log("Set YAML key: '" + fullKey + "' with field value.");
                    }
                }

                yamlKeys.remove(yamlKey);
            } catch (Exception e) {
                log("Exception occurred while processing field: '" + field.getName() + "' - " + e.getMessage());
                throw YAJSIException.builder()
                        .message("Failed to process field '" + field.getName() + "'")
                        .cause(e)
                        .build();
            }
        }

        // Handle unused keys and add comments to unused keys
        for (String unusedKey : yamlKeys) {
            String unusedPath = sectionPath + unusedKey;
            if (yaml.contains(unusedPath)) {
                switch (config.updatingBehaviour) {
                    case MARK_UNUSED -> {
                        yaml.setComments(unusedPath, List.of(config.unusedSettingWarning));
                        log("Marking config value: '" + unusedPath + "' as unused.");
                    }
                    case REMOVE -> {
                        yaml.set(unusedPath, null);
                        log("Removing unused config value: '" + unusedPath + "'.");
                    }
                    case IGNORE -> log("Ignoring unused config value: '" + unusedPath + "'.");
                }
            }
        }
    }



    /**
     * Checks if the specified object is not a primitive object.
     * @param object the object to check
     * @return {@code false} if the specified object is a primitive object, otherwise {@code true}
     */
    private boolean isCustomObject(Object object) {
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
        log("Saving all registered YAML configuration classes.");
        registeredSettings.keySet().forEach(config -> {
            log("Saving configuration for class: " + config.getClass().getName());
            save(config);
        });
    }

    /**
     * Saves all values from the specified YAML config class to the classes corresponding YAML config file.
     * @param yamlConfig the YAML config class to save
     */
    public void save(@NotNull Object yamlConfig) throws YAJSIException {
        log("Attempting to save configuration for class: " + yamlConfig.getClass().getName());

        if (!isRegistered(yamlConfig)) {
            log("Configuration class not registered: " + yamlConfig.getClass().getName() + ". Registering now.");
            registerYAMLConfiguration(yamlConfig, false);
            return;
        }

        File configFile = getConfigFile(yamlConfig);
        log("Configuration file for class '" + yamlConfig.getClass().getName() + "' resolved to: " + configFile.getPath());

        YamlConfiguration yaml = registeredSettings.get(yamlConfig);
        HashSet<Object> processedObjects = new HashSet<>();

        log("Processing YAML fields for class: " + yamlConfig.getClass().getName());
        processYAMLFields(yamlConfig, yaml, "", processedObjects, true);

        try {
            log("Saving YAML configuration to file: " + configFile.getPath());
            yaml.save(configFile);
            log("Successfully saved YAML configuration for class: " + yamlConfig.getClass().getName());
        } catch (IOException e) {
            log("Failed to save YAML configuration to file for class: " + yamlConfig.getClass().getName() + ". Exception: " + e.getMessage());
            throw YAJSIException.builder()
                    .message("Failed to save YAML config to file for '" + yamlConfig.getClass().getName() + "' config class!")
                    .cause(e)
                    .build();
        }
    }

}
