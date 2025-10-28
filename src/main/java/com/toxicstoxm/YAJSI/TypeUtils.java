package com.toxicstoxm.YAJSI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class TypeUtils {
    /**
     * Checks if the specified object is not a primitive object.
     * @param object the object to check
     * @return {@code false} if the specified object is a primitive object, otherwise {@code true}
     */
    public static boolean isCustomObject(@NotNull Object object) {
        Class<?> clazz = object.getClass();
        return !(clazz.isPrimitive() || clazz.equals(String.class) || clazz.equals(Boolean.class) || Number.class.isAssignableFrom(clazz));
    }

    /**
     * Checks if the specified object is a list of primitive objects.
     * @param object the object to check
     * @return {@code true} if the specified object is a list of primitive objects, otherwise {@code true}
     */
    public static boolean isListOfPrimitives(@Nullable Object object) {
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

    public static boolean isArrayOfPrimitives(@Nullable Object object) {
        if (object == null) return false;
        Class<?> clazz = object.getClass();
        return clazz.isArray() && clazz.getComponentType().isPrimitive();
    }
}
