package com.toxicstoxm.YAJSI.api.settings;

import com.toxicstoxm.YAJSI.api.logging.Logger;

public interface SettingsManagerSettings {

    SettingsManagerSettings setConfigDirectory(String configDirectory);

    SettingsManagerSettings setAppName(String appName);

    SettingsManagerSettings setUpdatingBehaviour(YAMLUpdatingBehaviour updatingBehaviour);

    SettingsManagerSettings setLogger(Logger logger);

    SettingsManagerSettings setEnableLogBuffer(boolean enableLogBuffer);

    SettingsManagerSettings setLogMessageBufferSize(int logMessageBufferSize);

    SettingsManagerSettings setConfigClassesHaveNoArgsConstructor(boolean configClassesHaveNoArgsConstructor);

    SettingsManagerSettings setUnusedSettingWarning(String unusedSettingWarning);
}
