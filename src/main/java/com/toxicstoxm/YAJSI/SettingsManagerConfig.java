package com.toxicstoxm.YAJSI;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Builder(builderMethodName = "configure", buildMethodName = "done")
public class SettingsManagerConfig {
    @Contract(value = " -> new", pure = true)
    public static @NotNull SettingsManagerConfig getDefaults() {
        return SettingsManagerConfig.configure().done();
    }

    @Builder.Default
    private String appName = "YAJSI";

    @Builder.Default
    private DuplicateSettingsStrategy duplicatedSettingsStrategy = DuplicateSettingsStrategy.ERROR;

}
