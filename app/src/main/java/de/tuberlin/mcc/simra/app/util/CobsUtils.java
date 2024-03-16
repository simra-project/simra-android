package de.tuberlin.mcc.simra.app.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CobsUtils {

    public static final String TAG = "CobsUtils_LOG:";


    // Expected to be the entire packet to encode
    public static byte[] encode(byte[] packet) {
        if (packet == null
                || packet.length == 0) {
            return new byte[]{};
        }

        byte[] output = new byte[packet.length + 2];
        byte blockStartValue = 1;
        int lastZeroIndex = 0;
        int srcIndex = 0;
        int destIndex = 1;

        while (srcIndex < packet.length) {
            if (packet[srcIndex] == 0) {
                output[lastZeroIndex] = blockStartValue;
                lastZeroIndex = destIndex++;
                blockStartValue = 1;
            } else {
                output[destIndex++] = packet[srcIndex];
                if (++blockStartValue == 255) {
                    output[lastZeroIndex] = blockStartValue;
                    lastZeroIndex = destIndex++;
                    blockStartValue = 1;
                }
            }

            ++srcIndex;
        }

        output[lastZeroIndex] = blockStartValue;
        return output;
    }

    public static ArrayList<Byte> encode2(byte[] packet) {
        if (packet == null
                || packet.length == 0) {
            return new ArrayList<>();
        }

        ArrayList<Byte> output = new ArrayList<>();

        for (int i = 0; i < packet.length + 2; i++) {
            output.add((byte) 0);
        }
        byte blockStartValue = 1;
        int lastZeroIndex = 0;
        int srcIndex = 0;
        int destIndex = 1;

        while (srcIndex < packet.length) {
            if (packet[srcIndex] == 0) {
                output.set(lastZeroIndex,blockStartValue);
                lastZeroIndex = destIndex++;
                blockStartValue = 1;
            } else {
                output.set(destIndex++, packet[srcIndex]);
                if (++blockStartValue == 255) {
                    output.set(lastZeroIndex, blockStartValue);
                    lastZeroIndex = destIndex++;
                    blockStartValue = 1;
                }
            }

            ++srcIndex;
        }

        output.set(lastZeroIndex,blockStartValue);
        return output;
    }

    // Expected to be the entire packet to decode with trailing 0
    public static byte[] decode(byte[] packet) {
        // Log.d(TAG, "empty: " + (packet == null || packet.length == 0 || packet[packet.length - 1] != 0));
        if (packet == null
                || packet.length == 0
                || packet[packet.length - 1] != 0) {
            return new byte[]{};
        }
        // Log.d(TAG, "packet[packet.length - 1]: " + packet[packet.length - 1]);

        byte[] output = new byte[packet.length - 2];
        int srcPacketLength = packet.length - 1;
        int srcIndex = 0;
        int destIndex = 0;

        while (srcIndex < srcPacketLength) {
            int code = packet[srcIndex++] & 0xff;
            for (int i = 1; srcIndex < srcPacketLength && i < code; ++i) {
                output[destIndex++] = packet[srcIndex++];
            }
            if (code != 255 && srcIndex != srcPacketLength) {
                output[destIndex++] = 0;
            }
        }

        return output;
    }

    public static byte[] decode(LinkedList<Byte> packet) {
        // Log.d(TAG, "empty: " + (packet == null || packet.length == 0 || packet[packet.length - 1] != 0));
        if (packet == null
                || packet.isEmpty()
                || packet.get(packet.size() - 1) != 0) {
            return new byte[]{};
        }
        // Log.d(TAG, "packet[packet.length - 1]: " + packet[packet.length - 1]);

        byte[] output = new byte[packet.size() - 2];
        int srcPacketLength = packet.size() - 1;
        int srcIndex = 0;
        int destIndex = 0;

        while (srcIndex < srcPacketLength) {
            int code = packet.get(srcIndex++) & 0xff;
            for (int i = 1; srcIndex < srcPacketLength && i < code; ++i) {
                output[destIndex++] = packet.get(srcIndex++);
            }
            if (code != 255 && srcIndex != srcPacketLength) {
                output[destIndex++] = 0;
            }
        }

        return output;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

}
