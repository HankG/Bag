package com.brettonw.bag;

import com.brettonw.AppTest;
import org.junit.Test;

public class KeyTest {
    @Test
    public void test() {
        new Key ();
    }

    @Test
    public void testKey() {
        new Key ();

        // hierarchical values
        BagObject bagObject = new BagObject ().put ("com/brettonw/bag/name", "test");
        String com = "com";
        String brettonw = "brettonw";
        String bag = "bag";
        String name = "name";

        String key = Key.cat (com, brettonw);
        AppTest.report (bagObject.has (Key.cat (key, "test")), false, "Key - test that an incorrect path concatenation returns false");
        AppTest.report (bagObject.has (Key.cat (key, bag, name, "xxx")), false, "Key - test that a longer incorrect path concatenation returns false");
        AppTest.report (bagObject.has (Key.cat (com, brettonw, bag, name)), true, "Key - test that a correct path returns true");
    }

}
