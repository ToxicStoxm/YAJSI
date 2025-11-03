import com.toxicstoxm.YAJSI.SettingsManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InstanceIntegrityTests {
    @Test
    public void instanceIntegrityTest() {

        SettingsManager.configure()
                .autoUpgrade(true)
                .done();

        Assertions.assertTrue(SettingsManager.getSettings().isAutoUpgrade());

    }
}
