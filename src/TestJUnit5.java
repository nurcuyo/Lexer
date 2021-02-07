import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJUnit5 {
    @Test
    void test(){
        assertEquals(Testing.add(5,6), 11);
    }
}
