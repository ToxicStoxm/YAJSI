package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.upgrading.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

import static com.toxicstoxm.YAJSI.SettingsManager.DEFAULT_SUPPLIERS;

public class SettingsBundleManager {
    private final HashMap<Version, UpgradeCallback> upgradeCallbacks = new HashMap<>();
    protected final HashMap<SettingsBundle, YamlConfiguration> registeredConfigs = new HashMap<>();

    public @Nullable YamlConfiguration upgrade(@NotNull SettingsBundle bundle, @NotNull YamlConfiguration yaml) throws IllegalStateException, UnsupportedOperationException {
        Version old = bundle.getVersion().fromString(yaml.getString(SettingsManager.getSettings().getVersionKey()));
        int cmp = bundle.getVersion().compareTo(old);
        if (cmp > 0) {
            UpgradeCallback cb = upgradeCallbacks.get(old);
            if (cb == null) {
                if (SettingsManager.getSettings().isAutoUpgrade()) {
                    return null;
                } else {
                    throw new IllegalStateException("Unable to find upgradeCallback for Version " + old + " bundle " + bundle.getClass().getName());
                }
            }
            return upgrade(bundle, cb.process(yaml, bundle.getId()));
        } else if (cmp == 0) {
            return yaml;
        } else {
            throw new UnsupportedOperationException("Downgrading configs is not supported!");
        }
    }

    public void registerUpgradeCallback(@NotNull UpgradeCallback cb, @NotNull Version base) throws UnsupportedOperationException {
        if (upgradeCallbacks.containsKey(base)) {
            throw new UnsupportedOperationException("Only one callback per base Version is allowed!");
        }
        upgradeCallbacks.put(base, cb);
    }

    public void registerUpgradeCallbacks(@NotNull Object o) {
        if (o instanceof Class<?> clazz) {
            try {
                Constructor<?> constructor = clazz.getConstructor();
                o = constructor.newInstance();
            } catch (Exception e) {
                throw new UnsupportedOperationException(clazz.getName() + " is not eligible for use as UpgradeCallbackBundle!", e);
            }
        }

        final Class<?> clazz = o.getClass();
        final Object upgraderBundle = o;

        for (Method m : clazz.getDeclaredMethods()) {
            try {
                if (m.isAnnotationPresent(Upgrader.class)) {
                    m.setAccessible(true);
                    Upgrader upgrader = m.getAnnotation(Upgrader.class);

                    Class<? extends VersionFactory<?>> factory = upgrader.factory();
                    Constructor<? extends VersionFactory<?>> factoryConstructor = factory.getConstructor();
                    VersionFactory<?> versionFactory = factoryConstructor.newInstance();

                    registerUpgradeCallback((old, id) -> {
                        try {
                            return (YamlConfiguration) m.invoke(upgraderBundle, old, id);
                        } catch (Throwable e) {
                            throw new IllegalStateException("Failed to use method: " + m.getName() + " from class: " + clazz.getName() + " as upgrade callback!", e);
                        }
                    }, versionFactory.fromString(upgrader.base()));
                }
            } catch (Throwable e) {
                throw new UnsupportedOperationException("Method: " + m.getName() + " from class: " + clazz.getName() + " is not eligible for use as UpgradeCallback!", e);
            }
        }
    }

