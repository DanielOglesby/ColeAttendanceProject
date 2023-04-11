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

public class ConnectThread extends AsyncTask<Void, Void, Boolean> {
    //Variables
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private UUID myUUID;
    private Boolean status = false;

    public ConnectThread(BluetoothDevice device, UUID uuid) {
        mDevice = device;
        myUUID = uuid;
        onPostExecute(doInBackground());
    }

    @SuppressLint("MissingPermission")
    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(myUUID);
            mSocket.connect();
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            // Connection successful
            Log.d("BT", "Connection successful!");

            // If successfully connected, do IO in separate thread.
            new IOThread(mSocket).start();
            status = true;
        } else {
            // Connection failed
            Log.e("BT", "Connection failed");
            status = false;
            try {
                mSocket.close();
            }
            catch (IOException e) {
                Log.e("SOCKET", "Failed to close socket after failed connection");
            }
        }
    }

    public Boolean getConnectionStatus() {
        return status;
    }
}
