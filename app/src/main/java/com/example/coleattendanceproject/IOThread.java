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

    public void run() {
        //Buffer for stream
        byte[] buffer = new byte[1024];

        //Bytes returned
        int bytes;

        while (true) {
            try {
                bytes = iStream.read(buffer);
                String message = new String (buffer, 0, bytes);
                Log.d("IO", "Incoming message: " + message);

            } catch (IOException e) {
                Log.e("IO", "Error receiving message");
                break;
            }
        }
    }

    //Write to Attend.exe
    public void write(String scanner) {
        byte[] buffer = scanner.getBytes();
        try {
            oStream.write(buffer);
            oStream.flush();
        }
        catch (IOException e) {
            Log.e("IO", "Error sending/receiving message");
        }
    }

    //Get attendance sheet from Attend.exe (TODO: still needs to return string)
    private void getAttendance() {
        this.write("*ID*");
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
