package com.arsoft.edcpassportreader.common;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ingenico.pclservice.PclService;
import com.regula.documentreader.api.nfc.IUniversalNfcTag;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EDCTag implements IUniversalNfcTag {

    PclService pclService;
    EDCTagListener listener;

    public interface EDCTagListener {
        void OnNotify(String status, String message);
    }

    public EDCTag(PclService pclService, EDCTagListener listener) {
        this.pclService = pclService;
        this.listener = listener;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public byte[] sendApduCommand(@NonNull byte[] apduCommands) {
        int i = 0;
        int commandLength = apduCommands.length;
        byte[] receive = new byte[1024];
        int[] receiveLen = new int[1];
        byte[] result = new byte[1];

        byte[] newCommand = prependByte(apduCommands, (byte) 0x31);

        if (pclService.sendMessage(newCommand, new int[newCommand.length])) {
            listener.OnNotify(null, String.format("Apdu command: %s", bytesToHex(newCommand)));
            if (pclService.flushMessages()) {
                Log.e("TEST", "Send APDU: " + bytesToHex(newCommand));
                while (true) {
                    pclService.receiveMessage(receive, receiveLen);
                    if (receiveLen[0] != 0) {
                        result = Arrays.copyOf(receive, receiveLen[0]);
                        if (getFirstByte(result) == (byte) 0x30) {
                            listener.OnNotify(null, String.format("receive msg=%s, len=%d", String.format("%02X", result[0]), receiveLen[0]));
                            break;
                        } else if (getFirstByte(result) == (byte) 0x31) {
                            listener.OnNotify(null, String.format("receive msg=%s, len=%d", String.format("%02X", result[0]), receiveLen[0]));
                            break;
                        } else if (getFirstByte(result) == (byte) 0x33) {
                            listener.OnNotify(null, String.format("receive msg=%s, len=%d", String.format("%02X", result[0]), receiveLen[0]));
                            break;
                        }
                    }
                }
            }
        }

        Log.e("TEST", "Response: " + bytesToHex(result));
        return removeFirstByte(result);
    }

    public static byte[] prependByte(byte[] array, byte value) {
        byte[] newArray = new byte[array.length + 1];
        newArray[0] = value;
        System.arraycopy(array, 0, newArray, 1, array.length);
        return newArray;
    }

    public static byte[] removeFirstByte(byte[] array) {
        if (array == null || array.length <= 1) {
            return new byte[0];
        }

        byte[] newArray = new byte[array.length - 1];
        System.arraycopy(array, 1, newArray, 0, newArray.length);
        return newArray;
    }

    public static byte getFirstByte(byte[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }
        return array[0];
    }

    @Override
    public int getTransceiveTimeout() {
        return 0;
    }

    @Override
    public void setTransceiveTimeout(int i) {

    }

    @Override
    public void connect() {

    }

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] byteArray = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }

        return byteArray;
    }
}
