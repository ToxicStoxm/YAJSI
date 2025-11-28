package com.toxicstoxm.YAJSI.serializing;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface YAMLSerializable {
    @Contract(value = "-> new")
    default @NotNull ConfigurationSection emptyConfigSection() {
        return new YamlConfiguration();
    }

    default @NotNull ConfigurationSection serialize(Object o) {
        return emptyConfigSection();
    }

    default @NotNull ConfigurationSection serializeSelf() {
        return serialize(this);
    }

    @Contract(value = "_ -> new")
    Object deserialize(@NotNull ConfigurationSection yaml);
}
