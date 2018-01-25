package com.dreambandsdk;

/**
 * Created by seanf on 9/17/2017.
 */
import java.nio.ByteBuffer;

public class Utility {

    private static final String BLUETOOTH_SIG_UUID_BASE = "0000XXXX-0000-1000-8000-00805f9b34fb";
    private static final String HEX_CHARS="01234567890ABCDEF";

    public static boolean parseBoolean(String bool)
    {
        if (bool == null || bool.isEmpty())
            return false;
        if (bool.equalsIgnoreCase("YES") || bool.equalsIgnoreCase("1"))
            return true;
        return Boolean.parseBoolean(bool);
    }

    public static String normaliseUUID(String uuid) {
        String normalised_128_bit_uuid = uuid;
        if (uuid.length() == 4) {
            normalised_128_bit_uuid = BLUETOOTH_SIG_UUID_BASE.replace("XXXX",uuid);
        }
        if (uuid.length() == 32) {
            normalised_128_bit_uuid = uuid.substring(0,8) + "-"
                    + uuid.substring(8,12) + "-"
                    + uuid.substring(12,16) + "-"
                    + uuid.substring(16,20) + "-"
                    + uuid.substring(20,32);
        }
        return normalised_128_bit_uuid;
    }

    public static String extractCharacteristicUuidFromTag(String tag) {
        String uuid="";
        String[] parts = tag.split("_");
        if (parts.length == 4) {
            uuid = parts[3];
        }
        return uuid;
    }

    public static String extractServiceUuidFromTag(String tag) {
        String uuid="";
        String[] parts = tag.split("_");
        if (parts.length == 4) {
            uuid = parts[2];
        }
        return uuid;
    }

	public static byte[] getByteArrayFromHexString(String hex_string) {
     String hex = hex_string.replace(" ","");
     hex = hex.toUpperCase();
	
		byte[] bytes = new byte[hex.length() / 2];
		int i = 0;
		int j = 0;
		while (i < hex.length()) {
			String h1 = hex.substring(i, i + 1);
			String h2 = hex.substring(i + 1, i + 2);
			try {
				int b = (Integer.valueOf(h1, 16).intValue() * 16) + (Integer.valueOf(h2, 16).intValue());
				bytes[j++] = (byte) b;
				i = i + 2;
			} catch (NumberFormatException e) {
				System.out.println("NFE handling " + h1 + h2 + " with i=" + i);
				throw e;
			}
		}
		return bytes;
	}


	public static String byteArrayAsHexString(byte[] bytes) {
		if (bytes == null) {
			return "";
		}
		int l = bytes.length;
		StringBuffer hex = new StringBuffer();
		for (int i = 0; i < l; i++) {
			if ((bytes[i] >= 0) & (bytes[i] < 16))
				hex.append("0");
			hex.append(Integer.toString(bytes[i] & 0xff, 16).toUpperCase());
		}
		return hex.toString();
	}

    public static boolean isValidHex(String hex_string) {
        System.out.println("isValidHex("+hex_string+")");
        String hex = hex_string.replace(" ","");
        hex = hex.toUpperCase();
        int len = hex.length();
        int remainder = len % 2;
        if (remainder != 0) {
            System.out.println("isValidHex: not even number of chars");
            return false;
        }
        for (int i=0;i<len;i++) {
            if (!HEX_CHARS.contains(hex.substring(i,i+1))) {
                return false;
            }
        }
        return true;
    }

    public static String bytebufferToString(ByteBuffer buf, int length) {
        int i = 0, stringLen = 0;
        // Parse a null-terminated String from ByteBuffer
        StringBuilder sb = new StringBuilder();
        while (buf.remaining() > 0) {
            char c = (char)buf.get();
            sb.append(c);
            i++;
            if (c == '\0')
                stringLen = i;
            if (i >= length) break;
        }

        return sb.toString();
    }

    public static String bytebufferToString(ByteBuffer buf, int index, int length) {
        int i = 0, stringLen = 0;
        // Parse a null-terminated String from ByteBuffer
        StringBuilder sb = new StringBuilder();
        while (buf.remaining() > 0) {
            char c = (char)buf.get(index+i);
            sb.append(c);
            i++;
            if (c == '\0')
                stringLen = i;
            if (i >= length) break;
        }

        return sb.toString();
    }


    public static short bytebufferToShort(ByteBuffer buf) {
        int i = 0;
        int startPos = buf.position();
        // Parse a null-terminated String from ByteBuffer
        byte[] value = new byte[2];
        while (buf.remaining() > 0) {
            value[i++] = buf.get();
            if (i >= 2) break;
        }

        return  ((short)((value[1] << 8) | (value[0] & 0xFF)));
    }
    
    public static short bytebufferToInt(ByteBuffer buf) {
        int i = 0;
        int startPos = buf.position();
        // Parse a null-terminated String from ByteBuffer
        byte[] value = new byte[4];
        while (buf.remaining() > 0) {
            value[i++] = buf.get();
            if (i >= 4) break;
        }

        return  ((short)((value[3] << 24) | (value[2] << 16) | (value[1] << 8) | (value[0] & 0xFF)));
    }

    public static long getUnsignedInt32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset+1] & 0xFF) << 16) | ((bytes[offset+2] & 0xFF) << 8) | (bytes[offset+3] & 0xFF);
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

    public static void writeStringPadded(ByteBuffer buffer, String value, int len) {
        if (value == null) {
            value = "";
        }

        byte[] stringBytes = value.getBytes();

        int bytesToWrite = stringBytes.length < len - 1 ? stringBytes.length : len - 1;
        buffer.put(stringBytes, 0, bytesToWrite);
        int padLen = len - bytesToWrite;
        for(int i = 0 ; i < padLen ; i++) {
            buffer.put((byte)0);
        }
    }
}
