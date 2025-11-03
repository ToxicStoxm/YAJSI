import com.toxicstoxm.YAJSI.parsing.Priority;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;

public class PriorityTests {
    @Test
    public void priorityTest() {
        System.out.println(new Priority(0, false).compareTo(new Priority(0, true)));
        System.out.println(new Priority(5, false).compareTo(new Priority(1000, true)));
        System.out.println(new Priority(5, true).compareTo(new Priority(0, true)));
        System.out.println(new Priority(0, true).compareTo(new Priority(0, true)));
        System.out.println(new Priority(0, false).compareTo(new Priority(0, false)));

        TreeSet<Priority> testing = new TreeSet<>();

        testing.add(new Priority(0, false));
        testing.add(new Priority(0, false));
        testing.add(new Priority(0, true));
        testing.add(new Priority(0, true));
        testing.add(new Priority(5, false));
        testing.add(new Priority(5, true));
        testing.add(new Priority(1000, true));

        for (Priority p : testing) {
            System.out.println(p);
        }
    }
}
