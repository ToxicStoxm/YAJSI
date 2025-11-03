package com.toxicstoxm.YAJSI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

public class EnvUtils {
    private static final Pattern CAMEL_CASE = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Pattern SPACES_DASHES = Pattern.compile("[\\s\\-]+");

    private static final ConcurrentHashMap<Field, String> ENV_NAME_CACHE = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Function<String, ?>> PARSERS = new HashMap<>();
    static {
        PARSERS.put(String.class, s -> s);
        PARSERS.put(int.class, Integer::parseInt);
        PARSERS.put(Integer.class, Integer::parseInt);
        PARSERS.put(long.class, Long::parseLong);
        PARSERS.put(Long.class, Long::parseLong);
        PARSERS.put(double.class, Double::parseDouble);
        PARSERS.put(Double.class, Double::parseDouble);
        PARSERS.put(float.class, Float::parseFloat);
        PARSERS.put(Float.class, Float::parseFloat);
        PARSERS.put(boolean.class, Boolean::parseBoolean);
        PARSERS.put(Boolean.class, Boolean::parseBoolean);
        PARSERS.put(byte.class, Byte::parseByte);
        PARSERS.put(Byte.class, Byte::parseByte);
        PARSERS.put(short.class, Short::parseShort);
        PARSERS.put(Short.class, Short::parseShort);
        PARSERS.put(char.class, s -> s.isEmpty() ? '\0' : s.charAt(0));
        PARSERS.put(Character.class, s -> s.isEmpty() ? '\0' : s.charAt(0));
    }

    private static String getEnv(Field field) {
        String envName = ENV_NAME_CACHE.computeIfAbsent(field, f -> {
            if (f.isAnnotationPresent(YAMLSetting.class)) {
                String env = f.getAnnotation(YAMLSetting.class).env();
                if (!env.isBlank()) return env;
            }
            return toScreamingSnakeCase(f.getName());
        });
        return System.getenv(envName);
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
        String val = getEnv(field);
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
        String val = getEnv(field);
        if (val == null) return value;
        if (val.isEmpty()) return Collections.emptyList();

        Class<?> elementType = String.class;
        if (field.getGenericType() instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 1 && args[0] instanceof Class<?> c)
                elementType = c;
        }

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

        String val = getEnv(field);
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
