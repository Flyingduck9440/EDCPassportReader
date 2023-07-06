package com.arsoft.edcpassportreader.common;

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

    @Override
    public byte[] sendApduCommand(@NonNull byte[] apduCommands) {
        int i = 0;
        int commandLength = apduCommands.length;
        byte[] receive = new byte[1024];
        int[] receiveLen = new int[1];
        byte[] result = new byte[1];

        if (pclService.sendMessage(apduCommands, new int[commandLength])) {
            listener.OnNotify(null, String.format("Apdu command: %s", bytesToHex(apduCommands)));
            if (pclService.flushMessages()) {
                do {
                    try {
                        pclService.receiveMessage(receive, receiveLen);
                        if (receiveLen[0] != 0) {
                            result = Arrays.copyOf(receive, receiveLen[0]);
                            listener.OnNotify(null, String.format("receive msg=%s, len=%d", new String(result, StandardCharsets.UTF_8), receiveLen[0]));
                            break;
                        } else {
                            listener.OnNotify(null, "Response (byte): 0000");
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    i++;
                } while (i < 5);
            }
        }
        return result;
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

    private final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
