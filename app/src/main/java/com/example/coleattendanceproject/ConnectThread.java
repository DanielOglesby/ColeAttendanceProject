package com.example.coleattendanceproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    //Variables
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private UUID myUUID;
    private Boolean status = false;
    IOThread myThread;

    public ConnectThread(BluetoothDevice device, UUID uuid) {
        mDevice = device;
        myUUID = uuid;
    }


    // combine next two functions into run function
    // should be threaded
    @SuppressLint("MissingPermission")
    public void run() {
        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(myUUID);
            mSocket.connect();
            // Connection successful
            Log.d("BT", "Connection successful!");

            // If successfully connected, do IO in separate thread.
            IOThread ioThread = new IOThread(mSocket);
            myThread = ioThread;
            myThread.start();
            status = true;
        } catch (IOException e) {
            // Connection failed
            Log.e("BT", "Connection failed");
            status = false;
            try {
                mSocket.close();
            }
            catch (IOException a) {
                Log.e("SOCKET", "Failed to close socket after failed connection");
            }
        }
    }


    public void write(String scanner) {
        myThread.write(scanner);
    }
    public Boolean getConnectionStatus() {
        return status;
    }
}
