package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import com.toxicstoxm.YAJSI.serializing.ExternalYAMLSerializer;
import com.toxicstoxm.YAJSI.serializing.SerializableWith;
import com.toxicstoxm.YAJSI.serializing.YAMLSerializable;
import com.toxicstoxm.YAJSI.upgrading.*;
import com.toxicstoxm.YAJSI.utils.EnvUtils;
import com.toxicstoxm.YAJSI.utils.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

import static com.toxicstoxm.YAJSI.utils.TypeUtils.DEFAULT_SUPPLIERS;


public class SettingsBundleManager {
    private final HashMap<Version, UpgradeCallback> upgradeCallbacks = new HashMap<>();
    protected final HashMap<SettingsBundle, YamlConfiguration> registeredConfigs = new HashMap<>();
    private static final HashMap<Class<?>, ExternalYAMLSerializer<Object>> EXTERNAL_SERIALIZER_CACHE = new HashMap<>();

    public @NotNull UpgradedYamlConfiguration upgrade(@NotNull SettingsBundle bundle, @NotNull YamlConfiguration yaml) throws IllegalStateException, UnsupportedOperationException {
        Version old = bundle.getVersion().fromString(yaml.getString(SettingsManager.getSettings().getVersionKey()));
        int cmp = bundle.getVersion().compareTo(old);
        if (cmp > 0) {
            UpgradeCallback cb = upgradeCallbacks.get(old);
            if (cb == null) {
                if (SettingsManager.getSettings().isAutoUpgrade()) {
                    return new UpgradedYamlConfiguration(yaml, false, false);
                } else {
                    throw new IllegalStateException("Unable to find upgradeCallback for Version " + old + " bundle " + bundle.getClass().getName());
                }
            }
            UpgradedYamlConfiguration upgraded = upgrade(bundle, cb.process(yaml, bundle.getId()));
            return new UpgradedYamlConfiguration(upgraded.yaml(), upgraded.upToDate(), true);
        } else if (cmp == 0) {
            return new UpgradedYamlConfiguration(yaml, true, false);
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

        UpgradedYamlConfiguration upgradedYaml = upgrade(config, yaml);
        YamlConfiguration upgraded = upgradedYaml.yaml();

        boolean autoUpgraded = false;

        if (!upgradedYaml.upToDate()) {
            if (SettingsManager.getSettings().isAutoUpgrade()) {
                upgraded.set(SettingsManager.getSettings().getVersionKey(), config.getVersion().toString());
                autoUpgraded = true;
            } else {
                throw new IllegalStateException("Unable to auto upgrade: " + clazz.getName() + ", auto upgrading is disabled!");
            }
        }

        List<Object> processedObjects = new ArrayList<>();

        List<String> keys = new ArrayList<>(upgraded.getKeys(true));
        keys.remove(SettingsManager.getSettings().getVersionKey());

        loadValues(keys, processedObjects, config, upgraded);

        if (initial || config.isReadonly() && (autoUpgraded || upgradedYaml.cbUpgraded()) && SettingsManager.getSettings().isSaveReadOnlyConfigOnVersionUpgrade() || !config.isReadonly() && !keys.isEmpty()) {
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

    public void loadValues(@NotNull List<String> keys, @NotNull List<Object> processedObjects, @NotNull Object config, ConfigurationSection yaml) throws IllegalStateException {
        loadValues(keys, processedObjects, config, yaml, "");
    }

    public void loadValues(@NotNull List<String> keys, @NotNull List<Object> processedObjects, @NotNull Object config, ConfigurationSection yaml, String base) throws IllegalStateException {
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
                boolean checkEnv = SettingsManager.getSettings().isEnableOverwriters();

                if (fieldValue instanceof YAMLSerializable serializer) {
                    ConfigurationSection section = yaml.getConfigurationSection(fullKey);
                    Object o = null;
                    if (section != null) {
                        o = serializer.deserialize(section);
                        keys.removeAll(section.getKeys(true).stream().map(s -> fullKey + "." + s).toList());
                    }

                    fieldValue = o == null ? fieldValue : o;

                    if (!yamlHasKey) {
                        yaml.set(fullKey, ((YAMLSerializable) fieldValue).serializeSelf());
                    }

                    field.set(config, fieldValue);
                    continue;
                }

                if (hasExternalSerializer(field)) {
                    ExternalYAMLSerializer<Object> serializer = getExternalSerializer(field);
                    if (serializer != null) {
                        ConfigurationSection section = yaml.getConfigurationSection(fullKey);
                        Object o = null;
                        if (section != null) {
                            o = serializer.deserialize(section);
                            keys.removeAll(section.getKeys(true).stream().map(s -> fullKey + "." + s).toList());
                        }

                        fieldValue = o == null ? fieldValue : o;

                        if (!yamlHasKey) {
                            yaml.set(fullKey, serializer.serialize(fieldValue));
                        }

                        field.set(config, fieldValue);
                        continue;
                    }
                }

                if (TypeUtils.isListOfPrimitives(field, fieldValue)) {
                    List<?> value = yaml.getList(fullKey, (List<?>) fieldValue);

                    // Ensure all list elements are of the expected type
                    if (!TypeUtils.isListOfType(field, value)) {
                        throw new IllegalStateException("Type mismatch in YAML for field '" + field.getName() +
                                "': expected list of " + ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
                    }

                    if (!yamlHasKey) {
                        yaml.set(fullKey, fieldValue);
                        // Ensure value is not default immutable list
                        Supplier<?> supplier = DEFAULT_SUPPLIERS.get(field.getType());
                        if (supplier != null) {
                            List<?> tmp = (List<?>) supplier.get();
                            tmp.addAll((Collection) value);
                            value = tmp;
                        }
                    }
                    if (checkEnv) {
                        List<?> finalObject = EnvUtils.checkForEnvPrimitiveList(field, value);
                        if (!finalObject.equals(value) && processedObjects.getFirst() instanceof SettingsBundle bundle) {
                            bundle.setEnvSubstituted(field.getName());
                            value = finalObject;
                        }
                    }
                    field.set(config, value);
                    continue;
                } else if (fieldValue instanceof List<?> list) {

                    // Can maybe be replaced with yaml.getMapList because
                    // test:
                    // - hello: "something"
                    // - hello: "something2"
                    // - hello: "something5"
                    // list of custom objects will always produce map

                    // load list from YAML (unknown type)
                    List<?> loaded = yaml.getList(fullKey);

                    // List from fieldValue
                    // Suppressed because if isPrimitiveList fails, it must be List<Object>
                    @SuppressWarnings("unchecked")
                    List<Object> value = (List<Object>) list;

                    // If loaded list is not null (so it exists)
                    if (loaded != null && field.getGenericType() instanceof ParameterizedType pt) {
                        // clear existing list from field value
                        value = (List<Object>) DEFAULT_SUPPLIERS.get(field.getType()).get();

                        // Get Type parameter type
                        Class<?> type = (Class<?>) pt.getActualTypeArguments()[0];
                        if (type.equals(ConfigurationSection.class)) {
                            // First try by assuming list of config sections
                            for (ConfigurationSection section : (List<ConfigurationSection>) loaded) {
                                // Instantiate new value by using the type param type
                                Object o = getFieldValue(type);
                                // use existing load function
                                loadValues(keys, processedObjects, o, section);
                                value.add(o);
                            }
                        } else {
                            // Assume List of linked hash maps
                            List<LinkedHashMap<String, String>> mapList = (List<LinkedHashMap<String, String>>) loaded;
                            for (LinkedHashMap<String, String> map : mapList) {
                                // Convert hashmaps back into config sections to be able to use existing load function
                                // This could be prevented by writing a wrapper which under the hood can be a YAML config or a hashmap
                                // Since inner workings are similar enough (maybe)
                                ConfigurationSection section = new YamlConfiguration();
                                for (Map.Entry<String, String> entry : map.entrySet()) {
                                    section.set(entry.getKey(), entry.getValue());
                                }
                                // Instantiate new object vie type param type
                                Object o = getFieldValue(type);
                                // load using existing function
                                loadValues(keys, processedObjects, o, section);
                                value.add(o);
                            }
                        }
                    }

                    // Ensure all list elements are of the expected type
                    if (!TypeUtils.isListOfType(field, value)) {
                        throw new IllegalStateException("Type mismatch in YAML for field '" + field.getName() +
                                "': expected list of " + ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
                    }

                    // if YAML doesn't have key yet
                    if (!yamlHasKey) {
                        // serialize objects loaded from list (field value)
                        // By using the same loading function
                        List<ConfigurationSection> serialized = new ArrayList<>();
                        for (Object listObject : list) {
                            ConfigurationSection section = new YamlConfiguration();
                            loadValues(keys, processedObjects, listObject, section);
                            serialized.add(section);
                        }
                        yaml.set(fullKey, serialized);

                        Supplier<?> supplier = DEFAULT_SUPPLIERS.get(field.getType());
                        if (supplier != null) {
                            List<Object> tmp = (List<Object>) DEFAULT_SUPPLIERS.get(field.getType()).get();
                            tmp.addAll(value);
                            value = tmp;
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

                if (!yamlHasKey) yaml.set(fullKey, fieldValue);

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

    public void saveValues(@NotNull List<Object> processedObjects, @NotNull Object config, ConfigurationSection yaml) throws IllegalStateException {
        saveValues(processedObjects, config, yaml, "");
    }

    public void saveValues(@NotNull List<Object> processedObjects, @NotNull Object config, ConfigurationSection yaml, String base) throws IllegalStateException {
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

                if (fieldValue instanceof YAMLSerializable serializable) {
                    ConfigurationSection serialized = serializable.serializeSelf();
                    yaml.set(fullKey, serialized);
                    return;
                }

                if (hasExternalSerializer(field)) {
                    ExternalYAMLSerializer<Object> serializer = getExternalSerializer(field);
                    if (serializer != null) {
                        ConfigurationSection serialized = serializer.serialize(fieldValue);
                        yaml.set(fullKey, serialized);
                        return;
                    }
                }

                if (fieldValue instanceof List<?> list && !TypeUtils.isListOfPrimitives(field, fieldValue)) {
                    List<ConfigurationSection> serialized = new ArrayList<>();
                    for (Object listObject : list) {
                        ConfigurationSection section = new YamlConfiguration();
                        saveValues(processedObjects, listObject, section);
                        serialized.add(section);
                    }
                    yaml.set(fullKey, serialized);
                    return;
                }

                if (TypeUtils.isCustomObject(fieldValue) && !TypeUtils.isListOfPrimitives(field, fieldValue)) {
                    saveValues(processedObjects, fieldValue, yaml, fullKey);
                    return;
                }

                boolean checkEnv = SettingsManager.getSettings().isEnableOverwriters();

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

        Object instance = getFieldValue(field.getType());
        field.set(config, instance);
        return instance;
    }

    private @NotNull Object getFieldValue(@NotNull Class<?> type)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, IllegalStateException {
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
                Constructor<?> constructor = type.getConstructor();
                constructor.setAccessible(true);
                instance = constructor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "Cannot instantiate field '" + type.getName() +
                                "' of type " + type.getName() + ": no default supplier or no-args constructor found!", e
                );
            }
        }
        return instance;
    }

    private void updateComments(@NotNull Field field, String fullKey, ConfigurationSection yaml) {
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
    private @NotNull Object getValue(@NotNull Type desired, @NotNull Object current) {
        if (!current.getClass().equals(Float.class) && (desired.equals(float.class) || desired.equals(Float.class))) {
            return ((Double) current).floatValue();
        }
        return current;
    }

    private boolean hasExternalSerializer(@NotNull Field field) {
        Class<?> typeClazz = field.getType();
        return EXTERNAL_SERIALIZER_CACHE.containsKey(typeClazz) || typeClazz.isAnnotationPresent(SerializableWith.class);
    }

    private @Nullable ExternalYAMLSerializer<Object> getExternalSerializer(@NotNull Field field) {
        Class<?> typeClazz = field.getType();
        if (!EXTERNAL_SERIALIZER_CACHE.containsKey(typeClazz)) {
            Class<? extends ExternalYAMLSerializer<Object>> serializerClazz = (Class<? extends ExternalYAMLSerializer<Object>>) typeClazz.getAnnotation(SerializableWith.class).serializer();

            try {
                Constructor<? extends ExternalYAMLSerializer<Object>> constructor = serializerClazz.getConstructor();
                constructor.setAccessible(true);

                EXTERNAL_SERIALIZER_CACHE.put(typeClazz, constructor.newInstance());
            } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                     IllegalAccessException | IllegalArgumentException e) {
                return null;
            }
        }
        return EXTERNAL_SERIALIZER_CACHE.get(typeClazz);
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
