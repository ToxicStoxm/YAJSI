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

    public void registerConfig(SettingsBundle config, YamlConfiguration yaml) throws IllegalStateException {
        YamlConfiguration upgraded = upgrade(config, yaml);
        try {
            upgraded.save(config.getFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Object> processedObjects = new ArrayList<>();
        loadValues(processedObjects, config, upgraded, "");
        registeredConfigs.put(config, yaml);
    }

    public void loadValues(@NotNull List<Object> processedObjects, @NotNull Object config, YamlConfiguration yaml, String base) throws IllegalStateException {
        if (processedObjects.contains(config)) {
            return;
        }
        processedObjects.add(config);

        for (Field field : config.getClass().getDeclaredFields()) {
            if (!isEligibleForConfig(field)) continue;

            try {
                field.setAccessible(true);

                String fullKey = getYAMLPath(field, base.isEmpty() ? "" : base + ".");
                updateComments(field, fullKey, yaml);

                Object fieldValue = getFieldValue(config, field);

                if (isCustomObject(fieldValue)) {
                    //field.set(config, fieldValue);
                    loadValues(processedObjects, fieldValue, yaml, fullKey);
                    return;
                }

                boolean yamlHasKey = yaml.contains(fullKey);
                boolean checkEnv = SettingsManager.getInstance().settings.isEnvOverwrites();

                if (isListOfPrimitives(fieldValue)) {
                    Object value = yamlHasKey ? yaml.getList(fullKey) : fieldValue;
                    if (!yamlHasKey) yaml.set(fullKey, fieldValue);
                    if (checkEnv) {
                        Object finalObject = EnvUtils.checkForEnvPrimitiveList(field, fieldValue);
                        if (!finalObject.equals(value) && processedObjects.getFirst() instanceof SettingsBundle bundle) {
                            bundle.setEnvSubstituted(field.getName());
                        }
                        field.set(config, finalObject);
                    }
                    field.set(config, value);
                } else {
                    Object value = yamlHasKey ? getValue(field.getType(), yaml.get(fullKey)) : fieldValue;
                    if (!yamlHasKey) yaml.set(fullKey, fieldValue);
                    if (checkEnv) {
                        Object finalObject = EnvUtils.checkForEnvPrimitive(field, fieldValue);
                        if (!finalObject.equals(value) && processedObjects.getFirst() instanceof SettingsBundle bundle) {
                            bundle.setEnvSubstituted(field.getName());
                        }
                        field.set(config, finalObject);
                    }
                    field.set(config, value);
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

    public void saveValues(@NotNull List<Object> processedObjects, @NotNull Object config, YamlConfiguration yaml, String base) throws IllegalStateException {
        if (processedObjects.contains(config)) {
            return;
        }
        processedObjects.add(config);

        for (Field field : config.getClass().getDeclaredFields()) {
            if (!isEligibleForConfig(field)) continue;

            try {
                field.setAccessible(true);

                String fullKey = getYAMLPath(field, base.isEmpty() ? "" : base + ".");
                Object fieldValue = getFieldValue(config, field);

                if (isCustomObject(fieldValue)) {
                    //field.set(config, fieldValue);
                    saveValues(processedObjects, fieldValue, yaml, fullKey);
                    return;
                }

                boolean checkEnv = SettingsManager.getInstance().settings.isEnvOverwrites();

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
        return base + "." + (declaredName.isBlank() ? field.getName() : declaredName);
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

    public void save() {
        registeredConfigs.keySet().forEach(this::save);
    }

    public boolean save(SettingsBundle bundle) {
        if (bundle == null || bundle.isReadonly() || !registeredConfigs.containsKey(bundle)) {
            return false;
        }

        List<Object> processedObjects = new ArrayList<>();
        YamlConfiguration yaml = registeredConfigs.get(bundle);
        loadValues(processedObjects, bundle, yaml, "");


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
