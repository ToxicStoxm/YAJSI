package com.toxicstoxm.YAJSI.parsing;

import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import com.toxicstoxm.YAJSI.EnvUtils;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@ParserPriority
public class PrimitiveParser implements YAMLParser {
    private static final Map<Class<?>, Supplier<?>> DEFAULT_SUPPLIERS = new HashMap<>();
    private static final Map<Class<?>, YAMLGetter<ConfigurationSection, String, ?>> YAML_GETTERS = new HashMap<>();

    private static final ConcurrentHashMap<Field, String> YAML_PATH_CACHE = new ConcurrentHashMap<>();

    static {
        DEFAULT_SUPPLIERS.put(Integer.class, () -> 0);
        DEFAULT_SUPPLIERS.put(String.class, () -> "");
        DEFAULT_SUPPLIERS.put(Long.class, () -> 0L);
        DEFAULT_SUPPLIERS.put(Float.class, () -> 0.0F);
        DEFAULT_SUPPLIERS.put(Double.class, () -> 0.0D);
        DEFAULT_SUPPLIERS.put(Boolean.class, () -> false);

        YAML_GETTERS.put(Integer.class, ConfigurationSection::getInt);
        YAML_GETTERS.put(String.class, ConfigurationSection::getString);
        YAML_GETTERS.put(Long.class, ConfigurationSection::getLong);
        YAML_GETTERS.put(Float.class, (param1, param2) -> ((Double) param1.getDouble(param2)).floatValue());
        YAML_GETTERS.put(Double.class, ConfigurationSection::getDouble);
        YAML_GETTERS.put(Boolean.class, ConfigurationSection::getBoolean);
    }


    @Override
    public boolean canParse(@NotNull Class<?> clazz) {
        return clazz.isPrimitive() || Number.class.isAssignableFrom(clazz) || clazz.equals(Boolean.class) || clazz.equals(String.class);
    }

    @Override
    public @NotNull Object deserialize(@NotNull Field field, @Nullable Object value, @NotNull Object config, @NotNull ConfigurationSection section) {
        Class<?> clazz = field.getClass();

        if (SettingsManager.getSettings().isEnvOverwrites()) {
            Object tmp = EnvUtils.checkForEnvPrimitive(field, value);
            if (tmp != null && tmp.equals(value) && config instanceof SettingsBundle bundle) {
                bundle.setEnvSubstituted(field.getName());
            }
            value = tmp;
        }

        if (value == null) {
            value = DEFAULT_SUPPLIERS.get(clazz).get();
        }

        String ymlKey = getYAMLKey(field);

        if (section.contains(ymlKey)) {
            value = YAML_GETTERS.get(clazz).get(section, ymlKey);
        }

        return value;
    }

    @Override
    public void serialize(@NotNull Field field, @Nullable Object value, @NotNull Object config, @NotNull ConfigurationSection section) {
        if (config instanceof SettingsBundle bundle && bundle.isEnvSubstituted(field.getName())) {
            return;
        }

        section.set(getYAMLKey(field), value);
    }

    private String getYAMLKey(@NotNull Field field) {
        if (YAML_PATH_CACHE.containsKey(field)) {
            YAML_PATH_CACHE.put(field, ParsingUtils.getYAMLPath(field));
        }
        return YAML_PATH_CACHE.get(field);
    }
}
