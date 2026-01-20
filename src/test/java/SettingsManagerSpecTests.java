import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.Version;
import com.toxicstoxm.YAJSI.upgrading.VersionFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class SettingsManagerSpecTests {

    private Path workDir;

    @BeforeEach
    public void setup() throws Exception {
        workDir = Files.createTempDirectory("yajsi-spec-");
        resetSettingsManagerSingleton();
    }

    @AfterEach
    public void teardown() throws Exception {
        // cleanup temp dir
        if (workDir != null && Files.exists(workDir)) {
            try (var s = Files.walk(workDir)) {
                s.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
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

    /* ---------- small test Version/Bundle types ---------- */

    public record TestVersion(int major, int minor) implements Version {
        @Override
        public Version getVersion() { return this; }

        @Override
        public int compareTo(Version o) {
            if (o instanceof TestVersion tv) {
                if (major != tv.major) return Integer.compare(major, tv.major);
                return Integer.compare(minor, tv.minor);
            }
            throw new IllegalArgumentException("Incompatible version type");
        }

        @Override
        public @NotNull String toString() { return major + "." + minor; }

        @Override
        public Version fromString(String s) { return new Factory().fromString(s); }

        public static class Factory implements VersionFactory<TestVersion> {
            @Override
            public TestVersion fromString(String versionString) {
                if (versionString == null || versionString.isBlank()) throw new IllegalArgumentException("blank");
                String[] parts = versionString.trim().split("\\.");
                return new TestVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
        }
    }

    public static class TestBundle extends SettingsBundle {
        @YAMLSetting(name = "Testing", env = "HELLO")
        public int test = 10;

        public TestBundle(Version v, File f, ConfigType t) {
            super(v, f, t);
        }
    }

    /* ---------- Tests ---------- */

    @Test
    public void registeringReadonlyConfig_createsFileWithDefaults_whenMissing() throws Exception {
        File cfg = workDir.resolve("readonly-create.yaml").toFile();
        assertFalse(cfg.exists());

        TestBundle bundle = new TestBundle(new TestVersion(1, 0), cfg, ConfigType.READONLY);

        // default SettingsManagerConfig has autoUpgrade = false, saveReadOnlyConfigOnVersionUpgrade = true
        SettingsManager.configure().done();

        assertDoesNotThrow(() -> SettingsManager.getInstance().registerConfig(bundle),
                "Registering a READONLY config should succeed even if no file exists");

        assertTrue(cfg.exists(), "File should be created on registration (initial write with defaults)");

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(cfg);

        String versionKey = SettingsManager.getSettings().getVersionKey();
        assertEquals(bundle.getVersion().toString(), yaml.getString(versionKey),
                "Created file should contain the bundle version at the root version key");
        assertEquals(10, Integer.parseInt(String.valueOf(yaml.get("Testing"))),
                "Created file should contain default field values");
    }

    @Test
    public void registeringWithExistingOlderVersion_andNoUpgrader_andAutoUpgradeFalse_throws() throws Exception {
        File cfg = workDir.resolve("readonly-oldversion.yaml").toFile();

        // create file with old version 0.0 and Testing: 5
        YamlConfiguration initial = new YamlConfiguration();
        initial.set(SettingsManager.getSettings() == null ? "Version" : SettingsManager.getSettings().getVersionKey(), "0.0");
        initial.set("Testing", 5);
        initial.save(cfg);

        // configure: ensure autoUpgrade is false
        SettingsManager.configure()
                .autoUpgrade(false)
                .saveReadOnlyConfigOnVersionUpgrade(true) // irrelevant for this test
                .done();

        TestBundle bundle = new TestBundle(new TestVersion(1, 0), cfg, ConfigType.READONLY);

        assertThrows(IllegalStateException.class, () -> SettingsManager.getInstance().registerConfig(bundle),
                "When file version differs and no upgrader exists and autoUpgrade=false, registration must fail");
    }

    @Test
    public void registeringWithExistingOlderVersion_andAutoUpgradeTrue_updatesVersion_ifConfiguredToSave() throws Exception {
        File cfg = workDir.resolve("readonly-autoupgrade.yaml").toFile();

        // create file with old version 0.0 and Testing: 5
        YamlConfiguration initial = new YamlConfiguration();
        initial.set("Version", "0.0");
        initial.set("Testing", 5);
        initial.save(cfg);

        // configure: enable auto upgrade and ensure saving read-only on upgrade is true
        SettingsManager.configure()
                .autoUpgrade(true)
                .saveReadOnlyConfigOnVersionUpgrade(true)
                .done();

        TestBundle bundle = new TestBundle(new TestVersion(2, 0), cfg, ConfigType.READONLY);

        assertDoesNotThrow(() -> SettingsManager.getInstance().registerConfig(bundle),
                "With autoUpgrade=true registration should succeed");

        // after registration, the file's Version should be updated to bundle's version (because saveReadOnlyConfigOnVersionUpgrade=true)
        YamlConfiguration yamlAfter = new YamlConfiguration();
        yamlAfter.load(cfg);
        assertEquals(bundle.getVersion().toString(), yamlAfter.getString("Version"),
                "File Version should be updated to the new bundle version after auto-upgrade when configured to save");
    }

    @Test
    public void environmentOverwriters_onlyAffectInMemory_andNotPersisted() throws Exception {
        File cfg = workDir.resolve("env-inmemory.yaml").toFile();

        // create file with Testing: 10 (default)
        YamlConfiguration initial = new YamlConfiguration();
        initial.set("Version", "1.0");
        initial.set("Testing", 10);
        initial.save(cfg);

        // ensure overwriters apply in the order: first returns null, second returns system property
        System.setProperty("HELLO", "42");

        SettingsManager.configure()
                .enableOverwriters(true)
                .overwriters(List.of(
                        // first overwriter: always null
                        (String _) -> null,
                        // second: system property
                        System::getProperty
                ))
                .autoUpgrade(false)
                .done();

        TestBundle bundle = new TestBundle(new TestVersion(1, 0), cfg, ConfigType.READONLY);

        // registering should succeed (file exists and version matches)
        assertDoesNotThrow(() -> SettingsManager.getInstance().registerConfig(bundle));

        // in-memory field should be overwritten by env (via second overwriter)
        assertEquals(42, bundle.test, "In-memory field should reflect overwriter value");

        // reload YAML from disk (should still read 10)
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.load(cfg);
        assertEquals(10, Integer.parseInt(String.valueOf(onDisk.get("Testing"))),
                "On-disk YAML must not be changed by overwriter; env values must never be persisted");

        // call save() - ensure that env-overwritten value is NOT written back to YAML
        SettingsManager.getInstance().save();

        YamlConfiguration afterSave = new YamlConfiguration();
        afterSave.load(cfg);
        assertEquals(10, Integer.parseInt(String.valueOf(afterSave.get("Testing"))),
                "After save, YAML should still not contain env-overwritten value");
    }

    @Test
    public void saveById_and_saveByInstance_work_and_roundtrip() throws Exception {
        File cfg = workDir.resolve("roundtrip.yaml").toFile();
        TestBundle b = new TestBundle(new TestVersion(1, 1), cfg, ConfigType.SETTINGS);

        SettingsManager.configure()
                .autoUpgrade(true)
                .saveReadOnlyConfigOnVersionUpgrade(true)
                .done();

        UUID id = SettingsManager.getInstance().registerConfig(b);
        assertNotNull(id);

        Object got = SettingsManager.getInstance().getSettingsBundleInstance(TestBundle.class, id);
        assertSame(b, got);

        assertTrue(SettingsManager.getInstance().save(b), "save by instance should return true");
        assertTrue(SettingsManager.getInstance().save(TestBundle.class, id), "save by id should return true");

        // and file must exist on disk
        assertTrue(cfg.exists(), "Config file must exist after saving");
    }
}
