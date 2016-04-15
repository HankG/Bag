package com.brettonw.bag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * A collection of text-based values store in key/value pairs (maintained in a sorted array).
 */
public class BagObject {
    private static final Logger log = LogManager.getLogger (BagObject.class);

    private static final int DEFAULT_CONTAINER_SIZE = 1;
    private static final int DOUBLING_CAP = 16;
    private static final String PATH_SEPARATOR = "/";

    private Pair[] container;
    private int count;


    /**
     * Create a new BagObject with a default underlying storage size.
     */
    public BagObject () {
        init (DEFAULT_CONTAINER_SIZE);
    }

    /**
     * Create a new BagObject with hint for the underlying storage size.
     *
     * @param size The expected number of elements in the BagObject, treated as a hint to optimize
     *             memory allocation. If additional elements are stored, the BagObject will revert
     *             to normal allocation behavior.
     */
    public BagObject (int size) {
        init (size);
    }

    private void init (int containerSize) {
        count = 0;
        container = new Pair[containerSize];
    }

    /**
     * Return the number of elements stored in the BagObject.
     *
     * @return the count of elements in the underlying store. This is distinct from the capacity of
     * the underlying store.
     */
    public int getCount () {
        return count;
    }

    private void grow (int gapIndex) {
        Pair src[] = container;
        if (count == container.length) {
            // if the array is smaller than the cap then double its size, otherwise just add the block
            int newSize = (count > DOUBLING_CAP) ? (count + DOUBLING_CAP) : (count * 2);
            container = new Pair[newSize];
            System.arraycopy (src, 0, container, 0, gapIndex);
        }
        System.arraycopy (src, gapIndex, container, gapIndex + 1, count - gapIndex);
        ++count;
    }

    private int binarySearch (String key) {
        // XXX it would be nice if this didn't need to create a new object to conduct this search
        Pair term = new Pair (key);
        return Arrays.binarySearch (container, 0, count, term);
    }

    private Pair getOrAddPair (String key) {
        // conduct a binary search for where the pair should be
        int index = binarySearch (key);
        if (index < 0) {
            // the binary search returns a funky encoding of the index where the new value
            // should go when it's not there, so we have to decode that number (-index - 1)
            index = -(index + 1);

            // make sure there is room in the underlying container, then store a new (empty) Pair
            grow (index);
            container[index] = new Pair (key);
        }
        return container[index];
    }

    /**
     * Return an object stored at the requested key value.
     * <p>
     * Using a binary search of the underlying store, finds where the first component of the path
     * should be and returns it.
     *
     * @param key A string value used to index the element, using "/" as separators, for example:
     *             "com/brettonw/bag/key".
     * @return The indexed element (if found), or null
     */
    public Object getObject (String key) {
        String path[] = key.split (PATH_SEPARATOR, 2);
        int index = binarySearch (path[0]);
        if (index >= 0) {
            Pair pair = container[index];
            Object found = pair.getValue ();
            return (path.length == 1) ? found : ((BagObject) found).getObject (path[1]);
        }
        return null;
    }

    /**
     * Store an object at the requested key value. The key may be a simple name, or it may be a path
     * (with keys separated by "/") to create a hierarchical "bag-of-bags" that is indexed
     * recursively.
     * <p>
     * Using a binary search of the underlying store, finds where the first component of the path
     * should be. If it does not already exist, it is created (recursively in the case of a path),
     * and the underlying store is shifted to make a space for it. The shift might cause the
     * underlying store to be resized if there is insufficient room.
     * <p>
     * Note that null values for the element are NOT stored at the leaf of the tree denoted by
     * a path, as returning null from getObject would be indistinguishable from a call to getObject
     * with an unknown key. This check is performed before the tree traversal, so the underlying
     * store will NOT contain the path after an attempt to add a null value.
     *
     * @param key A string value used to index the element, using "/" as separators, for example:
     *             "com/brettonw/bag/key".
     * @param object The element to store.
     * @return The BagObject, so that operations can be chained together.
     */
    public BagObject put (String key, Object object) {
        object = BagHelper.objectify (object);
        if (object != null) {
            String path[] = key.split (PATH_SEPARATOR, 2);
            if (path.length == 1) {
                Pair pair = getOrAddPair (path[0]);
                pair.setValue (object);
            } else {
                Pair pair = getOrAddPair (path[0]);
                BagObject bagObject = (BagObject) pair.getValue ();
                if (bagObject == null) {
                    pair.setValue (bagObject = new BagObject ());
                }
                bagObject.put (path[1], object);
            }
        }
        return this;
    }

