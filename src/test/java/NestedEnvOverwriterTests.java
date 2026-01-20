import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests nested object loading and that environment overwriters apply only in-memory and are not persisted.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class NestedEnvOverwriterTests {
    private Path tmp;

    @BeforeEach
    public void before() throws Exception {
        tmp = Files.createTempDirectory("yajsi-nested-");
        resetSettingsManagerSingleton();
    }

    @AfterEach
    public void after() throws Exception {
        if (tmp != null && Files.exists(tmp)) {
            try (var s = Files.walk(tmp)) {
                s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        System.clearProperty("HELLO");
        resetSettingsManagerSingleton();
    }

    private static void resetSettingsManagerSingleton() throws Exception {
        Field f = SettingsManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    public static class NestedBundle extends SettingsBundle {
        public NestedBundle(File f) {
            super(new com.toxicstoxm.YAJSI.upgrading.ConfigVersion(1,0,0), f);
        }

        @YAMLSetting(name = "outer")
        public Outer outer = new Outer();

        public static class Outer {
            @YAMLSetting(name = "inner", env = "HELLO")
            public int inner = 5;
        }
    }

    @Test
    public void envOverwriter_changesInMemoryButNotOnDisk() throws Exception {
        File cfg = tmp.resolve("nested.yaml").toFile();

        // create file with Version and outer.inner = 5
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(SettingsManager.getSettings() == null ? "Version" : SettingsManager.getSettings().getVersionKey(), "1.0.0");
        yaml.set("outer", new YamlConfiguration()); // ensure outer exists as section
        ((YamlConfiguration) yaml.get("outer")).set("inner", 5);
        yaml.save(cfg);

        // configure overwriters: first null, second system property
        SettingsManager.configure()
                .enableOverwriters(true)
                .overwriters(List.of(
                        (String _) -> null,
                        System::getProperty
                ))
                .done();

        System.setProperty("HELLO", "99");

        NestedBundle b = new NestedBundle(cfg);
        SettingsManager.getInstance().registerConfig(b);

        // in-memory must be overwritten
        assertEquals(99, b.outer.inner);

        // disk must remain original 5
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.load(cfg);
        ConfigurationSection outerSec = (ConfigurationSection) onDisk.get("outer");
        assertEquals(5, outerSec.getInt("inner"));

        // saving should NOT write the env-overwritten value
        SettingsManager.getInstance().save();
        YamlConfiguration afterSave = new YamlConfiguration();
        afterSave.load(cfg);
        ConfigurationSection afterOuter = (ConfigurationSection) afterSave.get("outer");
        assertEquals(5, afterOuter.getInt("inner"));
    }
}
