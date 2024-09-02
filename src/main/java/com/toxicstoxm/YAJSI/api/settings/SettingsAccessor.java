package com.toxicstoxm.YAJSI.api.settings;

public interface SettingsAccessor {
    Setting<Object> get(String path);
}
