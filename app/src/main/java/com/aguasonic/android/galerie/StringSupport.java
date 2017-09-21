/*
 * ©2015 Aguasonic Acoustics
 * http://aguasonic.com/
 */

package com.aguasonic.android.galerie;

/**
 * Methods support string operations.
 * Declaring as 'enum' is the best way to build a Singleton.
 */
public enum StringSupport {
    //- Never used -- but <must> declare at least one symbol to be an enumerated type.
    SINGLETON_INSTANCE;
    private final static String DOUBLE_QUOTE = "\"";

    static final protected String addDoubleQuotes(final String str) {

        return (DOUBLE_QUOTE + str + DOUBLE_QUOTE);
    }

    static final protected String stripLeadingAndTrailingQuotes(String the_str) {

        if (the_str.startsWith(DOUBLE_QUOTE)) {
            the_str = the_str.substring(1, the_str.length());
        }

        if (the_str.endsWith(DOUBLE_QUOTE)) {
            the_str = the_str.substring(0, the_str.length() - 1);
        }

        return the_str;
    }

    //- Decode a hexadecimal character string. _No checks_, so know what you're doing!
    static final protected byte[] decodeHex(final String invec) {
        final byte[] char_bytes = invec.getBytes();
        final int len = invec.length();
        final byte[] byte_sequence = new byte[len >> 1];

        // two characters form the hex value.
        for (short idx1 = 0, idx2 = 0; idx2 < len; idx1++) {
            final char char_1 = (char) char_bytes[idx2++];
            final char char_2 = (char) char_bytes[idx2++];
            int this_byte = Character.digit(char_1, 16) << 4;

            this_byte = this_byte | Character.digit(char_2, 16);

            byte_sequence[idx1] = (byte) (this_byte & 0xFF);
        }

        return byte_sequence;
    }


    //- Encodes byte array as hexadecimal sequence.
    static final protected String encodeHex(final byte[] data) {
        /**
         * Used to build output as Hex
         */
        final char[] toDigits =
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        final int the_len = data.length;
        final char[] char_sequence = new char[the_len << 1];

        //- two characters form the hex value.
        for (int idx1 = 0, idx2 = 0; idx1 < the_len; idx1++) {
            char_sequence[idx2++] = toDigits[(0xF0 & data[idx1]) >>> 4];
            char_sequence[idx2++] = toDigits[0x0F & data[idx1]];
        }

        return (new String(char_sequence));
    }
}

/*
 * ©2015 Aguasonic Acoustics
 * http://aguasonic.com/
 */
