import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import com.toxicstoxm.YAJSI.ExternalYAMLSerializer;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSerializable;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;

public class CustomSerializableTests {

    public static class TestBundleSerializer implements ExternalYAMLSerializer<TestBundle.LoggerSection> {


        @Override
        public TestBundle.LoggerSection deserialize(@NotNull ConfigurationSection yaml) {
            TestBundle.LoggerSection l = new TestBundle.LoggerSection();
            l.helloWorld = yaml.getInt("Hallo2") - 10;
            return l;
        }

        @Override
        public @NotNull ConfigurationSection serialize(TestBundle.LoggerSection o) {
            ConfigurationSection section = emptyConfigSection();
            section.set("Hallo2", o.helloWorld + 10);
            return section;
        }
    }

    public static class TestBundle extends SettingsBundle {

        public TestBundle() {
            super(new ConfigVersion(1,0,0), new File("src/test/resources/SerializerTests/settings.yaml"));
        }

        public int testing;
        public Integer lmaoLoL;
        public Double testing2;
        public Float hello;
        public float halli;

        private LoggerSection section;

        @NoArgsConstructor
        public static class LoggerSection implements YAMLSerializable {

            public int helloWorld;

            @Override
            public @NotNull ConfigurationSection serializeSelf() {
                ConfigurationSection section = emptyConfigSection();
                section.set("Hallo2", helloWorld + 10);
                return section;
            }

            @Override
            public Object deserialize(@NotNull ConfigurationSection yaml) {
                LoggerSection l = new LoggerSection();
                l.helloWorld = yaml.getInt("Hallo2") - 10;
                return l;
            }
        }
    }

    @Test
    public void customSerializerTest() {
        TestBundle bundle = new TestBundle();
        SettingsManager.getInstance().registerConfig(bundle);

        System.out.println(bundle.section.helloWorld);
        SettingsManager.getInstance().save();
    }
}
