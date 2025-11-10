import com.toxicstoxm.YAJSI.ConfigType;
import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.YAMLSetting;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class MoreObjectListTests {

    public static class Settings extends SettingsBundle {

        public Settings(File f) {
            super(new ConfigVersion(1, 0, 0), f, ConfigType.SETTINGS);
        }

        @YAMLSetting(name = "Width")
        public int width = 50;

        @YAMLSetting(name = "Loggers")
        public List<Logger> loggers;

        @NoArgsConstructor
        public static class Logger {
            @YAMLSetting(name = "Enable-Limit")
            public boolean enableLimit = false;
            @YAMLSetting(name = "Message-Limit")
            public int messageLimit;
            @YAMLSetting(name = "Message-Template")
            public String messageTemplate = "[%date%] [%level%] [%area%]: %message%";

            public Logger(boolean enableLimit, int messageLimit, String messageTemplate) {
                this.enableLimit = enableLimit;
                this.messageLimit = messageLimit;
                this.messageTemplate = messageTemplate;
            }

            @Override
            public String toString() {
                return "Logger{" +
                        "enableLimit=" + enableLimit +
                        ", messageLimit=" + messageLimit +
                        ", messageTemplate='" + messageTemplate + '\'' +
                        '}';
            }
        }
    }

    @Test
    public void objectListTests() {
        Settings settings = new Settings(new File("src/test/resources/ObjectListTests/settings.yaml"));

        settings.register();

        settings.loggers.add(new Settings.Logger(false, 0, "435324857dfgdsfg"));

        SettingsManager.getInstance().save();

        System.out.println(settings.loggers);
    }
}
