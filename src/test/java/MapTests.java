import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.InvalidConfigurationException;
import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MapTests {

    private class Settings extends SettingsBundle {
        public Settings() {
            super(new ConfigVersion(1, 0, 1), new File("src/test/resources/MapTests/settings.yaml"), ConfigType.SETTINGS);
        }

        public double hello12;
        public List<LoggerConfig> loggerConfigs = new ArrayList<>(List.of(new LoggerConfig(0,List.of("TEsting")), new LoggerConfig(2,List.of("TEsting"))));

        @NoArgsConstructor
        @AllArgsConstructor
        private static class LoggerConfig {
            public int testing;
            //public float hello;
            private List<String> areas;
        }

    }

    @Test
    public void mapTest() throws IOException, InvalidConfigurationException {
        SettingsManager.configure()
                .autoUpgrade(true)
                .done();
        UUID id = SettingsManager.getInstance().registerConfig(new Settings());

        Settings settings = SettingsManager.getInstance().getSettingsBundleInstance(Settings.class, id);

        assert settings != null;
        //settings.loggerConfigs.add(new Settings.LoggerConfig(5, 5.7f, List.of("Hello", "Test", "hello")));
        settings.loggerConfigs.add(new Settings.LoggerConfig(5, List.of("Hello", "Test", "hello")));

        SettingsManager.getInstance().save(settings);

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/test/resources/MapTests/settings.yaml"));
        System.out.println(yaml.saveToString());
        System.out.println("Is Section?: " + yaml.isConfigurationSection("loggerConfigs"));
        System.out.println("Is List?: " + yaml.isList("loggerConfigs"));
        System.out.println("?: " + yaml.getMapList("loggerConfigs"));
    }
}