    public void registerConfig(SettingsBundle config, @NotNull YamlConfiguration yaml) throws IllegalStateException, UnsupportedOperationException {
        boolean initial = !yaml.contains(SettingsManager.getSettings().getVersionKey());
        if (initial) {
            yaml.set(SettingsManager.getSettings().getVersionKey(), config.getVersion().toString());
        }

        Class<? extends SettingsBundle> clazz = config.getClass();
        if (registeredConfigs.isEmpty()) {
            if (clazz.isAnnotationPresent(UpgraderBundle.class)) {
                UpgraderBundle bundle = clazz.getAnnotation(UpgraderBundle.class);
                registerUpgradeCallbacks(bundle.upgraderBundle());
            } else {
                registerUpgradeCallbacks(config);
            }
        }

        YamlConfiguration upgraded = upgrade(config, yaml);

        boolean autoUpgraded = false;
        boolean cbUpgraded = !yaml.equals(upgraded);

        if (upgraded == null) {
            if (SettingsManager.getSettings().isAutoUpgrade()) {
                upgraded = yaml;
                yaml.set(SettingsManager.getSettings().getVersionKey(), config.getVersion().toString());
                autoUpgraded = true;
            } else {
                throw new IllegalStateException("Unable to auto upgrade: " + clazz.getName() + ", auto upgrading is disabled!");
            }
        }

        List<Object> processedObjects = new ArrayList<>();

        List<String> keys = new ArrayList<>(upgraded.getKeys(true));
        keys.remove(SettingsManager.getSettings().getVersionKey());

        loadValues(keys, processedObjects, config, upgraded);

        if (initial || config.isReadonly() && (autoUpgraded || cbUpgraded) && SettingsManager.getSettings().isSaveReadOnlyConfigOnVersionUpgrade() || !config.isReadonly() && !keys.isEmpty()) {
            for (String unused : keys) {
                if (!upgraded.contains(unused)) continue;
                switch (SettingsManager.getSettings().getAutoUpgradeBehaviour()) {
                    case REMOVE -> yaml.set(unused, null);
                    case MARK_UNUSED -> yaml.setComments(unused, List.of(SettingsManager.getSettings().getUnusedWarning()));
                }
            }

            try {
                upgraded.save(config.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        registeredConfigs.put(config, upgraded);
    }

    public void loadValues(@NotNull List<String> keys, @NotNull List<Object> processedObjects, @NotNull Object config, YamlConfiguration yaml) throws IllegalStateException {
        loadValues(keys, processedObjects, config, yaml, "");
    }

    public void loadValues(@NotNull List<String> keys, @NotNull List<Object> processedObjects, @NotNull Object config, YamlConfiguration yaml, String base) throws IllegalStateException {
        if (processedObjects.contains(config)) {
            return;
        }
        processedObjects.add(config);

        for (Field field : config.getClass().getDeclaredFields()) {
            if (isNotEligibleForConfig(field)) continue;

            try {
                field.setAccessible(true);

                String fullKey = getYAMLPath(field, base);
                keys.remove(fullKey);
                updateComments(field, fullKey, yaml);
                Object fieldValue = getFieldValue(config, field);

                boolean yamlHasKey = yaml.contains(fullKey);
                boolean checkEnv = SettingsManager.getSettings().isEnvOverwrites();

                if (TypeUtils.isListOfPrimitives(fieldValue)) {
                    List<?> value = yaml.getList(fullKey, (List<?>) fieldValue);

                    // Ensure all list elements are of the expected type
                    if (!TypeUtils.isListOfType(field, value)) {
                        throw new IllegalStateException("Type mismatch in YAML for field '" + field.getName() +
                                "': expected list of " + ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
                    }

                    if (!yamlHasKey) yaml.set(fullKey, fieldValue);
                    if (checkEnv) {
                        List<?> finalObject = EnvUtils.checkForEnvPrimitiveList(field, value);
                        if (!finalObject.equals(value) && processedObjects.getFirst() instanceof SettingsBundle bundle) {
                            bundle.setEnvSubstituted(field.getName());
                            value = finalObject;
                        }
                    }
                    field.set(config, value);
                    continue;
                }

                if (TypeUtils.isArrayOfPrimitives(fieldValue)) {
                    Object value = yaml.get(fullKey, fieldValue);

                    // If YAML returned a List, convert to array
                    if (value instanceof List<?> listValue) {
                        Class<?> componentType = fieldValue.getClass().getComponentType();
                        Object array = java.lang.reflect.Array.newInstance(componentType, listValue.size());
                        for (int i = 0; i < listValue.size(); i++) {
                            java.lang.reflect.Array.set(array, i, listValue.get(i));
                        }
                        value = array;
                    }

                    if (!yamlHasKey) yaml.set(fullKey, fieldValue);

                    if (checkEnv) {
                        Object finalArray = EnvUtils.checkForEnvPrimitiveArray(field, value);
                        if (!finalArray.equals(value) && processedObjects.getFirst() instanceof SettingsBundle bundle) {
                            bundle.setEnvSubstituted(field.getName());
                            value = finalArray;
                        }
                    }
                    field.set(config, value);
                    continue;
                }

                if (TypeUtils.isCustomObject(fieldValue)) {
                    loadValues(keys, processedObjects, fieldValue, yaml, fullKey);
                    continue;
                }

                Object value = getValue(field.getType(), yaml.get(fullKey, fieldValue));
                if (checkEnv) {
                    Object finalObject = EnvUtils.checkForEnvPrimitive(field, value);
                    if (!finalObject.equals(value) && processedObjects.getFirst() instanceof SettingsBundle bundle) {
                        bundle.setEnvSubstituted(field.getName());
                        value = finalObject;
                    }
                }
                field.set(config, value);

            } catch (InvocationTargetException | IllegalAccessException |
                     InstantiationException | NullPointerException | IllegalStateException e) {
                if (processedObjects.getFirst() instanceof SettingsBundle bundle) {
                    throw new IllegalStateException("Failed to register config! File: '" + bundle.getFile() + "' ID: '" + bundle.getId() + "' Version: '" + bundle.getVersion() + "'", e);
                }
                throw new IllegalStateException("Failed to register config: " + config.getClass().getName() + "!", e);
            }
        }
    }

    public void saveValues(@NotNull List<Object> processedObjects, @NotNull Object config, YamlConfiguration yaml) throws IllegalStateException {
        saveValues(processedObjects, config, yaml, "");
    }

    public void saveValues(@NotNull List<Object> processedObjects, @NotNull Object config, YamlConfiguration yaml, String base) throws IllegalStateException {
        if (processedObjects.contains(config)) {
            return;
        }
        processedObjects.add(config);

        for (Field field : config.getClass().getDeclaredFields()) {
            if (isNotEligibleForConfig(field)) continue;

            try {
                field.setAccessible(true);

                String fullKey = getYAMLPath(field, base);
                Object fieldValue = getFieldValue(config, field);

                if (TypeUtils.isCustomObject(fieldValue)) {
                    saveValues(processedObjects, fieldValue, yaml, fullKey);
                    return;
                }

                boolean checkEnv = SettingsManager.getSettings().isEnvOverwrites();

                if (!checkEnv || config instanceof SettingsBundle bundle && !bundle.isEnvSubstituted(field.getName())) {
                    yaml.set(fullKey, fieldValue);
                }

            } catch (InvocationTargetException | IllegalAccessException |
                     InstantiationException | NullPointerException | IllegalStateException e) {
                if (processedObjects.getFirst() instanceof SettingsBundle bundle) {
                    throw new IllegalStateException("Failed to register config! File: '" + bundle.getFile() + "' ID: '" + bundle.getId() + "' Version: '" + bundle.getVersion() + "'", e);
                }
                return;
            }
        }
    }

    private @NotNull Object getFieldValue(Object config, @NotNull Field field)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, IllegalStateException {
        Object value = field.get(config);
        if (value != null) return value;

        Class<?> type = field.getType();

        // Try direct match
        Supplier<?> supplier = DEFAULT_SUPPLIERS.get(type);

        // Try assignable (e.g., custom subclass of List)
        if (supplier == null) {
            for (Map.Entry<Class<?>, Supplier<?>> e : DEFAULT_SUPPLIERS.entrySet()) {
                if (e.getKey().isAssignableFrom(type)) {
                    supplier = e.getValue();
                    break;
                }
            }
        }

        Object instance;
        if (supplier != null) {
            instance = supplier.get();
        } else if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            instance = java.lang.reflect.Array.newInstance(componentType, 0);
        } else {

            // Fallback: try reflection
            try {
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                instance = constructor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "Cannot instantiate field '" + field.getName() +
                                "' of type " + type.getName() + ": no default supplier or no-args constructor found!", e
                );
            }
        }

        field.set(config, instance);
        return instance;
    }

    private void updateComments(@NotNull Field field, String fullKey, YamlConfiguration yaml) {
        if (field.isAnnotationPresent(YAMLSetting.class)) {
            String[] comments = field.getAnnotation(YAMLSetting.class).comments();
            if (comments.length > 0) {
                yaml.setComments(fullKey, null);
                yaml.setComments(fullKey, List.of(comments));  // Set comments for the key
            }
        }
    }

    private @NotNull String getYAMLPath(@NotNull Field field, String base) {
        String declaredName = "";
        if (field.isAnnotationPresent(YAMLSetting.class)) {
            declaredName = field.getAnnotation(YAMLSetting.class).name();
        }
        return base + (base.isBlank() ? "" : ".") + (declaredName.isBlank() ? field.getName() : declaredName);
    }

    private boolean isNotEligibleForConfig(@NotNull Field field) {
        int modifiers = field.getModifiers();
        return field.isAnnotationPresent(YAMLSetting.Ignore.class)
                || Modifier.isFinal(modifiers)
                || Modifier.isStatic(modifiers);
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

    public void save() {
        registeredConfigs.keySet().forEach(this::save);
    }

    public boolean save(SettingsBundle bundle) {
        if (bundle == null || bundle.isReadonly() || !registeredConfigs.containsKey(bundle)) {
            return false;
        }

        List<Object> processedObjects = new ArrayList<>();
        YamlConfiguration yaml = registeredConfigs.get(bundle);
        saveValues(processedObjects, bundle, yaml);

        try {
            yaml.save(bundle.getFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public @Nullable SettingsBundle getSettingsBundleInstance(UUID id) {
        for (SettingsBundle b : registeredConfigs.keySet()) {
            if (b.getId().equals(id)) return b;
        }
        return null;
    }

    public boolean save(@NotNull UUID id) {
        return save(getSettingsBundleInstance(id));
    }
}
