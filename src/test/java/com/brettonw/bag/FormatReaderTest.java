package com.brettonw.bag;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class FormatReaderTest {

    @Test
    public void testFormatReader () {
        BagObject bagObject = BagObjectFrom.file (new File ("data", "title.properties"));
        assertTrue (bagObject.getString ("note").equals ("this is a test"));
    }
}
