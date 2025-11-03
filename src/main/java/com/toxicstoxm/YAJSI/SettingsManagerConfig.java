package com.toxicstoxm.YAJSI;

import com.toxicstoxm.YAJSI.upgrading.AutoUpgradingBehaviour;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Builder(buildMethodName = "done", toBuilder = true)
public class SettingsManagerConfig {
    @Contract(value = " -> new", pure = true)
    public static @NotNull SettingsManagerConfig getDefaults() {
        return SettingsManagerConfig.builder().done();
    }

    @Builder.Default
    private boolean envOverwrites = false;

    @Builder.Default
    private boolean saveReadOnlyConfigOnVersionUpgrade = true;

    @Builder.Default
    private String versionKey = "Version";

    @Builder.Default
    private boolean autoUpgrade = false;

    @Builder.Default
    private AutoUpgradingBehaviour autoUpgradeBehaviour = AutoUpgradingBehaviour.MARK_UNUSED;

    @Builder.Default
    private String unusedWarning = "Deprecated. No longer used";
}
