package com.toxicstoxm.YAJSI.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TypeUtils {
    /**
     * Class to Parser map for primitives and their wrappers.
     */
    public static final HashMap<Class<?>, Function<String, ?>> PARSERS = new HashMap<>();
    /**
     * Class to Supplier map for common objects, like lists, maps, and other common collection implementations.
     */
    public static Map<Class<?>, Supplier<?>> DEFAULT_SUPPLIERS = new HashMap<>();

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

        DEFAULT_SUPPLIERS.put(List.class, ArrayList::new);
        DEFAULT_SUPPLIERS.put(Map.class, HashMap::new);
        DEFAULT_SUPPLIERS.put(Collection.class, ArrayList::new);

        DEFAULT_SUPPLIERS.put(ArrayList.class, ArrayList::new);
        DEFAULT_SUPPLIERS.put(HashSet.class, HashSet::new);
        DEFAULT_SUPPLIERS.put(LinkedHashSet.class, LinkedHashSet::new);
        DEFAULT_SUPPLIERS.put(HashMap.class, HashMap::new);
        DEFAULT_SUPPLIERS.put(LinkedHashMap.class, LinkedHashMap::new);
        DEFAULT_SUPPLIERS.put(LinkedList.class, LinkedList::new);

        DEFAULT_SUPPLIERS.put(Integer.class, () -> 0);
        DEFAULT_SUPPLIERS.put(String.class, () -> "");
        DEFAULT_SUPPLIERS.put(Long.class, () -> 0L);
        DEFAULT_SUPPLIERS.put(Float.class, () -> 0.0F);
        DEFAULT_SUPPLIERS.put(Double.class, () -> 0.0D);
        DEFAULT_SUPPLIERS.put(Boolean.class, () -> false);
    }

    private static final HashMap<Field, Class<?>> GENERIC_TYPE_CACHE = new HashMap<>();

    public static @Nullable Class<?> getGenericTypeClass(@NotNull Field field) {
        if (!GENERIC_TYPE_CACHE.containsKey(field)) {
            if (field.getGenericType() instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length == 1 && args[0] instanceof Class<?> c)
                    GENERIC_TYPE_CACHE.put(field, c);
            }
        }
        return GENERIC_TYPE_CACHE.get(field);
    }

    /**
     * Checks if the specified object is not a primitive object.
     * @param object the object to check
     * @return {@code false} if the specified object is a primitive object, otherwise {@code true}
     */
    public static boolean isCustomObject(@NotNull Object object) {
        Class<?> clazz = object.getClass();
        return !clazz.isPrimitive() && !PARSERS.containsKey(clazz);
    }

    /**
     * Checks if the specified object is a list of primitive objects.
     * @param object the object to check
     * @return {@code true} if the specified object is a list of primitive objects, otherwise {@code true}
     */
    public static boolean isListOfPrimitives(@NotNull Field field, @Nullable Object object) {
        return object instanceof List<?> && PARSERS.containsKey(getGenericTypeClass(field));
    }

    public static boolean isListOfType(@NotNull Field field, @Nullable Object value) {
        if (value instanceof List<?> list) {
            if (list.isEmpty()) return true;
            Class<?> elementType = getGenericTypeClass(field);

            return elementType != null && list.stream().allMatch((Predicate<Object>) elementType::isInstance);
        }
        return false;
    }

    public static boolean isArrayOfPrimitives(@Nullable Object object) {
        if (object == null) return false;
        Class<?> clazz = object.getClass();
        return clazz.isArray() && clazz.getComponentType().isPrimitive();
    }
}
