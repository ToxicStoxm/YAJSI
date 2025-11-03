package com.toxicstoxm.YAJSI.parsing;

import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

@ParserPriority()
public class ObjectParser implements YAMLParser {
    private static final ConcurrentHashMap<Field, String> YAML_PATH_CACHE = new ConcurrentHashMap<>();


    @Override
    public boolean canParse(Class<?> clazz) {
        return false;
    }

    @Override
    public Object deserialize(Field field, Object value, Object config, ConfigurationSection section) {
        if (value == null) {
            try {
                value = getValue(field);
            } catch (Exception e) {
                return null;
            }
        }
        String key = getYAMLKey(field);

        ConfigurationSection subSection = section.isConfigurationSection(key) ? section.getConfigurationSection(key) : section.createSection(key);

        for (Field f : field.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object fieldValue = f.get(value);
                ParsingUtils.parse(f, fieldValue, config, subSection);
            } catch (Exception _) {}
        }

        return value;
    }

    @Override
    public void serialize(Field field, Object value, Object config, ConfigurationSection section) {
        String key = getYAMLKey(field);

        ConfigurationSection subSection = section.isConfigurationSection(key) ? section.getConfigurationSection(key) : section.createSection(key);

        for (Field f : field.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object fieldValue = f.get(value);
                ParsingUtils.serialize(f, fieldValue, config, subSection);
            } catch (Exception _) {}
        }
    }

    private @NotNull Object getValue(@NotNull Field field) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        Class<?> type = field.getType();
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private String getYAMLKey(@NotNull Field field) {
        if (YAML_PATH_CACHE.containsKey(field)) {
            YAML_PATH_CACHE.put(field, ParsingUtils.getYAMLPath(field));
        }
        return YAML_PATH_CACHE.get(field);
    }
}
