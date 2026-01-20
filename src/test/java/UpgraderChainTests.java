import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import com.toxicstoxm.YAJSI.upgrading.Upgrader;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that a chain of @Upgrader annotated methods is applied in sequence to bring old configs to the latest version
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class UpgraderChainTests {
    private Path tmp;

    @BeforeEach
    public void before() throws Exception {
        tmp = Files.createTempDirectory("yajsi-upgrade-");
        resetSettingsManagerSingleton();
    }

    @AfterEach
    public void after() throws Exception {
        if (tmp != null && Files.exists(tmp)) {
            try (var s = Files.walk(tmp)) {
                s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        resetSettingsManagerSingleton();
    }

    private static void resetSettingsManagerSingleton() throws Exception {
        Field f = SettingsManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    public static class TestBundle extends SettingsBundle {
        public TestBundle(File f) {
            super(new ConfigVersion(1,5,2), f, ConfigType.SETTINGS);
        }

        @YAMLSetting(name = "subSection")
        public SubTestBundle subSection = new SubTestBundle();

        public static class SubTestBundle {
            @YAMLSetting(name = "Test")
            public int test;
        }

        @Upgrader(base = "0.9.9", factory = ConfigVersion.Factory.class)
        private YamlConfiguration upgrade0_9_9(YamlConfiguration old, UUID id) {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 0, 0).toString());
            updated.set("testing", old.get("test"));
            return updated;
        }

        @Upgrader(base = "1.0.0", factory = ConfigVersion.Factory.class)
        private YamlConfiguration upgrade1_0_0(YamlConfiguration old, UUID id) {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 2, 0).toString());
            updated.set("Tested", old.get("testing"));
            return updated;
        }

        @Upgrader(base = "1.2.0", factory = ConfigVersion.Factory.class)
        private YamlConfiguration upgrade1_2_0(YamlConfiguration old, UUID id) {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 3, 0).toString());
            updated.set("subSection.testing", old.get("Tested"));
            return updated;
        }

        @Upgrader(base = "1.3.0", factory = ConfigVersion.Factory.class)
        private YamlConfiguration upgrade1_3_0(YamlConfiguration old, UUID id) {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 5, 2).toString());
            updated.set("subSection.Test", old.get("subSection.testing"));
            return updated;
        }
    }

    @Test
    public void upgraderChain_appliesAllSteps_and_finalFieldIsCorrect() throws Exception {
        File cfg = tmp.resolve("upgrader-chain.yaml").toFile();

        // create an old YAML with version 0.9.9 and original top-level "test" key
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(SettingsManager.getSettings() == null ? "Version" : SettingsManager.getSettings().getVersionKey(), "0.9.9");
        yaml.set("test", 123);
        yaml.save(cfg);

        // configure to auto-upgrade
        SettingsManager.configure().autoUpgrade(true).done();

        // register bundle (should run upgrader chain)
        TestBundle b = new TestBundle(cfg);
        UUID id = SettingsManager.getInstance().registerConfig(b);
        assertNotNull(id);

        // the final nested value should be moved to subSection.Test by upgrader chain
        assertEquals(123, b.subSection.test,
                "Upgrader chain should move the original 'test' value into subSection.Test");
    }
}
