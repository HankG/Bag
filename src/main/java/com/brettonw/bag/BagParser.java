package com.brettonw.bag;

// The BagParser is loosely modeled after a JSON parser grammar from the site (http://www.json.org).
// The main difference is that we ignore differences between value types (all of them will be
// strings internally), and assume the input is a well formed string representation of a BagObject
// or BagArray in JSON-ish format

import java.io.*;

class BagParser {
    private int index;
    private final String input;

    private String readInputStream (InputStream inputStream) throws IOException {
        InputStreamReader   inputStreamReader = new InputStreamReader (inputStream);
        BufferedReader bufferedReader = new BufferedReader (inputStreamReader);
        StringBuilder stringBuilder = new StringBuilder ();
        String line;
        while ((line = bufferedReader.readLine ()) != null) {
            stringBuilder.append (line);
        }
        bufferedReader.close ();
        return stringBuilder.toString ();
    }

    BagParser(String input) {
        this.input = input;
        index = 0;
    }

    public BagParser(InputStream inputStream) throws IOException {
        input = readInputStream (inputStream);
        index = 0;
    }

    public BagParser(File file) throws IOException {
        InputStream inputStream = new FileInputStream (file);
        input = readInputStream (inputStream);
        index = 0;
    }

    BagArray ReadBagArray() {
        // <Array> :: [ ] | [ <Elements> ]
        BagArray bagArray = new BagArray();
        return (Expect('[') && ReadElements(bagArray) && Expect(']')) ? bagArray : null;
    }

    BagObject ReadBagObject() {
        // <Object> ::= { } | { <Members> }
        BagObject bagObject = new BagObject();
        return (Expect('{') && ReadMembers(bagObject, true) && Expect('}')) ? bagObject : null;
    }

    private void consumeWhiteSpace () {
        // consume white space (space, carriage return, tab, etc.
        while (Character.isWhitespace (input.charAt (index))) {
            ++index;
        }
    }

    private boolean Expect(char c) {
        consumeWhiteSpace ();

        // the next character should be the one we expect
        if (input.charAt (index) == c) {
            ++index;
            return true;
        }
        return false;
    }

    private boolean ReadElements(BagArray bagArray) {
        // <Elements> ::= <Value> | <Value> , <Elements>
        Object value = ReadValue();
        if (value != null) {
            // special case for "null"
            if ((value instanceof String) && (((String) value).equalsIgnoreCase ("null"))) {
                value = null;
            }
            bagArray.add (value);
        }
        //noinspection PointlessBooleanExpression
        return (Expect(',') && ReadElements(bagArray)) || true;
    }

    private boolean ReadMembers(BagObject bagObject, boolean first) {
        // <Members> ::= <Pair> | <Pair> , <Members>
        return ReadPair (bagObject) ? (Expect (',') ? ReadMembers (bagObject, false) : true) : first;
    }

    private boolean ReadPair(BagObject bagObject) {
        // <Pair> ::= <String> : <Value>
        String key = ReadString();
        if ((key != null) && (key.length () > 0) && Expect(':')) {
            Object value = ReadValue();
            if (value != null) {
                // special case for "null"
                if (!((value instanceof String) && (((String) value).equalsIgnoreCase ("null")))) {
                    bagObject.put (key, value);
                }
                return true;
            }
        }

        // this will only happen if we are reconstructing from invalid source
        return false;
    }

    private boolean isAllowedBareValue (char c) {
        return (Character.isLetterOrDigit (c)) || (".+=_$".indexOf (c) >= 0);
    }

    private String ReadString() {
        // " chars " | <chars>
        String result = null;
        if (Expect('"')) {
            int start = index;
            char c = input.charAt(index);
            while (c != '"') {
                // using the escape mechanism is like a free pass for the next character, but we
                // don't do any transformation on the substring, just return it as written
                if (c == '\\') {
                    ++index;
                }
                c = input.charAt(++index);
            }
            result = input.substring (start, index++);
        } else {
            // technically, we're being sloppy allowing bare values where quoted strings are
            // expected, but it's part of the simplified structure we support. This allows us to
            // read valid JSON files without handling every single case.
            int start = index;
            char c = input.charAt (index);
            while (isAllowedBareValue (c)) {
                c = input.charAt (++index);
            }

            // capture the result if we actually consumed some characters
            if (index > start) {
                result = input.substring (start, index);
            }
        }
        return result;
    }

    private Object ReadValue() {
        // <Value> ::= <String> | <Object> | <Array>
        consumeWhiteSpace ();

        Object value = null;
        switch (input.charAt (index)) {
            case '{':
                value = ReadBagObject();
                break;

            case '[':
                value = ReadBagArray();
                break;

            case '"':
            default:
                value = ReadString();
                break;
        }
        return value;
    }
}
