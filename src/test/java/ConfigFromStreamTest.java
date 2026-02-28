import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigFromStreamTest {
    public static class TestBundle extends SettingsBundle {
        public TestBundle(@NotNull InputStream is) {
            super(new ConfigVersion(1, 0, 0), is);
        }

        @YAMLSetting(name = "Test-Value")
        public int testValue = 5;
    }

    @Test
    public void test() {
        String s = """
                Version: 1.0.0
                Test-Value: 45346
                """;

        TestBundle tb = new TestBundle(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
        SettingsManager.getInstance().registerConfig(tb);
        assertEquals(45346, tb.testValue);
    }
}
