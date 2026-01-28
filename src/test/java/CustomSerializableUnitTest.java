import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.serializing.ExternalYAMLSerializer;
import com.toxicstoxm.YAJSI.serializing.SerializableWith;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that an ExternalYAMLSerializer declared via @SerializableWith is used when loading/saving.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class CustomSerializableUnitTest {
    private Path tmp;

    @BeforeEach
    public void before() throws Exception {
        tmp = Files.createTempDirectory("yajsi-serializer-");
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

    public static class LoggerSectionSerializer implements ExternalYAMLSerializer<TestBundle.LoggerSection> {
        @Override
        public TestBundle.LoggerSection deserialize(@NotNull ConfigurationSection yaml) {
            TestBundle.LoggerSection l = new TestBundle.LoggerSection();
            l.helloWorld = yaml.getInt("Hallo2") - 10;
            return l;
        }

        @Override
        public @NotNull ConfigurationSection serialize(TestBundle.LoggerSection o) {
            ConfigurationSection section = new YamlConfiguration();
            section.set("Hallo2", o.helloWorld + 10);
            return section;
        }
    }

    public static class TestBundle extends SettingsBundle {
        public TestBundle(File f) {
            super(new ConfigVersion(1,0,0), f);
        }

        // annotated to use external serializer
        @SerializableWith(serializer = LoggerSectionSerializer.class)
        @NoArgsConstructor
        public static class LoggerSection {
            public int helloWorld;
        }

        public LoggerSection section = new LoggerSection();
    }

    @Test
    public void deserializeUsesExternalSerializer_whenYamlHasSerializedSection() throws Exception {
        File cfg = tmp.resolve("custom-serializer.yaml").toFile();

        // create YAML that uses serializer format: Hallo2 = 30 -> helloWorld should be 20 after deserialize
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(SettingsManager.getSettings() == null ? "Version" : SettingsManager.getSettings().getVersionKey(), "1.0.0");
        ConfigurationSection section = new YamlConfiguration();
        section.set("Hallo2", 30);
        yaml.set("section", section);
        yaml.save(cfg);

        SettingsManager.configure().autoUpgrade(true).done();

        TestBundle b = new TestBundle(cfg);
        assertDoesNotThrow(() -> SettingsManager.getInstance().registerConfig(b));
        assertNotNull(b.section);
        assertEquals(20, b.section.helloWorld, "Deserializer must transform Hallo2->helloWorld (30-10)");
    }

    @Test
    public void serializeUsesExternalSerializer_whenSavingBundle() throws Exception {
        File cfg = tmp.resolve("custom-serializer-save.yaml").toFile();

        SettingsManager.configure().autoUpgrade(true).done();

        TestBundle b = new TestBundle(cfg);
        // set in-memory value and register -> should create file on registration
        b.section.helloWorld = 7; // serializer should write Hallo2 = 17
        SettingsManager.getInstance().registerConfig(b);

        // force save
        SettingsManager.getInstance().save();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(cfg);

        Object raw = yaml.get("section");
        assertNotNull(raw, "Serialized section must exist on disk");

        // get section and ensure Hallo2 = helloWorld + 10
        ConfigurationSection sec = (ConfigurationSection) yaml.get("section");
        assertEquals(17, sec.getInt("Hallo2"));
    }
}