    /**
     * Add an object to a BagArray stored at the requested key. Using a binary search of the
     * underlying store, finds where the BagArray mapped to the key should be. If the BagArray does
     * not already exist, it is created, and the underlying store is shifted to make a space for it.
     * The shift might cause the underlying store to be resized if there is insufficient room.
     * <p>
     * Note that null values for the BagArray ARE stored per the design decision for arrays.
     *
     * @param key A string value used to index the element, using "/" as separators, for example:
     *             "com/brettonw/bag/key".
     * @param object The element to store.
     * @return The BagObject, so that operations can be chained together.
     */
    public BagObject add (String key, Object object) {
        String path[] = key.split (PATH_SEPARATOR, 2);
        Pair pair = getOrAddPair (path[0]);
        if (path.length == 1) {
            BagArray bagArray = (BagArray) pair.getValue ();
            if (bagArray == null) {
                pair.setValue (bagArray = new BagArray ());
            }
            bagArray.add (BagHelper.objectify (object));
        } else {
            BagObject bagObject = (BagObject) pair.getValue ();
            if (bagObject == null) {
                pair.setValue (bagObject = new BagObject ());
            }
            bagObject.add (path[1], object);
        }
        return this;
    }

    /**
     * Using a binary search of the underlying store, finds where the element mapped to the key
     * should be, and removes it. If the element doesn't exist, nothing happens. If
     * the element is removed, the underlying store is shifted to close the space where it was.
     * removing elements will never cause the underlying store to shrink.
     *
     * @param key A string value used to index the element, using "/" as separators, for example:
     *             "com/brettonw/bag/key".
     * @return The BagObject, so that operations can be chained together.
     */
    public BagObject remove (String key) {
        String path[] = key.split (PATH_SEPARATOR, 2);
        int index = binarySearch (path[0]);
        if (index >= 0) {
            if (path.length == 1) {
                int gapIndex = index + 1;
                System.arraycopy (container, gapIndex, container, index, count - gapIndex);
                --count;
            } else {
                BagObject found = (BagObject) container[index].getValue ();
                found.remove (path[1]);
            }
        }
        return this;
    }

    /**
     * Retrieve a mapped element and return it as a String.
     *
     * @param key A string value used to index the element.
     * @return The element as a string, or null if the element is not found (or not a String).
     */
    public String getString (String key) {
        Object object = getObject (key);
        try {
            return (String) object;
        } catch (ClassCastException exception) {
            log.warn ("Cannot cast value type (" + object.getClass ().getName () + ") to String for key (" + key + ")");
        }
        return null;
    }

    /**
     * Retrieve a mapped element and return it as a Boolean.
     *
     * @param key A string value used to index the element.
     * @return The element as a Boolean, or null if the element is not found.
     */
    public Boolean getBoolean (String key) {
        String string = getString (key);
        return (string != null) ? Boolean.parseBoolean (string) : null;
    }

    /**
     * Retrieve a mapped element and return it as a Long.
     *
     * @param key A string value used to index the element.
     * @return The element as a Long, or null if the element is not found.
     */
    @SuppressWarnings ("WeakerAccess")
    public Long getLong (String key) {
        String string = getString (key);
        return (string != null) ? Long.parseLong (string) : null;
    }

    /**
     * Retrieve a mapped element and return it as an Integer.
     *
     * @param key A string value used to index the element.
     * @return The element as an Integer, or null if the element is not found.
     */
    public Integer getInteger (String key) {
        Long value = getLong (key);
        return (value != null) ? value.intValue () : null;
    }

    /**
     * Retrieve a mapped element and return it as a Double.
     *
     * @param key A string value used to index the element.
     * @return The element as a Double, or null if the element is not found.
     */
    public Double getDouble (String key) {
        String string = getString (key);
        return (string != null) ? Double.parseDouble (string) : null;
    }

