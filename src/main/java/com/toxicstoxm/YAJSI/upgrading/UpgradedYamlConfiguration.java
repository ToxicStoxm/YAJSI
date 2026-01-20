package com.toxicstoxm.YAJSI.upgrading;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;

public record UpgradedYamlConfiguration(YamlConfiguration yaml, boolean upToDate, boolean cbUpgraded) {}
