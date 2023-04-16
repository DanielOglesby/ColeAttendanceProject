package com.example.coleattendanceproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class ConnectThread extends Thread {
    //Variables
    private ArrayList<BluetoothDevice> mDeviceList;
    private BluetoothSocket mSocket;
    private UUID myUUID;
    private IOThread myThread;
    //Status codes for handler
    private static final int CONNECTED = 1;
    private static final int FAILED = 2;
    private Handler mHandler;

    public ConnectThread(ArrayList<BluetoothDevice> device, UUID uuid, Handler handler) {
        mDeviceList = device;
        myUUID = uuid;
        mHandler = handler;
    }


    // combine next two functions into run function
    // should be threaded
    @SuppressLint("MissingPermission")
    public void run() {
        for(BluetoothDevice mDevice : mDeviceList) {
            if (mDevice.getName() != null) {
                Log.d("DEVICE", "Attempting to connect to device: " + mDevice.getName());
                try {
                    mSocket = mDevice.createRfcommSocketToServiceRecord(myUUID);
                    mSocket.connect();
                    // Connection successful
                    mHandler.sendEmptyMessage(CONNECTED);
                    // If successfully connected, do IO in separate thread.
                    myThread = new IOThread(mSocket);
                    myThread.start();
                    break;
                } catch (IOException e) {
                    // Connection failed
                    mHandler.sendEmptyMessage(FAILED);
                    try {
                        mSocket.close();
                    } catch (IOException a) {
                        Log.e("SOCKET", "Failed to close socket after failed connection");
                    }
                }
            }
        }
    }

    //Get attendance from Attend.exe (TODO: return string?)
    public void getAttendance() {
        this.write("*ID*");
    }

    //Write to Attend.exe
    public void write(String scanner) {
        myThread.write(scanner);
    }
}
