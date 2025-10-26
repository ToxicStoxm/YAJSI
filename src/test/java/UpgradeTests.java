import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import com.toxicstoxm.YAJSI.upgrading.UpgraderBundle;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class UpgradeTests {

    @Test
    public void upgradeTest() {
        SettingsManager.configure()
                .autoUpgrade(true)
                .envOverwrites(false)
                .done();

        UserSettings settings = new UserSettings(new File("src/test/resources/user_settings.yaml"));
        settings.register();

        System.out.println(settings.isEnvSubstituted("test"));

        System.out.println(settings.gui.rows + " * " + settings.gui.cols);
        System.out.println(settings.test);

        for (int i : settings.test) {
            System.out.println(i);
        }

        SettingsManager.getInstance().save();
    }

    @UpgraderBundle(upgraderBundle = UserSettingsUpgraders.class)
    public static class UserSettings extends SettingsBundle {

        public UserSettings(File f) {
            super(new ConfigVersion(1, 1, 0), f, ConfigType.READONLY);
        }

        @YAMLSetting(name = "GUI")
        public GUI gui = new GUI();

        public static class GUI {

            @YAMLSetting(name = "rows")
            public int rows = 5;

            @YAMLSetting(name = "columns")
            public int cols;
        }

        @YAMLSetting(name = "SOmeInT")
        public int testing;

        @YAMLSetting(name = "INTS", env = "INTS")
        public List<Integer> test = List.of(1, 56, 34, 56);
    }

    public static class UserSettingsUpgraders {

    }
}
