package com.example.coleattendanceproject;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOThread extends Thread {
    //Variables
    private final BluetoothSocket mSocket;
    private final InputStream iStream;
    private final OutputStream oStream;
    private StringBuilder incomingMessages = new StringBuilder();
    //Used to stop thread in case of no connection being made.
    private volatile boolean running = true;

    private boolean messageReceived = false;

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

        while (running) {
            try {
                bytes = iStream.read(buffer);
                String message = new String (buffer, 0, bytes);
                Log.d("IO", "Incoming message: " + message);
                incomingMessages.append(message);
                if (message.endsWith("\n")){
                    messageReceived = true;
                }
            } catch (IOException e) {
                Log.e("IO", "Error receiving message");
                break;
            }
        }
    }

    //Write to Attend.exe
    public String write(String scanner) {
        byte[] buffer = scanner.getBytes();
        try {
            oStream.write(buffer);
            oStream.flush();
        }
        catch (IOException e) {
            Log.e("IO", "Error sending message");
        }
        return scanner;
    }

    //Get attendance sheet from Attend.exe
    public String getAttendance() {
        incomingMessages.setLength(0);      //Clear any previous messages
        messageReceived = false;

        this.write("*ID*");
        while (!messageReceived) {
            try {
                Thread.sleep(100); // Sleep for a short time to avoid busy waiting
            } catch (InterruptedException e) {
                Log.e("IO", "Thread interrupted while waiting for message");
            }
        }
        //Log.d("IO", "String of IDs: " + incomingMessages.toString());
        return incomingMessages.toString();


    }

    //Get incoming messages
    public String getMessages() {return incomingMessages.toString();}

    //Close all sockets and streams
    public void stopThread() {
        try {
            mSocket.close();
            iStream.close();
            oStream.close();
        }
        catch(IOException e) {
            Log.e("BT", "Socket could not be closed.");
        }
        running = false;
    }
}
