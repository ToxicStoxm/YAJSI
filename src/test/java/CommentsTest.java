import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import com.toxicstoxm.YAJSI.upgrading.Upgrader;
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
public class CommentsTest {
    private Path tmp;

    @BeforeEach
    public void before() throws Exception {
        tmp = Files.createTempDirectory("yajsi-comments-upgrade-");
        resetSettingsManagerSingleton();
    }

    @AfterEach
    public void after() throws Exception {
        if (tmp != null && Files.exists(tmp)) {
            try (var s = Files.walk(tmp)) {
                s.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
        resetSettingsManagerSingleton();
    }

    private static void resetSettingsManagerSingleton() throws Exception {
        Field f = SettingsManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    /**
     * Bundle with evolving comments across versions
     */
    public static class CommentedBundle extends SettingsBundle {

        public CommentedBundle(ConfigVersion v, File f, ConfigType type) {
            super(v, f, type);
        }

        @YAMLSetting(
                name = "message",
                comments = {
                        "Old comment",
                        "Applies to version 1.0.0"
                }
        )
        private String message = "Hello";

        /**
         * Manual upgrader that updates comments
         */
        @Upgrader(base = "1.0.0", factory = ConfigVersion.Factory.class)
        private YamlConfiguration upgrade1_0_0(YamlConfiguration old, UUID id) {
            YamlConfiguration updated = new YamlConfiguration();

            updated.set(
                    SettingsManager.getSettings().getVersionKey(),
                    new ConfigVersion(1, 1, 0).toString()
            );

            updated.set("message", old.get("message"));

            updated.setComments("message", List.of(
                    "New comment",
                    "Updated in version 1.1.0"
            ));

            return updated;
        }
    }

    @Test
    public void commentsAreUpgradedInReadOnlyConfig() throws Exception {
        File cfg = tmp.resolve("readonly-comments.yaml").toFile();

        // create old config manually
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Version", "1.0.0");
        yaml.set("message", "Hello");
        yaml.setComments("message", List.of(
                "Old comment",
                "Applies to version 1.0.0"
        ));
        yaml.save(cfg);

        SettingsManager.configure()
                .autoUpgrade(true)
                .done();

        SettingsManager.getInstance().registerConfig(
                new CommentedBundle(
                        new ConfigVersion(1, 1, 0),
                        cfg,
                        ConfigType.READONLY
                )
        );

        SettingsManager.getInstance().save();

        YamlConfiguration upgraded = new YamlConfiguration();
        upgraded.load(cfg);

        assertLinesMatch(
                List.of("New comment", "Updated in version 1.1.0"),
                upgraded.getComments("message")
        );
    }

    @Test
    public void commentsAreUpgradedInSettingsConfig() throws Exception {
        File cfg = tmp.resolve("settings-comments.yaml").toFile();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Version", "1.0.0");
        yaml.set("message", "Hello");
        yaml.setComments("message", List.of(
                "Old comment",
                "Applies to version 1.0.0"
        ));
        yaml.save(cfg);

        SettingsManager.configure()
                .autoUpgrade(true)
                .done();

        CommentedBundle bundle = new CommentedBundle(
                new ConfigVersion(1, 1, 0),
                cfg,
                ConfigType.SETTINGS
        );

        SettingsManager.getInstance().registerConfig(bundle);
        SettingsManager.getInstance().save();

        YamlConfiguration upgraded = new YamlConfiguration();
        upgraded.load(cfg);

        assertLinesMatch(
                List.of("New comment", "Updated in version 1.1.0"),
                upgraded.getComments("message")
        );

        assertEquals("Hello", bundle.message);
    }
}

