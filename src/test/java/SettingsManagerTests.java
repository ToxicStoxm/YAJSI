import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

public class SettingsManagerTests {

    public static class TestBundle extends SettingsBundle {

        public int test = 5;

        public TestBundle() {
            super(new ConfigVersion(1,5,2), new File("src/test/resources/test.yaml"), ConfigType.SETTINGS);
        }
    }

    @Test
    public void testSettingsManager() {
        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            System.out.println("0.9.9 to 1.0.0");
            old.set("Version", "1.0.0");
            return old;
        }, new ConfigVersion(0,9,9));
        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            System.out.println("1.0.0 to 1.2.0");
            old.set("Version", "1.2.0");
            return old;
        }, new ConfigVersion(1,0,0));
        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            System.out.println("1.2.0 to 1.3.0");
            old.set("Version", "1.3.0");
            return old;
        }, new ConfigVersion(1,2,0));
        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            System.out.println("1.3.0 to 1.5.2");
            old.set("Version", "1.5.2");
            return old;
        }, new ConfigVersion(1,3,0));

        UUID id = SettingsManager.getInstance().registerConfig(new TestBundle());

    }
}
