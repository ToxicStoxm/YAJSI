package com.toxicstoxm.YAJSI;

import com.toxicstoxm.YAJSI.upgrading.Version;
import lombok.Builder;
import lombok.Getter;

import java.io.File;
import java.util.UUID;

@Getter
@Builder
public class SettingsBundle {
    private final UUID id = UUID.randomUUID();
    private final Version version;
    private final File file;
    private final ConfigType type;

    public SettingsBundle(Version version, File f, ConfigType type) {
        this.version = version;
        this.file = f;
        this.type = type;
    }

    public SettingsBundle(Version version, File f) {
        this.version = version;
        this.file = f;
        this.type = ConfigType.SETTINGS;
    }

    public void register() {
        SettingsManager.getInstance().registerConfig(this);
    }

    public boolean isReadonly() {
        return this.type == ConfigType.READONLY;
    }
}
