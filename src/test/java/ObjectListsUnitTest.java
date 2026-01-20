import com.toxicstoxm.StormYAML.file.YamlConfiguration;
import com.toxicstoxm.StormYAML.yaml.ConfigurationSection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for object lists stored as ConfigurationSection entries in YamlConfiguration.
 */
public class ObjectListsUnitTest {

    public static class RandomObject {
        public int i = 5;
        public String s = "Hello";
    }

    @Test
    public void yamlListOfConfigurationSections_roundtrips_and_valuesAreAccessible() {
        List<RandomObject> input = new ArrayList<>();
        for (int k = 0; k < 5; k++) input.add(new RandomObject());

        YamlConfiguration yaml = new YamlConfiguration();

        List<ConfigurationSection> sections = new ArrayList<>();
        int idx = 0;
        for (RandomObject o : input) {
            ConfigurationSection sec = new YamlConfiguration();
            sec.set("i", o.i + idx);
            sec.set("s", o.s + idx);
            sections.add(sec);
            idx++;
        }

        yaml.set("TestingList", sections);
        String dumped = yaml.saveToString();
        assertNotNull(dumped);
        assertTrue(dumped.contains("TestingList"));

        // getList without generic type -> should still return the underlying ConfigurationSection items
        List<?> raw = yaml.getList("TestingList");
        assertNotNull(raw);
        assertEquals(5, raw.size());
        for (Object o : raw) {
            assertTrue(o instanceof ConfigurationSection, "Elements must be ConfigurationSection");
        }

        @SuppressWarnings("unchecked")
        List<ConfigurationSection> loaded = (List<ConfigurationSection>) raw;
        ConfigurationSection first = loaded.get(0);
        assertEquals(5, first.getInt("i"));
        assertEquals("Hello0", first.getString("s"));
    }
}
