package com.example.coleattendanceproject;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOThread extends Thread {
    //Variables
    private final BluetoothSocket mSocket;
    private final InputStream iStream;
    private final OutputStream oStream;

    public IOThread(BluetoothSocket socket) {
        //Getting input/output from connection
        mSocket = socket;
        InputStream testIn = null;
        OutputStream testOut = null;
        try{
            testIn = mSocket.getInputStream();
            testOut = mSocket.getOutputStream();
        }
        catch(IOException e) {
            Log.e("BT", "No input OR output");
        }

        iStream = testIn;
        oStream = testOut;
    }

    @Override
    public void run() {
        //Request Attendance Sheet
        getAttendance("*ID*");
    }

    private void getAttendance(String message) {
        byte[] buffer = message.getBytes();
        try {
            oStream.write(buffer);
            oStream.flush();

            byte[] received = new byte[8192];
            int responseBytes = iStream.read(received);
            String response = new String(received, 0, responseBytes);
            Log.d("IO", "Response received: " + response);

        }
        catch (IOException e) {
            Log.e("IO", "Error sending/receiving message");
        }
    }

    public void cancel() {
        try {
            mSocket.close();
            iStream.close();
            oStream.close();
        }
        catch(IOException e) {
            Log.e("BT", "Socket could not be closed.");
        }
    }
}
