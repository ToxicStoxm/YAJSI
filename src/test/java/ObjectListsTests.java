import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import org.junit.jupiter.api.Test;

import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

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
        List<RandomObject> testing = new ArrayList<>();
        testing.add(new RandomObject());
        testing.add(new RandomObject());
        testing.add(new RandomObject());
        testing.add(new RandomObject());
        testing.add(new RandomObject());

        YamlConfiguration yaml = new YamlConfiguration();
        List<ConfigurationSection> sections = new ArrayList<>();
        for (RandomObject o : testing) {
            ConfigurationSection section = new YamlConfiguration();
            section.set("i", o.i);
            section.set("s", o.s);
            sections.add(section);
        }
        yaml.set("TestingList", sections);
        System.out.println(yaml.saveToString());

        List<ConfigurationSection> l = (List<ConfigurationSection>) yaml.getList("TestingList");

        ConfigurationSection sec = l.getFirst();
        System.out.println(sec.get("i"));

        List<?> list = yaml.getList("TestingList");
        for (TypeVariable<?> par : list.getClass().getTypeParameters()) {
            System.out.println(par.getTypeName());
        }
    }
}
