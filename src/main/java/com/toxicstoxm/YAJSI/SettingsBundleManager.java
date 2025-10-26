package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.upgrading.UpgradeCallback;
import com.toxicstoxm.YAJSI.upgrading.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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

    public void registerUpgradeCallback(UpgradeCallback cb, Version base) throws UnsupportedOperationException {
        if (upgradeCallbacks.containsKey(base)) {
            throw new UnsupportedOperationException("Only one callback per base Version is allowed!");
        }
        upgradeCallbacks.put(base, cb);
    }

    public void registerConfig(SettingsBundle config, @NotNull YamlConfiguration yaml) throws IllegalStateException, UnsupportedOperationException {
        boolean initial = !yaml.contains(SettingsManager.getSettings().getVersionKey());
        if (initial) {
            yaml.set(SettingsManager.getSettings().getVersionKey(), config.getVersion().toString());
        }

        YamlConfiguration upgraded = upgrade(config, yaml);

        boolean autoUpgraded = false;

        if (upgraded == null) {
            if (SettingsManager.getSettings().isAutoUpgrade()) {
                upgraded = yaml;
                yaml.set(SettingsManager.getSettings().getVersionKey(), config.getVersion().toString());
                autoUpgraded = true;
            } else {
                throw new IllegalStateException("Unable to auto upgrade: " + config.getClass().getName() + ", auto upgrading is disabled!");
            }
        }

        List<Object> processedObjects = new ArrayList<>();

        List<String> keys = new ArrayList<>(yaml.getKeys(true));
        keys.remove(SettingsManager.getSettings().getVersionKey());

        loadValues(keys, processedObjects, config, upgraded);

        if (initial || config.isReadonly() && autoUpgraded && SettingsManager.getSettings().isSaveReadOnlyConfigOnVersionUpgrade() || !config.isReadonly() && !keys.isEmpty()) {
            for (String unused : keys) {
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
            if (!isEligibleForConfig(field)) continue;

            try {
                field.setAccessible(true);

                String fullKey = getYAMLPath(field, base);
                keys.remove(fullKey);
                updateComments(field, fullKey, yaml);

                Object fieldValue = getFieldValue(config, field);

                if (isCustomObject(fieldValue)) {
                    loadValues(keys, processedObjects, fieldValue, yaml, fullKey);
                    continue;
                }

                boolean yamlHasKey = yaml.contains(fullKey);
                boolean checkEnv = SettingsManager.getSettings().isEnvOverwrites();

                if (isListOfPrimitives(fieldValue)) {
                    List<?> value = yaml.getList(fullKey, (List<?>) fieldValue);

                    // Ensure all list elements are of the expected type
                    if (!isListOfType(field, value)) {
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
                } else {
                    Object value = getValue(field.getType(), yaml.get(fullKey, fieldValue));
                    if (!yamlHasKey) yaml.set(fullKey, fieldValue);
                    if (checkEnv) {
                        Object finalObject = EnvUtils.checkForEnvPrimitive(field, value);
                        if (!finalObject.equals(value) && processedObjects.getFirst() instanceof SettingsBundle bundle) {
                            bundle.setEnvSubstituted(field.getName());
                            value = finalObject;
                        }
                    }
                    field.set(config, value);
                }

            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException |
                     InstantiationException | NullPointerException e) {
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
            if (!isEligibleForConfig(field)) continue;

            try {
                field.setAccessible(true);

                String fullKey = getYAMLPath(field, base);
                Object fieldValue = getFieldValue(config, field);

                if (isCustomObject(fieldValue)) {
                    saveValues(processedObjects, fieldValue, yaml, fullKey);
                    return;
                }

                boolean checkEnv = SettingsManager.getSettings().isEnvOverwrites();

                if (!checkEnv || config instanceof SettingsBundle bundle && !bundle.isEnvSubstituted(field.getName())) {
                    yaml.set(fullKey, fieldValue);
                }

            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException |
                     InstantiationException | NullPointerException e) {
                if (processedObjects.getFirst() instanceof SettingsBundle bundle) {
                    throw new IllegalStateException("Failed to register config! File: '" + bundle.getFile() + "' ID: '" + bundle.getId() + "' Version: '" + bundle.getVersion() + "'", e);
                }
                return;
            }
        }
    }

    private @NotNull Object getFieldValue(Object config, @NotNull Field field) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException, NullPointerException {
        Object fieldValue = field.get(config);
        if (fieldValue == null) {
            field.set(config, field.getType().getConstructor().newInstance());
            fieldValue = field.get(config);
        }
        if (fieldValue == null) {
            throw new NullPointerException("Unable to initialize field value!");
        }
        return fieldValue;
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

    private boolean isEligibleForConfig(@NotNull Field field) {
        int modifiers = field.getModifiers();
        return !(field.isAnnotationPresent(YAMLSetting.Ignore.class)
                || Modifier.isFinal(modifiers)
                || Modifier.isStatic(modifiers));
    }

    /**
     * Checks if the specified object is not a primitive object.
     * @param object the object to check
     * @return {@code false} if the specified object is a primitive object, otherwise {@code true}
     */
    private boolean isCustomObject(@NotNull Object object) {
        Class<?> clazz = object.getClass();
        return !(clazz.isPrimitive() || clazz.equals(String.class) || clazz.equals(Boolean.class) || Number.class.isAssignableFrom(clazz) || clazz.isArray() || isListOfPrimitives(object));
    }

    /**
     * Checks if the specified object is a list of primitive objects.
     * @param object the object to check
     * @return {@code true} if the specified object is a list of primitive objects, otherwise {@code true}
     */
    private boolean isListOfPrimitives(@Nullable Object object) {
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

    public static boolean isListOfType(@NotNull Field field, @Nullable Object value) {
        if (!(value instanceof List<?> list))
            return false;

        // Determine declared element type
        Class<?> elementType = Object.class;
        if (field.getGenericType() instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 1 && args[0] instanceof Class<?> c)
                elementType = c;
        }

        // Empty lists are always fine
        if (list.isEmpty())
            return true;

        // Check that every element matches the declared type
        for (Object elem : list) {
            if (elem != null && !elementType.isInstance(elem))
                return false;
        }

        return true;
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
