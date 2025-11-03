package com.toxicstoxm.YAJSI.parsing;

import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import com.toxicstoxm.YAJSI.YAMLSetting;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.TreeMap;

public class ParsingUtils {
    public static @NotNull String getYAMLPath(@NotNull Field field) {
        String declaredName = "";
        if (field.isAnnotationPresent(YAMLSetting.class)) {
            declaredName = field.getAnnotation(YAMLSetting.class).name();
        }
        return declaredName.isBlank() ? field.getName() : declaredName;
    }

    private static final TreeMap<Priority, YAMLParser> parsers = new TreeMap<>();

    static {
        parsers.put(new Priority(0, false), new PrimitiveParser());
        parsers.put(new Priority(Integer.MAX_VALUE, false), new ObjectParser());
    }

    public static Object parse(Field field, Object value, Object config, ConfigurationSection section) {
        for (YAMLParser parser : parsers.sequencedValues()) {
            if (parser.canParse(field.getClass())) {
                return parser.deserialize(field, value, config, section);
            }
        }
        return null;
    }

    public static void serialize(Field field, Object value, Object config, ConfigurationSection section) {

    }
}
