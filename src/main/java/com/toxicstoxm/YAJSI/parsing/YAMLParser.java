package com.toxicstoxm.YAJSI.parsing;

import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;

import java.lang.reflect.Field;

public interface YAMLParser {

    boolean canParse(Class<?> clazz);
    Object deserialize(Field field, Object value, Object config, ConfigurationSection section);
    void serialize(Field field, Object value, Object config, ConfigurationSection section);
}

