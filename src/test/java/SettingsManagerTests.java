import com.toxicstoxm.YAJSI.api.settings.ManualAdjustmentHelper;
import com.toxicstoxm.YAJSI.api.settings.SettingsManager;
import com.toxicstoxm.YAJSI.api.settings.YAMLConfiguration;
import com.toxicstoxm.YAJSI.api.settings.YAMLSetting;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Set;

@YAMLConfiguration()
public class SettingsManagerTests {

    private static final String wantedOutput = """
            stringField: default
            intField: 42
            doubleField: 99.99
            longField: 1000000000
            floatField: 10.5
            # This is a string field
            # Used for testing
            customStringName: customDefaultValue
            # This is an integer field
            # Used to test integer fields
            customIntName: 123
            nestedSection:
              sectionString: sectionStringDefault
              sectionDouble: 3.14159
              # Private field comment
              # Test nested field
              sectionPrivateString: privateSectionString
              # List of strings in the nested section
              nestedSectionList:
              - apple
              - banana
              - cherry
              innerSection:
                innerString: innerStringDefault
                innerBoolean: false
                # This is an inner custom field
                # Testing the nested structure
                innerCustomField: 7.8
            privateBoolean: true
            # This is a list of integers
            # Test list
            integerList:
            - 1
            - 2
            - 3
            - 4
            - 5
            # This is a list of strings
            # Test list of strings
            stringList:
            - hello
            - world
            # This is a list of booleans
            # Test list of booleans
            booleanList:
            - true
            - false
            - true
            anotherNestedSection:
              # Another section string
              anotherCustomName: anotherDefaultValue
              # This is a list of integers in another section
              anotherSectionList:
              - 10
              - 20
              - 30
              - 40
              nestedTestSection:
                sectionString: sectionStringDefault
                sectionDouble: 3.14159
                # Private field comment
                # Test nested field
                sectionPrivateString: privateSectionString
                # List of strings in the nested section
                nestedSectionList:
                - apple
                - banana
                - cherry
                innerSection:
                  innerString: innerStringDefault
                  innerBoolean: false
                  # This is an inner custom field
                  # Testing the nested structure
                  innerCustomField: 7.8
              anotherPrivateBoolean: false
            complexSection:
              complexString: complexValue
              # This is a complex field
              # It has both nested and list data
              complexField:
                sectionString: sectionStringDefault
                sectionDouble: 3.14159
                # Private field comment
                # Test nested field
                sectionPrivateString: privateSectionString
                # List of strings in the nested section
                nestedSectionList:
                - apple
                - banana
                - cherry
                innerSection:
                  innerString: innerStringDefault
                  innerBoolean: false
                  # This is an inner custom field
                  # Testing the nested structure
                  innerCustomField: 7.8
            # A simple boolean field
            # For testing purposes
            booleanField: true
            # This is a custom long field
            # It's used for testing
            customLongName: 123456789
            # This is a double field
            # Used for testing
            anotherDoubleField: 22.22
            # This setting is currently unused
            test: 22""";

    // Basic fields with default values
    public String stringField = "default";
    public int intField = 42;
    public double doubleField = 99.99;
    public long longField = 1000000000L;
    public float floatField = 10.5F;

    // Fields with custom YAML names and comments
    @YAMLSetting(name = "customStringName", comments = {"This is a string field", "Used for testing"})
    public String customStringField = "customDefaultValue";

    @YAMLSetting(name = "customIntName", comments = {"This is an integer field", "Used to test integer fields"})
    public int customIntField = 123;

    // Fields with nested objects
    public NestedTestSection nestedSection = new NestedTestSection();

    // Private and protected fields
    private boolean privateBoolean = true;
    protected char protectedChar = 'A';

    // Lists with basic types and comments
    @YAMLSetting(comments = {"This is a list of integers", "Test list"})
    public List<Integer> integerList = List.of(1, 2, 3, 4, 5);

    @YAMLSetting(comments = {"This is a list of strings", "Test list of strings"})
    private List<String> stringList = List.of("hello", "world");

    @YAMLSetting(comments = {"This is a list of booleans", "Test list of booleans"})
    public List<Boolean> booleanList = List.of(true, false, true);

    // Another nested section
    public AnotherTestSection anotherNestedSection = new AnotherTestSection();

