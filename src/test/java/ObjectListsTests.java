import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

public class ObjectListsTests {

    public static class RandomObject {

        public int i = 5;

        public String s = "Hello";

        @Override
        public String toString() {
            return "i: " + i + ", s: " + s;
        }
    }


    @Test
    public void objectListTest() {
        RandomObject object = new RandomObject();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Testing", object);

        System.out.println(yaml.saveToString());

        RandomObject object1 = (RandomObject) yaml.get("Testing");

        System.out.println(object1);
    }
}
