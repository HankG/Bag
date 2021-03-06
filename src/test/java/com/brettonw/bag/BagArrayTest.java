package com.brettonw.bag;

import com.brettonw.AppTest;
import com.brettonw.bag.expr.BooleanExpr;
import com.brettonw.bag.expr.Exprs;
import com.brettonw.bag.formats.MimeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class BagArrayTest {
    private static final Logger log = LogManager.getLogger (BagArrayTest.class);

    @Test
    public void testBagArray() {
        // a first basic test
        BagArray bagArray = new BagArray ()
                .add ("abdefg")
                .add (123456)
                .add (123.456)
                .add (true);
        bagArray.insert (1, 234567);
        bagArray.replace (2, 345678);

        assertEquals ("Check get double", 123.456, bagArray.getDouble (3), 1.0e-9);
        assertEquals ("Check size", 5, bagArray.getCount ());

        bagArray.insert (10, 456789);
        assertEquals ("Check size", 11, bagArray.getCount ());

        // convert that bag to a string
        String bagArrayAsString = bagArray.toString ();
        BagArray reconBagArray = BagArrayFrom.string (bagArrayAsString);
        AppTest.report (reconBagArray.toString (), bagArrayAsString, "BagArray - simple round trip with null values");
    }

    @Test
    public void testCopyConstructor() {
        // a first basic test
        BagArray bagArray = new BagArray ()
                .add ("abdefg")
                .add (123456)
                .add (123.456)
                .add (true);
        bagArray.insert (1, 234567);
        bagArray.replace (2, 345678);

        BagArray duplicate = new BagArray (bagArray);
        AppTest.report (bagArray, duplicate, "BagArray - deep copy should succeed and be equal to the original");
    }

    @Test
    public void testComplicated() {
        // a more complicated array test
        BagArray testArray = new BagArray ();
        testArray.add ("Bretton");
        testArray.add ("Wade");
        testArray.add (220.5);
        testArray.add (true);
        testArray.add (42);

        AppTest.report (testArray.getCount (), 5, "BagArray simple count check");
        AppTest.report (testArray.getString (0), "Bretton", "BagArray simple string extraction");
        AppTest.report (testArray.getDouble (2), 220.5, "BagArray simple double extraction");
        AppTest.report (testArray.getFloat (2), 220.5f, "BagArray simple float extraction");
        AppTest.report (testArray.getBoolean (3), true, "BagArray simple bool extraction");
        AppTest.report (testArray.getInteger (4), 42, "BagArray simple int extraction");

        testArray.remove (2);
        AppTest.report (testArray.getCount (), 4, "BagArray simple removal - count updated");
        AppTest.report (testArray.getBoolean (2), true, "BagArray simple bool extraction");
        AppTest.report (testArray.getInteger (3), 42, "BagArray simple int extraction");

        String testString = testArray.toString ();
        AppTest.report (testString, testString, "BagArray simple toString exercise (" + testString + ")");

        BagArray reconArray = BagArrayFrom.string (testString);
        String reconString = reconArray.toString ();
        AppTest.report (reconString, testString, "BagArray simple reconstitution");
    }

    @Test
    public void testArrayWithChildren() {
        BagArray testArray = new BagArray ();
        testArray.add ("Bretton");
        testArray.add ("Wade");
        testArray.add (220.5);
        testArray.add (true);
        testArray.add (42);

        BagObject dateObject = new BagObject ();
        dateObject.put ("Year", 2015);
        dateObject.put ("Month", 11);
        dateObject.put ("Day", 18);

        testArray.insert (1, dateObject);
        String testString = testArray.toString ();
        AppTest.report (testString, testString, "BagArray complex toString exercise (" + testString + ")");

        BagArray reconArray = BagArrayFrom.string (testString);
        String reconString = reconArray.toString ();
        AppTest.report (reconString, testString, "BagArray complex reconstitution");

        AppTest.report (reconArray.getString (2), "Wade", "BagArray simple string extraction after insert");
        AppTest.report (reconArray.getBagObject (1).getInteger ("Year"), 2015, "BagArray complex bag/int extraction");

        // a couple of invalid retrievals
        AppTest.report (reconArray.getString (1), null, "BagArray simple invalid type extraction as String");
        AppTest.report (reconArray.getBagArray (1), null, "BagArray simple invalid type extraction as BagArray");

        // put a bag array in the bag array
        BagArray childArray = new BagArray ().add ("hello").add ("world");
        reconArray.replace (1, childArray);
        AppTest.report (reconArray.getBagArray (1), childArray, "BagArray store and retrieve a BagArray");
        AppTest.report (reconArray.getBagObject (1), null, "BagArray simple invalid type extraction as BagObject");
        reconString = reconArray.toString ();
        testArray = BagArrayFrom.string (reconString);
        AppTest.report (testArray.toString (), reconString, "BagArray reconstitute with an array containing an array");
    }

    @Test
    public void testRegressionCase() {
        try {
            File testFile = new File ("data", "UCS_Satellite_Database_2-1-14.json");
            BagArray bagArray = BagArrayFrom.file (testFile, MimeType.JSON);
            AppTest.report (bagArray != null, true, "BagArray - Regression Test 1");
            bagArray = BagArrayFrom.file (testFile);
            AppTest.report (bagArray != null, true, "BagArray - Regression Test 2");
            bagArray = BagArrayFrom.inputStream (new FileInputStream (testFile), MimeType.JSON);
            AppTest.report (bagArray != null, true, "BagArray - Regression Test 3");

            String string = bagArray.toString (MimeType.JSON);
            bagArray = BagArrayFrom.string (string, MimeType.JSON);
            AppTest.report (bagArray != null, true, "BagArray - Regression Test 4");
        } catch (Exception exception) {
            AppTest.report (false, true, "BagArray - Regression Test - Exception failure");
        }
    }

    @Test
    public void testEmptyArrayStrings() {
        BagArray bagArray = BagArrayFrom.string ("[]");
        AppTest.report (bagArray != null, true, "BagArray - test empty shell");
    }

    @Test
    public void testReconstructFromBogusStrings() {
        BagArray bagArray = BagArrayFrom.string ("[123 234 345 456]");
        AppTest.report (bagArray, null, "BagArray - test empty shell");
    }

    @Test
    public void testCopyOfEmptyArray () {
        BagArray empty = new BagArray ();
        BagArray copy = new BagArray (empty);
        AppTest.report (empty, copy, "Copy of empty array should succeed");
        copy.add ("test");
        AppTest.report (copy.getString (0), "test", "Copy of empty array should yield usable array");
    }

    @Test
    public void testKeyIndex () {
        BagObject bagObject = new BagObject ()
                .put ("a", new BagArray ()
                        .add (new BagObject ()
                                .put ("x", "y")
                        )
                );
        AppTest.report (bagObject.getString ("a/#first/x"), "y", "Hierarchical indexing of arrays using strings - 1");
        AppTest.report (bagObject.getString ("a/#last/x"), "y", "Hierarchical indexing of arrays using strings - 2");
        AppTest.report (bagObject.getString ("a/0/x"), "y", "Hierarchical indexing of arrays using strings - 3");
        AppTest.report (bagObject.getString ("a/100/x"), null, "Hierarchical indexing of arrays using strings - 4");
    }

    @Test
    public void testQueryAndSort () {
        try {
            File testFile = new File ("data", "UCS_Satellite_Database_2-1-14.json");
            BagArray bagArray = BagArrayFrom.file (testFile);
            BooleanExpr equality = Exprs.equality ("Country of Operator/Owner", "USA");
            SelectKey selectKey = new SelectKey (new BagObject ().put (SelectKey.TYPE_KEY, SelectType.INCLUDE.name ()).put (SelectKey.KEYS_KEY, new BagArray().add ("Current Official Name of Satellite").add ("Country of Operator")));
            BagArray queried = bagArray.query (equality, selectKey);
            AppTest.report (queried.getCount () > 0, true, "Queried Array returned some results");
            AppTest.report (queried.getString ("53/Country of Operator/Owner"), "USA", "Query results for #53 should match the query");

            // now try sorting
            SortKey[] sortKeys = SortKey.keys (new BagArray ().add (new BagObject ().put (SortKey.KEY, "Current Official Name of Satellite")));
            queried.sort (sortKeys);
            AppTest.report (queried.getString ("36/Country of Operator/Owner"), "USA", "Query results for #36 should match the query");
        } catch (Exception exception) {
            log.error (exception);
            exception.printStackTrace ();
            AppTest.report (true, false, "Query of array should succeed");
        }
    }

    @Test
    public void testQuery2 () {
        File testFile = new File ("data", "spark-applications.json");
        BagArray bagArray = BagArrayFrom.file (testFile);
        BagArray queried = bagArray.query (Exprs.equality ("attempts/#last/completed", true), null);
        AppTest.report (queried.getCount () > 0, true, "Verify good load from sample file with query");
    }

    @Test
    public void testSort () {
        BagArray bagArray = new BagArray ();

        int count = 20;

        for (int i = 0; i < count; ++i) {
            int random = (int) (Math.random () * 10 + 1);
            int random2 = (int) (Math.random () * 20 + 1);
            bagArray.add (new BagObject ().put ("id", random).put ("value", random2));
        }

        {
            SortKey[] sortKeys = SortKey.keys ("id", "value");
            BagArray sortedBagArray = new BagArray (bagArray).sort (sortKeys);

            BagObject lastBagObject = sortedBagArray.getBagObject (0);
            for (int i = 1; i < count; ++i) {
                BagObject nextBagObject = sortedBagArray.getBagObject (i);
                AppTest.report (nextBagObject.getString ("id").compareTo (lastBagObject.getString ("id")) >= 0, true, "sorted by id, alphabetic, ascending...");
                lastBagObject = nextBagObject;
            }
        }
        {
            SortKey[] sortKeys = SortKey.keys ("id", "value");
            sortKeys[0].setOrder (SortOrder.DESCENDING);
            BagArray sortedBagArray = new BagArray (bagArray).sort (sortKeys);

            BagObject lastBagObject = sortedBagArray.getBagObject (0);
            for (int i = 1; i < count; ++i) {
                BagObject nextBagObject = sortedBagArray.getBagObject (i);
                AppTest.report (nextBagObject.getString ("id").compareTo (lastBagObject.getString ("id")) <= 0, true, "sorted by id, alphabetic, descending...");
                lastBagObject = nextBagObject;
            }
        }
        {
            SortKey[] sortKeys = SortKey.keys ("id", "value");
            sortKeys[0].setType (SortType.NUMERIC);
            BagArray sortedBagArray = new BagArray (bagArray).sort (sortKeys);

            BagObject lastBagObject = sortedBagArray.getBagObject (0);
            for (int i = 1; i < count; ++i) {
                BagObject nextBagObject = sortedBagArray.getBagObject (i);
                AppTest.report (nextBagObject.getInteger ("id") >= lastBagObject.getInteger ("id"), true, "sorted by id, numeric, ascending...");
                lastBagObject = nextBagObject;
            }
        }
        {
            SortKey[] sortKeys = SortKey.keys ("id", "value");
            sortKeys[0].setOrder (SortOrder.DESCENDING).setType (SortType.NUMERIC);
            BagArray sortedBagArray = new BagArray (bagArray).sort (sortKeys);

            BagObject lastBagObject = sortedBagArray.getBagObject (0);
            for (int i = 1; i < count; ++i) {
                BagObject nextBagObject = sortedBagArray.getBagObject (i);
                AppTest.report (nextBagObject.getInteger ("id") <= lastBagObject.getInteger ("id"), true, "sorted by id, numeric, descending...");
                lastBagObject = nextBagObject;
            }
        }
    }

    @Test
    public void testSubset () {
        BagArray bagArray = new BagArray (97);
        Random random = new Random ();
        for (int i = 0; i < 97; ++i) {
            bagArray.add (random.nextInt (100));
        }
        bagArray.sort (null);

        int start = 0;
        int pageSize = 10;
        int pages = 0;
        BagArray subset = bagArray.subset (start, pageSize);
        while (subset.getCount () > 0) {
            ++pages;
            start += pageSize;
            AppTest.report (subset.getCount () > 0, true, "subset " + pages + " has " + subset.getCount () + " element(s)");
            subset = bagArray.subset (start, pageSize);
        }
        AppTest.report (true, true, "Got through a subset run in " + pages + " page(s)");
    }
}
