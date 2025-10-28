import com.toxicstoxm.YAJSI.SettingsBundle;
import com.toxicstoxm.YAJSI.SettingsManager;
import com.toxicstoxm.YAJSI.upgrading.ConfigVersion;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

public class ArrayTests {

    public static class TestBundle extends SettingsBundle {

        public TestBundle(File f) {
            super(new ConfigVersion(1, 0, 0), f);
        }

        public int[] test = new int[5];
    }

    @Test
    public void arrayTest() {
        TestBundle bundle = new TestBundle(new File("src/test/resources/arrays.yaml"));
        bundle.register();

        bundle.test[0] = 54;
        bundle.test[1] = 55;
        bundle.test[2] = 5467;

        System.out.println(Arrays.toString(bundle.test));

        SettingsManager.getInstance().save();
    }
}
