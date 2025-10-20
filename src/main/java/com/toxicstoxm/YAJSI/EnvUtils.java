package com.toxicstoxm.YAJSI;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EnvUtils {
    private static String getEnv(Field field) {
        return System.getenv(getEnvName(field));
    }

    private static String getEnvName(Field field) {
        String declaredEnv = "";
        if (field.isAnnotationPresent(YAMLSetting.class)) {
            declaredEnv = field.getAnnotation(YAMLSetting.class).env();
        }
        return declaredEnv.isBlank() ? toScreamingSnakeCase(field.getName()) : declaredEnv;
    }

    private static String toScreamingSnakeCase(String name) {
        if (name == null || name.isEmpty())
            return name;

        // Insert underscores before uppercase letters (except at start),
        // then replace spaces/dashes with underscores, and uppercase the result.
        return name
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")  // camelCase → camel_Case
                .replaceAll("[\\s\\-]+", "_")              // spaces/dashes → underscore
                .toUpperCase();
    }

    public static Object checkForEnvPrimitive(Field field, Object fieldValue) {
        String val = getEnv(field);
        if (val == null)
            return fieldValue;

        Class<?> type = field.getType();
        Object converted;

        try {
            if (type == String.class) {
                converted = val;
            } else if (type == int.class || type == Integer.class) {
                converted = Integer.parseInt(val);
            } else if (type == long.class || type == Long.class) {
                converted = Long.parseLong(val);
            } else if (type == double.class || type == Double.class) {
                converted = Double.parseDouble(val);
            } else if (type == float.class || type == Float.class) {
                converted = Float.parseFloat(val);
            } else if (type == boolean.class || type == Boolean.class) {
                converted = Boolean.parseBoolean(val);
            } else if (type == byte.class || type == Byte.class) {
                converted = Byte.parseByte(val);
            } else if (type == short.class || type == Short.class) {
                converted = Short.parseShort(val);
            } else if (type == char.class || type == Character.class) {
                converted = val.isEmpty() ? '\0' : val.charAt(0);
            } else {
                // Unsupported type — return unmodified config
                return fieldValue;
            }

        } catch (Exception e) {
            // Optional: log or warn about invalid env conversion
            // e.g. YAJLManager.getInstance().logger.warn("Invalid env value for " + field.getName());
            return fieldValue;
        }

        return converted;
    }

    public static Object checkForEnvPrimitiveList(Field field, Object fieldValue) {
        String val = getEnv(field);
        if (val == null || val.isEmpty())
            return fieldValue;

        // Detect list element type (default to String if unknown)
        Class<?> elementType = Object.class;
        if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?> clazz)
                elementType = clazz;
        }

        // Split env value by commas (e.g. "1,2,3" or "true,false,true")
        String[] parts = val.split("\\s*,\\s*");
        List<Object> parsedList = new ArrayList<>(parts.length);

        for (String part : parts) {
            Object converted = switch (elementType.getSimpleName()) {
                case "String" -> part;
                case "Integer", "int" -> Integer.parseInt(part);
                case "Long", "long" -> Long.parseLong(part);
                case "Double", "double" -> Double.parseDouble(part);
                case "Float", "float" -> Float.parseFloat(part);
                case "Boolean", "boolean" -> Boolean.parseBoolean(part);
                case "Byte", "byte" -> Byte.parseByte(part);
                case "Short", "short" -> Short.parseShort(part);
                case "Character", "char" -> part.isEmpty() ? '\0' : part.charAt(0);
                default -> part; // fallback: keep as string
            };
            parsedList.add(converted);
        }

        try {
            field.setAccessible(true);
            field.set(fieldValue, parsedList);
        } catch (IllegalAccessException e) {
            // Optional: log or ignore
            // e.g. YAJLManager.getInstance().logger.warn("Failed to set env list for " + field.getName());
        }

        return fieldValue;
    }
}
