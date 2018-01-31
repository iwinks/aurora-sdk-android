package com.dreambandsdk;

import java.util.zip.CRC32;

public class Utility {


    public static long getUnsignedInt32(byte[] bytes, int offset) {

        return (bytes[offset] & 0xFF) | ((bytes[offset+1] & 0xFF) << 8) | ((bytes[offset+2] & 0xFF) << 16) | ((bytes[offset+3] & 0xFF)<< 24);
    }

    public static String getCamelCasedString(String string){

        StringBuilder stringBuilder = new StringBuilder(string.trim().toLowerCase());

        for (int i = 0; i < stringBuilder.length(); i++) {

            if (stringBuilder.charAt(i) == ' ') {

                stringBuilder.deleteCharAt(i);

                stringBuilder.replace(i, i+1, String.valueOf(Character.toUpperCase(stringBuilder.charAt(i))));
            }
        }

        return stringBuilder.toString();
    }

    public static long getCrc(byte[] data){

        CRC32 crc = new CRC32();

        crc.update(data, 0, data.length);

        return crc.getValue();
    }
}