    public static class NestedTestSection {

        // Basic fields in the nested section
        public String sectionString = "sectionStringDefault";
        public double sectionDouble = 3.14159;

        // Private field with custom comments
        @YAMLSetting(comments = {"Private field comment", "Test nested field"})
        private String sectionPrivateString = "privateSectionString";

        // List of items in the nested section
        @YAMLSetting(comments = {"List of strings in the nested section"})
        public List<String> nestedSectionList = List.of("apple", "banana", "cherry");

        // Nested section with other fields
        public NestedInnerSection innerSection = new NestedInnerSection();
    }

    public static class NestedInnerSection {
        public String innerString = "innerStringDefault";
        public boolean innerBoolean = false;
        @YAMLSetting(name = "innerCustomField", comments = {"This is an inner custom field", "Testing the nested structure"})
        public float innerFloat = 7.8F;
    }

    public static class AnotherTestSection {

        // Basic fields with different types
        @YAMLSetting(name = "anotherCustomName", comments = {"Another section string"})
        public String anotherCustomString = "anotherDefaultValue";

        @YAMLSetting(comments = {"This is a list of integers in another section"})
        public List<Integer> anotherSectionList = List.of(10, 20, 30, 40);

        // Nested objects inside another section
        public NestedTestSection nestedTestSection = new NestedTestSection();

        // Private and protected fields in another section
        private boolean anotherPrivateBoolean = false;
        protected char anotherProtectedChar = 'Z';
    }

    // Complex list with mixed types, including null and empty elements
    @YAMLSetting(comments = {"List with mixed types, including null and empty elements"})
    public List<Object> mixedList = List.of("string", 123, "null", true, 4.56F, "", new NestedTestSection());

    // A complex structure combining multiple nested levels and list fields
    public ComplexSection complexSection = new ComplexSection();

    public static class ComplexSection {
        public String complexString = "complexValue";
        public List<NestedTestSection> sectionList = List.of(new NestedTestSection(), new NestedTestSection());

        @YAMLSetting(name = "complexField", comments = {"This is a complex field", "It has both nested and list data"})
        public NestedTestSection complexField = new NestedTestSection();
    }

    // Another list of lists
    @YAMLSetting(comments = {"List of lists", "Testing nested lists"})
    public List<List<String>> listOfLists = List.of(
            List.of("one", "two", "three"),
            List.of("four", "five", "six")
    );

    // A set to test other collection types
    @YAMLSetting(comments = {"Testing a set of strings"})
    public Set<String> stringSet = Set.of("setOne", "setTwo", "setThree");

    // Mixed fields with comments and some using the default names
    @YAMLSetting(comments = {"A simple boolean field", "For testing purposes"})
    public boolean booleanField = true;

    @YAMLSetting(name = "customLongName", comments = {"This is a custom long field", "It's used for testing"})
    public long customLongField = 123456789L;

    @YAMLSetting(comments = {"This is a double field", "Used for testing"})
    public double anotherDoubleField = 22.22;

    @YAMLSetting(name = "complexTest", comments = {"Testing complex test", "Including nested objects and lists"})
    public List<ComplexTest> complexTestList = List.of(new ComplexTest(), new ComplexTest());

    public static class ComplexTest {
        public String name = "defaultName";
        public int value = 10;
        public boolean active = true;
    }

    @YAMLSetting.Ignore
    private SettingsManager settingsManager;

    public SettingsManagerTests() {}

    @Test
    public void testSettingsManagerAPI() {
        settingsManager = SettingsManager.getInstance();

        settingsManager.registerYAMLConfiguration(this);
        settingsManager.restoreDefaultsFor(this);
        settingsManager.unregisterYAMLConfiguration(this);
        ManualAdjustmentHelper helper = settingsManager.makeManualAdjustmentsTo(this);

        helper.getYAML().set("test", 22);
        helper.returnResponsibility();

        File configFile = settingsManager.getConfigLocation(this);

        settingsManager.save();

        if (!settingsManager.makeManualAdjustmentsTo(this).getYAML().saveToString().strip().equals(wantedOutput)) {
            throw new RuntimeException("Output is incorrect!");
        }

        if (!configFile.delete()) {
            throw new RuntimeException("Failed to cleanup test config file!");
        }
    }
}
