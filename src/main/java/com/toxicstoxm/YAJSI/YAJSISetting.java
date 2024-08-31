package com.toxicstoxm.YAJSI;

import lombok.Getter;
import lombok.Setter;

public class YAJSISetting<T> implements Setting<T> {
    private T value;

    @Setter
    @Getter
    private boolean shouldSave = false;

    public YAJSISetting(T value) {
        this.value = value;
    }

    public YAJSISetting(Setting<Object> setting, Class<T> clazz) {
        if (!setting.isType(setting.get())) {
            throw new IllegalArgumentException("Type mismatch");
        }
        this.value = clazz.cast(setting.get());
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(T value) {
        this.value = value;
    }

    @Override
    public void set(T value, boolean shouldSave) {
        this.value = value;
        this.shouldSave = shouldSave;
    }

    @Override
    public Class<?> getType() {
        return value.getClass();
    }

    @Override
    public String toString() {
        return YAJSISetting.class.getName() + ": [ Name: " + getClass().getName() + " Type: " + getType().getName() + " Value: " + value.toString() + " ]";
    }

    @Override
    public String getIdentifier(boolean withVarType) {
        String[] nameParts = getClass().getName().split("\\$");
        String[] typeParts = getType().getName().split("\\.");
        return nameParts[nameParts.length - 1] +
                (withVarType ? " [" + typeParts[typeParts.length - 1] + "]" : "") + " -> " + value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return isType(obj) && getClass().isInstance(obj);
    }
}
