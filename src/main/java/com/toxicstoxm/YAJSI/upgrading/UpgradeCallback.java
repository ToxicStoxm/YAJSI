package com.toxicstoxm.YAJSI.upgrading;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;

import java.util.UUID;

@FunctionalInterface
public interface UpgradeCallback {
    YamlConfiguration process(YamlConfiguration old, UUID id);
}
