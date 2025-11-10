import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class EnvTests {

    public static class Settings extends SettingsBundle {

        public Settings() {
            super(new ConfigVersion(1, 0, 0), new File("src/test/resources/EnvTests/settings.yaml"), ConfigType.READONLY);
        }

        @YAMLSetting(name = "Testing")
        public List<Integer> testing = List.of(5, 6, 4, 6, 45);
    }

    @Test
    public void envTest() {
        System.setProperty("TESTING", "655,346,67,435,457");

       SettingsManager.configure()
               .enableOverwriters(false)
               .overwriters(List.of(System::getenv, System::getProperty))
               .done();

        Settings settings = new Settings();
        SettingsManager.getInstance().registerConfig(settings);

        settings.testing.add(465);

        System.out.println(settings.testing);
    }
}
