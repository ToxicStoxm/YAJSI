package com.toxicstoxm.YAJSI;

import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ExternalYAMLSerializer<T> {
    @Contract(value = "-> new")
    default @NotNull ConfigurationSection emptyConfigSection() {
        return new YamlConfiguration();
    }

    T deserialize(@NotNull ConfigurationSection yaml);

    @NotNull ConfigurationSection serialize(T o);
}
