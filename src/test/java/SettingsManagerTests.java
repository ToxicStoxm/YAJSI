import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

public class SettingsManagerTests {

    public static class TestBundle extends SettingsBundle {
        public TestBundle(File f) {
            super(new ConfigVersion(1,5,2), f, ConfigType.SETTINGS);
        }

        @YAMLSetting(name = "subSection")
        public SubTestBundle subSection;

        public static class SubTestBundle {
            @YAMLSetting(name = "Test")
            public int test;

        }
    }

    @Test
    public void testSettingsManager() {
        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set("Version", "1.0.0");
            updated.set("testing", old.get("test"));

            System.out.println("0.9.9 to 1.0.0");
            System.out.println("------< OLD >------");
            System.out.println(old.saveToString());
            System.out.println("------< NEW >------");
            System.out.println(updated.saveToString());
            System.out.println("-------------------");

            return updated;
        }, new ConfigVersion(0,9,9));

        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set("Version", "1.2.0");
            updated.set("Tested", old.get("testing"));

            System.out.println("1.0.0 to 1.2.0");
            System.out.println("------< OLD >------");
            System.out.println(old.saveToString());
            System.out.println("------< NEW >------");
            System.out.println(updated.saveToString());
            System.out.println("-------------------");

            return updated;
        }, new ConfigVersion(1,0,0));

        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set("Version", "1.3.0");
            updated.set("subSection.testing", old.get("Tested"));

            System.out.println("1.2.0 to 1.3.0");
            System.out.println("------< OLD >------");
            System.out.println(old.saveToString());
            System.out.println("------< NEW >------");
            System.out.println(updated.saveToString());
            System.out.println("-------------------");

            return updated;
        }, new ConfigVersion(1,2,0));

        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set("Version", "1.5.2");
            updated.set("subSection.Test", old.get("subSection.testing"));

            System.out.println("1.3.0 to 1.5.2");
            System.out.println("------< OLD >------");
            System.out.println(old.saveToString());
            System.out.println("------< NEW >------");
            System.out.println(updated.saveToString());
            System.out.println("-------------------");

            return updated;
        }, new ConfigVersion(1,3,0));

        UUID id = SettingsManager.getInstance().registerConfig(new TestBundle(new File("src/test/resources/test.yaml")));
        UUID id2 = SettingsManager.getInstance().registerConfig(new TestBundle(new File("src/test/resources/test2.yaml")));
        System.out.println(id);

        TestBundle instance = SettingsManager.getInstance().getSettingsBundleInstance(TestBundle.class, id);
        Assertions.assertNotNull(instance);
        System.out.println(instance.getId());
        System.out.println(instance.subSection.test);

        TestBundle instance2 = SettingsManager.getInstance().getSettingsBundleInstance(TestBundle.class, id2);
        Assertions.assertNotNull(instance2);
        System.out.println(instance2.getId());
        System.out.println(instance2.subSection.test);
    }
}
