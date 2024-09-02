package com.toxicstoxm.YAJSI.api.settings;

import com.toxicstoxm.YAJSI.api.file.YamlConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class YAJSISettingsHandler<T> {

    public void loadSettings(Class<? extends SettingsBundle> settingsBundle, SettingsAccessor accessor)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!List.of(settingsBundle.getInterfaces()).contains(SettingsBundle.class)) {
            throw new IllegalArgumentException(settingsBundle.getName() + " must implement SettingsBundle interface!");
        }
        System.out.println("Loading settings: ");
        for (Class<?> innerClass : settingsBundle.getDeclaredClasses()) {
            if (innerClass.isAnnotationPresent(YAMLSetting.class)) {
                String path = innerClass.getAnnotation(YAMLSetting.class).path();
                Constructor<?> constructor = innerClass.getConstructor(Setting.class);
                Setting<Object> fetchedSetting = accessor.get(path);
                if (fetchedSetting != null) {
                    T setting = (T) constructor.newInstance(fetchedSetting);
                    if (setting instanceof YAJSISetting<?> YAJSISetting)
                        System.out.println(YAJSISetting.getIdentifier(true));
                    else System.out.print(setting);
                } else System.out.println(innerClass.getName() + " could not be loaded from config! Path '" + path + "' doesn't exist!");
            }
        }
    }

    public boolean saveSettings(Class<? extends SettingsBundle> settingsBundle, YamlConfiguration yaml) {
        boolean shouldSave = false;
        if (!List.of(settingsBundle.getInterfaces()).contains(SettingsBundle.class)) {
            throw new IllegalArgumentException(settingsBundle.getName() + " must implement SettingsBundle interface!");
        }
        for (Class<?> innerClass : settingsBundle.getDeclaredClasses()) {
            if (innerClass.isAnnotationPresent(YAMLSetting.class)) {
                String path = innerClass.getAnnotation(YAMLSetting.class).path();

                Optional<Method> getterOpt = getInstanceGetter(innerClass);
                if (getterOpt.isPresent()) {
                    Method getter = getterOpt.get();
                    try {
                        Setting<?> setting = ((Setting<?>) getter.invoke(innerClass));
                        T value = (T) setting.get();
                        boolean save = true;
                        if (setting instanceof YAJSISetting<?> YAJSISetting) {
                           if (!YAJSISetting.isShouldSave()) save = false;
                        }

                        if (save) {
                            shouldSave = true;
                            yaml.set(path, value);
                        }
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Error invoking method: " + getter.getName(), e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Illegal access to method: " + getter.getName(), e);
                    }
                } else {
                    throw new NullPointerException("Getter method not found in " + innerClass.getName());
                }
            }
        }
        return shouldSave;
    }

    private Optional<Method> getInstanceGetter(Class<?> clazz) {
        try {
            return Optional.of(clazz.getMethod("getInstance"));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}
