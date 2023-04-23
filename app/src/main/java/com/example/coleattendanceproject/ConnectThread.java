package com.example.coleattendanceproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class ConnectThread extends Thread{
    //Variables
    private final ArrayList<BluetoothDevice> mDeviceList;
    private BluetoothSocket mSocket;
    private final UUID myUUID;
    private IOThread myThread;
    //Status codes for handler
    private static final int CONNECTED = 1;
    private static final int FAILED = 2;
    private static final int FINISHED = 3;
    private final Handler mHandler;
    //Used to stop thread in case of no connection being made.
    private volatile boolean running = true;

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
                    //Add device as a bonded device
                    BluetoothDevice connectedDevice = mSocket.getRemoteDevice();
                    try {
                        Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                        createBondMethod.invoke(connectedDevice);
                    }
                    catch(Exception e) {
                        Log.e("CONNECT", "Failed to add the device as a paired device: " + e.getMessage());
                    }
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
        while(running){}
        mHandler.sendEmptyMessage(FINISHED);
        mDeviceList.clear();
    }

    //Get attendance from Attend.exe (TODO: return string?)
    public void getAttendance() {myThread.getAttendance();}

    //Get incoming messages
    public String getMessages() {return myThread.getMessages();}

    //Write to Attend.exe
    public void write(String scanner) {myThread.write(scanner);}
    //Close all sockets and streams as well as close thread
    public void stopThread() {
        try {
            myThread.stopThread();
        }
        catch (NullPointerException e) {
            Log.e("CONNECT", "No existing IOThread myThread.");
        }
        try {
            mSocket.close();
        }
        catch(IOException e) {
            Log.e("CONNECT", "Socket could not be closed.");
        }
        running = false;
    }
}
