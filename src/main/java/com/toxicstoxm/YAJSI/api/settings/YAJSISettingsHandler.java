package com.toxicstoxm.YAJSI.api.settings;

import com.toxicstoxm.YAJSI.api.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.api.logging.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class YAJSISettingsHandler<T> {
    
    private final Logger logger;
    
    public YAJSISettingsHandler(Logger logger) {
        this.logger = logger;
    }

    public boolean loadSettings(Class<? extends SettingsBundle> settingsBundle, SettingsAccessor accessor)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!List.of(settingsBundle.getInterfaces()).contains(SettingsBundle.class)) {
            throw new IllegalArgumentException(settingsBundle.getName() + " must implement SettingsBundle interface!");
        }
        String prefix = "[" + Arrays.stream(settingsBundle.getName().split("\\.")).toList().getLast() + "]: ";
        logger.log(prefix + "Loading settings...");
        boolean error = false;
        for (Class<?> innerClass : settingsBundle.getDeclaredClasses()) {
            if (innerClass.isAnnotationPresent(YAMLSetting.class)) {
                String path = innerClass.getAnnotation(YAMLSetting.class).path();
                Constructor<?> constructor = innerClass.getConstructor(Setting.class);
                Setting<Object> fetchedSetting = accessor.get(path);
                if (fetchedSetting != null) {
                    T setting = (T) constructor.newInstance(fetchedSetting);
                    if (setting instanceof YAJSISetting<?> YAJSISetting)
                        logger.log(prefix + YAJSISetting.getIdentifier(true));
                    else System.out.print(prefix + setting);
                } else  {
                    logger.log(prefix + innerClass.getName() + " could not be loaded from config! Path '" + path + "' doesn't exist!");
                    error = true;
                }
            }
        }
        if (!error) logger.log(prefix + "Successfully loaded settings!");
        return error;
    }

    public boolean saveSettings(Class<? extends SettingsBundle> settingsBundle, YamlConfiguration yaml) {
        boolean shouldSave = false;
        boolean error = false;
        if (!List.of(settingsBundle.getInterfaces()).contains(SettingsBundle.class)) {
            throw new IllegalArgumentException(settingsBundle.getName() + " must implement SettingsBundle interface!");
        }
        String prefix = "[" + Arrays.stream(settingsBundle.getName().split("\\.")).toList().getLast() + "]: ";
        logger.log(prefix + "Saving settings...");
        for (Class<?> innerClass : settingsBundle.getDeclaredClasses()) {
            if (innerClass.isAnnotationPresent(YAMLSetting.class)) {
                String path = innerClass.getAnnotation(YAMLSetting.class).path();

                Optional<Method> getterOpt = getInstanceGetter(innerClass);
                if (getterOpt.isPresent()) {
                    Method getter = getterOpt.get();
                    try {
                        Setting<?> setting = ((Setting<?>) getter.invoke(innerClass));
                        if (setting != null) {
                            T value = (T) setting.get();
                            boolean save = true;
                            if (setting instanceof YAJSISetting<?> YAJSISetting) {
                                if (!YAJSISetting.isShouldSave()) save = false;
                            }

                            if (save) {
                                logger.log(prefix + setting.getIdentifier(true));
                                shouldSave = true;
                                yaml.set(path, value);
                            } else logger.log(prefix + setting.getIdentifier(true) + " [Didn't save because the value didn't change!]");
                        }
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        logger.log(prefix + "Wasn't able to call the instance getter for '" + Arrays.stream(innerClass.getName().split("\\.")).toList().getLast() + "' Or its value was null!");
                        error = true;
                    }
                } else {
                    logger.log(prefix + "Wasn't able to call the instance getter for '" + Arrays.stream(innerClass.getName().split("\\.")).toList().getLast() + "'!");
                    error = true;
                }
            }
        }
        if (!error) {
            if (shouldSave) logger.log(prefix + "Successfully saved settings!");
            else logger.log(prefix + "Didn't save because no values changed!");
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
