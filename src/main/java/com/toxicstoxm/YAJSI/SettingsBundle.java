package com.toxicstoxm.YAJSI;

import com.toxicstoxm.YAJSI.upgrading.Version;
import lombok.Builder;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder
public class SettingsBundle {
    private final UUID id = UUID.randomUUID();
    private final Version version;
    private final File file;
    private final ConfigType type;
    private final List<String> envSubstituted = new ArrayList<>();

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

    public boolean isEnvSubstituted(String variable) {
        return envSubstituted.contains(variable);
    }

    protected void setEnvSubstituted(String variable) {
        envSubstituted.add(variable);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SettingsBundle that = (SettingsBundle) o;
        return Objects.equals(id, that.id) && Objects.equals(version, that.version) && Objects.equals(file, that.file) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, file, type);
    }
}
