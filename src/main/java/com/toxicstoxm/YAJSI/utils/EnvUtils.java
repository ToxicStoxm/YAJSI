package com.toxicstoxm.YAJSI.utils;

import com.toxicstoxm.YAJSI.Overwriter;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.toxicstoxm.YAJSI.utils.TypeUtils.PARSERS;

public class EnvUtils {
    private static final Pattern CAMEL_CASE = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Pattern SPACES_DASHES = Pattern.compile("[\\s\\-]+");

    private static final ConcurrentHashMap<Field, String> ENV_NAME_CACHE = new ConcurrentHashMap<>();

    private static @Nullable String getReplacement(Field field) {
        String envName = ENV_NAME_CACHE.computeIfAbsent(field, f -> {
            if (f.isAnnotationPresent(YAMLSetting.class)) {
                String env = f.getAnnotation(YAMLSetting.class).env();
                if (!env.isBlank()) return env;
            }
            return toScreamingSnakeCase(f.getName());
        });
        for (Overwriter overwriter : SettingsManager.getSettings().getOverwriters()) {
            String replacement = overwriter.get(envName);
            if (replacement != null) {
                return replacement;
            }
        }
        return null;
    }

    private static @Nullable String toScreamingSnakeCase(@Nullable String name) {
        if (name == null || name.isEmpty())
            return name;

        // Insert underscores before uppercase letters (except at start),
        // then replace spaces/dashes with underscores, and uppercase the result.
        return SPACES_DASHES
                .matcher(CAMEL_CASE.matcher(name).replaceAll("$1_$2")) // camelCase → camel_Case
                .replaceAll("_") // spaces/dashes → underscore
                .toUpperCase();
    }

    public static Object checkForEnvPrimitive(@NotNull Field field, @Nullable Object fieldValue) {
        String val = getReplacement(field);
        if (val == null)
            return fieldValue;

        Function<String, ?> parser = PARSERS.get(field.getType());
        if (parser == null) return fieldValue;

        try {
            return parser.apply(val);
        } catch (Exception ignored) {
            return fieldValue;
        }
    }

    public static @NotNull List<?> checkForEnvPrimitiveList(@NotNull Field field, @NotNull List<?> value) {
        String val = getReplacement(field);
        if (val == null) return value;
        if (val.isEmpty()) return new ArrayList<>();

        Class<?> elementType = TypeUtils.getGenericTypeClass(field);

        Function<String, ?> parser = PARSERS.getOrDefault(elementType, s -> s);

        String[] parts = val.split("\\s*,\\s*");
        List<Object> parsedList = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                parsedList.add(parser.apply(part));
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse list from env variables, contains different types!", e);
            }
        }

        return parsedList;
    }

    public static @NotNull Object checkForEnvPrimitiveArray(@NotNull Field field, @NotNull Object array) {
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Field is not an array: " + field.getName());
        }

        String val = getReplacement(field);
        if (val == null || val.isEmpty()) return array;

        Class<?> componentType = array.getClass().getComponentType();
        Function<String, ?> parser = PARSERS.getOrDefault(componentType, s -> s);

        String[] parts = val.split("\\s*,\\s*");
        Object newArray = java.lang.reflect.Array.newInstance(componentType, parts.length);

        for (int i = 0; i < parts.length; i++) {
            try {
                java.lang.reflect.Array.set(newArray, i, parser.apply(parts[i]));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Unable to parse array element from env variable: " + parts[i], e
                );
            }
        }

        return newArray;
    }
}