    /**
     * Retrieve a mapped element and return it as a Float.
     *
     * @param key A string value used to index the element.
     * @return The element as a Float, or null if the element is not found.
     */
    public Float getFloat (String key) {
        Double value = getDouble (key);
        return (value != null) ? value.floatValue () : null;
    }

    /**
     * Retrieve a mapped element and return it as a BagObject.
     *
     * @param key A string value used to index the element.
     * @return The element as a BagObject, or null if the element is not found.
     */
    public BagObject getBagObject (String key) {
        Object object = getObject (key);
        try {
            return (BagObject) object;
        } catch (ClassCastException exception) {
            log.warn ("Cannot cast value type (" + object.getClass ().getName () + ") to BagObject for key (" + key + ")");
        }
        return null;
    }

    /**
     * Retrieve a mapped element and return it as a BagArray.
     *
     * @param key A string value used to index the element.
     * @return The element as a BagArray, or null if the element is not found.
     */
    public BagArray getBagArray (String key) {
        Object object = getObject (key);
        try {
            return (BagArray) object;
        } catch (ClassCastException exception) {
            log.warn ("Cannot cast value type (" + object.getClass ().getName () + ") to BagArray for key (" + key + ")");
        }
        return null;
    }

    /**
     * Return whether or not the requested key or path is present in the BagObject or hierarchical
     * "bag-of-bags"
     *
     * @param key A string value used to index the element, using "/" as separators, for example:
     *             "com/brettonw/bag/key".
     * @return A boolean value, true if the key is present in the underlying store. Note that null
     * values are not stored (design decision), so this equivalent to checking for null.
     */
    public boolean has (String key) {
        String path[] = key.split (PATH_SEPARATOR, 2);
        int index = binarySearch (path[0]);
        try {
            return (index >= 0) &&
                    ((path.length == 1) ||
                            ((BagObject) container[index].getValue ()).has (path[1]));
        } catch (ClassCastException classCastException) {
            // if a requested value is not a BagObject - this should be an exceptional case
            return false;
        }
    }

    /**
     * Returns an array of the keys contained in the underlying container. it does not enumerate the
     * container and all of its children.
     *
     * @return The keys in the underlying map as an array of Strings.
     */
    public String[] keys () {
        String keys[] = new String[count];
        for (int i = 0; i < count; ++i) {
            keys[i] = container[i].getKey ();
        }
        return keys;
    }

    /**
     * Returns the BagObject represented as JSON.
     *
     * @return A String containing the JSON representation of the underlying store.
     */
    @Override
    public String toString () {
        StringBuilder result = new StringBuilder ();
        boolean isFirst = true;
        for (int i = 0; i < count; ++i) {
            result.append (isFirst ? "" : ",");
            isFirst = false;

            Pair pair = container[i];
            result
                    .append (BagHelper.quote (pair.getKey ()))
                    .append (":")
                    .append (BagHelper.stringify (pair.getValue ()));
        }
        return BagHelper.enclose (result.toString (), "{}");
    }

    /**
     * Returns a BagObject extracted from a JSON representation.
     *
     * @param  input A String containing a JSON encoding of a BagObject.
     * @return A new BagObject containing the elements encoded in the input.
     */
    public static BagObject fromString (String input) {
        // parse the string out... it is assumed to be a well formed BagObject serialization
        BagParser parser = new BagParser (input);
        return parser.ReadBagObject ();
    }

    /**
     * Returns a BagObject extracted from a JSON representation.
     *
     * @param  inputStream An InputStream containing a JSON encoding of a BagObject.
     * @return A new BagObject containing the elements encoded in the input.
     */
    public static BagObject fromStream (InputStream inputStream) throws IOException {
        BagParser parser = new BagParser (inputStream);
        return parser.ReadBagObject ();
    }

    /**
     * Returns a BagObject extracted from a JSON representation.
     *
     * @param  file A File containing a JSON encoding of a BagObject.
     * @return A new BagObject containing the elements encoded in the input.
     */
    public static BagObject fromFile (File file) throws IOException {
        BagParser parser = new BagParser (file);
        return parser.ReadBagObject ();
    }

}
