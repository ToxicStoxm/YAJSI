package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.upgrading.UpgradeCallback;
import com.toxicstoxm.YAJSI.upgrading.Version;
import lombok.AccessLevel;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public class SettingsManager {
    protected static final Map<Class<?>, Supplier<?>> DEFAULT_SUPPLIERS = new HashMap<>();

    static {
        DEFAULT_SUPPLIERS.put(List.class, ArrayList::new);
        DEFAULT_SUPPLIERS.put(Map.class, HashMap::new);
        DEFAULT_SUPPLIERS.put(Collection.class, ArrayList::new);

        DEFAULT_SUPPLIERS.put(ArrayList.class, ArrayList::new);
        DEFAULT_SUPPLIERS.put(HashSet.class, HashSet::new);
        DEFAULT_SUPPLIERS.put(LinkedHashSet.class, LinkedHashSet::new);
        DEFAULT_SUPPLIERS.put(HashMap.class, HashMap::new);
        DEFAULT_SUPPLIERS.put(LinkedHashMap.class, LinkedHashMap::new);
        DEFAULT_SUPPLIERS.put(LinkedList.class, LinkedList::new);

        DEFAULT_SUPPLIERS.put(Integer.class, () -> 0);
        DEFAULT_SUPPLIERS.put(String.class, () -> "");
        DEFAULT_SUPPLIERS.put(Long.class, () -> 0L);
        DEFAULT_SUPPLIERS.put(Float.class, () -> 0.0F);
        DEFAULT_SUPPLIERS.put(Double.class, () -> 0.0D);
        DEFAULT_SUPPLIERS.put(Boolean.class, () -> false);
    }

    private static SettingsManager instance;

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager(SettingsManagerConfig.getDefaults());
        }
        return instance;
    }

    public static SettingsManagerConfig getSettings() {
        return getInstance().settings.toBuilder().done();
    }

    @Contract(" -> new")
    public static @NotNull SettingsManagerBlueprint configure() {
        if (instance != null) {
            return new SettingsManagerBlueprint(getSettings());
        }

        return new SettingsManagerBlueprint();
    }

    public static class SettingsManagerBlueprint extends SettingsManagerConfig.SettingsManagerConfigBuilder {

        public SettingsManagerBlueprint() {
            this(SettingsManagerConfig.getDefaults());
        }

        public SettingsManagerBlueprint(SettingsManagerConfig existingConfig) {
            envOverwrites(existingConfig.isEnvOverwrites());
            saveReadOnlyConfigOnVersionUpgrade(existingConfig.isSaveReadOnlyConfigOnVersionUpgrade());
            versionKey(existingConfig.getVersionKey());
            autoUpgrade(existingConfig.isAutoUpgrade());
            autoUpgradeBehaviour(existingConfig.getAutoUpgradeBehaviour());
            unusedWarning(existingConfig.getUnusedWarning());
        }

        @Override
        public SettingsManagerConfig done() {
            SettingsManagerConfig conf = super.done();
            if (instance == null) {
                instance = new SettingsManager(conf);
            } else {
                instance.setSettings(conf);
            }
            return conf;
        }
    }

    @Setter(AccessLevel.PRIVATE)
    private SettingsManagerConfig settings;

    private final HashMap<String, SettingsBundleManager> registeredBundles = new HashMap<>();

    private SettingsManager(SettingsManagerConfig settings) {
        this.settings = settings;
    }

    public void registerUpgradeCallback(Class<? extends SettingsBundle> bundle, UpgradeCallback cb, Version base) throws UnsupportedOperationException {
        getBundleManager(bundle).registerUpgradeCallback(cb, base);
    }

    public void registerUpgradeCallbacks(@NotNull SettingsBundle bundle) {
        getBundleManager(bundle.getClass()).registerUpgradeCallbacks(bundle);
    }

    public void registerUpgradeCallbacks(@NotNull Class<? extends SettingsBundle> bundle, @NotNull Object o) {
        getBundleManager(bundle).registerUpgradeCallbacks(o);
    }

    public UUID registerConfig(@NotNull SettingsBundle config) throws IllegalStateException, UnsupportedOperationException {
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

    private SettingsBundleManager getBundleManager(@NotNull Class<? extends SettingsBundle> bundle) {
        if (!registeredBundles.containsKey(bundle.getTypeName())) {
            registeredBundles.put(bundle.getTypeName(), new SettingsBundleManager());
        }
        return registeredBundles.get(bundle.getTypeName());
    }

    public <T> @Nullable T getSettingsBundleInstance(@NotNull Class<T> bundle, UUID id) {
        return bundle.cast(registeredBundles.get(bundle.getTypeName()).getSettingsBundleInstance(id));
    }

    public void save() {
        registeredBundles.values().forEach(SettingsBundleManager::save);
    }

    public boolean save(@NotNull SettingsBundle bundle) {
        if (!registeredBundles.containsKey(bundle.getClass().getTypeName())) return false;
        return registeredBundles.get(bundle.getClass().getTypeName()).save(bundle);
    }
    
    public boolean save(@NotNull Class<? extends SettingsBundle> bundle, UUID id) {
        if (!registeredBundles.containsKey(bundle.getTypeName())) return false;
        return registeredBundles.get(bundle.getTypeName()).save(id);
    }

    public <T> void addSupplier(Class<T> clazz, Supplier<T> supplier) {
        DEFAULT_SUPPLIERS.put(clazz, supplier);
    }
}
