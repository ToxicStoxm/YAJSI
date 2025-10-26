import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
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
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 0, 0).toString());
            updated.set("testing", old.get("test"));

            return updated;
        }, new ConfigVersion(0,9,9));

        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 2, 0).toString());
            updated.set("Tested", old.get("testing"));

            return updated;
        }, new ConfigVersion(1,0,0));

        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 3, 0).toString());
            updated.set("subSection.testing", old.get("Tested"));

            return updated;
        }, new ConfigVersion(1,2,0));

        SettingsManager.getInstance().registerUpgradeCallback(TestBundle.class, (old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 5, 2).toString());
            updated.set("subSection.Test", old.get("subSection.testing"));

            return updated;
        }, new ConfigVersion(1,3,0));

        AppConfigBundle test = new AppConfigBundle(new File("src/test/resources/testing.yaml"));

        // From 1.0.0 → 1.3.0
        test.registerUpgradeCallback((old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 3, 0).toString());

            updated.set("application.name", old.get("app.name"));
            updated.set("application.version", old.get("app.version"));
            updated.set("application.enabled", old.get("app.enabled"));
            updated.set("application.startup.delay_ms", old.get("app.launch_delay_ms"));

            updated.set("logging", old.get("logging"));
            updated.set("network", old.get("network"));
            updated.set("ui", old.get("ui"));
            updated.set("modules", old.get("modules"));
            updated.set("experimental", old.get("experimental"));
            updated.set("environment", old.get("environment"));

            return updated;
        }, new ConfigVersion(1, 0, 0));

        // From 1.3.0 → 1.5.0
        test.registerUpgradeCallback((old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(1, 5, 0).toString());

            updated.set("system.logging", old.get("logging"));
            updated.set("system.networking", old.get("network"));

            updated.set("interface", old.get("ui"));
            updated.set("components", old.get("modules"));
            updated.set("application.details.version", old.get("application.version"));
            updated.set("application.details.enabled", old.get("application.enabled"));
            updated.set("application.details.startup.delay_ms", old.get("application.startup.delay_ms"));
            updated.set("application.id", old.get("application.name"));

            updated.set("experimental", old.get("experimental"));
            updated.set("environment", old.get("environment"));

            return updated;
        }, new ConfigVersion(1, 3, 0));

        // From 1.5.0 → 2.0.0
        test.registerUpgradeCallback((old, id) -> {
            YamlConfiguration updated = new YamlConfiguration();
            updated.set(SettingsManager.getSettings().getVersionKey(), new ConfigVersion(2, 0, 0).toString());

            // rename + restructure for final v2.0.0 layout
            updated.set("application.id", old.get("application.id"));
            updated.set("application.details", old.get("application.details"));

            updated.set("system.logging.default_level", old.get("system.logging.level"));
            updated.set("system.logging.outputs.file", old.get("system.logging.file"));
            updated.set("system.logging.outputs.console", old.get("system.logging.console"));
            updated.set("system.networking", old.get("system.networking"));

            updated.set("interface", old.get("interface"));
            updated.set("components", old.get("components"));

            updated.set("experimental.features", old.get("experimental.feature_flags"));
            updated.set("experimental.performance", old.get("experimental.values"));
            updated.set("environment.overrides", old.get("environment"));

            return updated;
        }, new ConfigVersion(1, 5, 0));

        /*
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
         */

        test.registerUpgradeCallback(AppConfigBundle::upgradeFrom1_0_0, new ConfigVersion(1, 0, 0));

        test.register();

        UUID id3 = SettingsManager.getInstance().registerConfig(new AppConfigBundle(new File("src/test/resources/old2.yaml")));
        SettingsManager.configure()
                .saveReadOnlyConfigOnVersionUpgrade(false)
                .done();
        UUID id4 = SettingsManager.getInstance().registerConfig(new AppConfigBundle(new File("src/test/resources/old.yaml")));
        UUID id5 = SettingsManager.getInstance().registerConfig(new AppConfigBundle(new File("src/test/resources/new.yaml")));

        AppConfigBundle instance3 = SettingsManager.getInstance().getSettingsBundleInstance(AppConfigBundle.class, id3);
        assert instance3 != null;
        System.out.println(instance3.getId());

        AppConfigBundle instance4 = SettingsManager.getInstance().getSettingsBundleInstance(AppConfigBundle.class, id4);
        assert instance4 != null;
        System.out.println(instance4.getId());

        AppConfigBundle instance5 = SettingsManager.getInstance().getSettingsBundleInstance(AppConfigBundle.class, id5);
        assert instance5 != null;
        System.out.println(instance5.getId());

        SettingsManager.getInstance().save();
    }

    public static class AppConfigBundle extends SettingsBundle {

        public AppConfigBundle(File f) {
            super(new ConfigVersion(2, 0, 0), f, ConfigType.READONLY);
        }

        @YAMLSetting(name = "application")
        public ApplicationSection application;

        @YAMLSetting(name = "system")
        public SystemSection system;

        @YAMLSetting(name = "interface")
        public InterfaceSection ui;

        @YAMLSetting(name = "components")
        public ComponentsSection components;

        @YAMLSetting(name = "experimental")
        public ExperimentalSection experimental;

        // ===== Nested Structures =====

        public static class ApplicationSection {
            @YAMLSetting(name = "id")
            public String id;

            @YAMLSetting(name = "details")
            public DetailsSection details;

            public static class DetailsSection {
                @YAMLSetting(name = "version")
                public String version;

                @YAMLSetting(name = "enabled")
                public boolean enabled;

                @YAMLSetting(name = "startup")
                public StartupSection startup;

                public static class StartupSection {
                    @YAMLSetting(name = "delay_ms")
                    public int delayMs;
                }
            }
        }

        public static class SystemSection {
            @YAMLSetting(name = "logging")
            public LoggingSection logging;

            @YAMLSetting(name = "networking")
            public NetworkingSection networking;

            public static class LoggingSection {
                @YAMLSetting(name = "default_level")
                public String defaultLevel;

                @YAMLSetting(name = "outputs")
                public OutputsSection outputs;

                public static class OutputsSection {
                    @YAMLSetting(name = "file")
                    public FileOutput file;

                    @YAMLSetting(name = "console")
                    public ConsoleOutput console;

                    public static class FileOutput {
                        @YAMLSetting(name = "path")
                        public String path;

                        @YAMLSetting(name = "rotation")
                        public Rotation rotation;

                        @YAMLSetting(name = "format")
                        public String format;

                        public static class Rotation {
                            @YAMLSetting(name = "enabled")
                            public boolean enabled;

                            @YAMLSetting(name = "size_limit_mb")
                            public int sizeLimitMb;

                            @YAMLSetting(name = "file_limit")
                            public int fileLimit;

                            @YAMLSetting(name = "compression")
                            public boolean compression;
                        }
                    }

                    public static class ConsoleOutput {
                        @YAMLSetting(name = "active")
                        public boolean active;

                        @YAMLSetting(name = "color_mode")
                        public String colorMode;
                    }
                }
            }

            public static class NetworkingSection {
                @YAMLSetting(name = "timeout")
                public int timeout;

                @YAMLSetting(name = "retry")
                public Retry retry;

                @YAMLSetting(name = "apis")
                public Apis apis;

                public static class Retry {
                    @YAMLSetting(name = "count")
                    public int count;

                    @YAMLSetting(name = "mode")
                    public String mode;

                    @YAMLSetting(name = "base_delay")
                    public int baseDelay;
                }

                public static class Apis {
                    @YAMLSetting(name = "main")
                    public ApiEndpoint main;

                    @YAMLSetting(name = "backup")
                    public ApiEndpoint backup;

                    public static class ApiEndpoint {
                        @YAMLSetting(name = "url")
                        public String url;

                        @YAMLSetting(name = "headers")
                        public Map<String, Object> headers;
                    }
                }
            }
        }

        public static class InterfaceSection {
            @YAMLSetting(name = "theme")
            public String theme;

            @YAMLSetting(name = "font")
            public Font font;

            @YAMLSetting(name = "window")
            public Window window;

            public static class Font {
                @YAMLSetting(name = "family")
                public String family;

                @YAMLSetting(name = "size")
                public int size;
            }

            public static class Window {
                @YAMLSetting(name = "width")
                public int width;

                @YAMLSetting(name = "height")
                public int height;

                @YAMLSetting(name = "resizable")
                public boolean resizable;

                @YAMLSetting(name = "fullscreen")
                public boolean fullscreen;
            }
        }

        public static class ComponentsSection {
            @YAMLSetting(name = "auth")
            public Auth auth;

            @YAMLSetting(name = "metrics")
            public Metrics metrics;

            public static class Auth {
                @YAMLSetting(name = "active")
                public boolean active;

                @YAMLSetting(name = "mode")
                public String mode;

                @YAMLSetting(name = "token_expiry")
                public int tokenExpiry;
            }

            public static class Metrics {
                @YAMLSetting(name = "active")
                public boolean active;

                @YAMLSetting(name = "push_interval")
                public int pushInterval;

                @YAMLSetting(name = "endpoint")
                public String endpoint;
            }
        }

        public static class ExperimentalSection {
            @YAMLSetting(name = "features")
            public Features features;

            @YAMLSetting(name = "performance")
            public Performance performance;

            public static class Features {
                @YAMLSetting(name = "scheduler_v2")
                public boolean schedulerV2;

                @YAMLSetting(name = "caching")
                public boolean caching;

                @YAMLSetting(name = "strict")
                public boolean strict;
            }

            public static class Performance {
                @YAMLSetting(name = "limits")
                public Limits limits;

                @YAMLSetting(name = "exclude_patterns")
                public List<String> excludePatterns;

                public static class Limits {
                    @YAMLSetting(name = "cpu_usage")
                    public double cpuUsage;

                    @YAMLSetting(name = "memory_usage")
                    public double memoryUsage;
                }
            }
        }

        public static YamlConfiguration upgradeFrom1_0_0(YamlConfiguration old, UUID id) {
            return null;
        }
    }
}
