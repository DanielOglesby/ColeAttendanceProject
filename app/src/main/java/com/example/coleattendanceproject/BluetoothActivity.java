package com.example.coleattendanceproject;

import static android.content.ContentValues.TAG;
import static android.provider.Telephony.Carriers.NAME;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    TextView mStatusBlueTv, mPairedTv;
    ImageView mBlueIv;
    Button mOnBtn, mOffBtn, mDiscoverBtn, mPairedBtn;

    BluetoothAdapter mBlueAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mStatusBlueTv = findViewById(R.id.statusBluetoothTv);
        mPairedTv = findViewById(R.id.pairedTv);
        mBlueIv = findViewById(R.id.bluetoothIv);
        mOnBtn = findViewById(R.id.onBtn);
        mOffBtn = findViewById(R.id.offBtn);
        mDiscoverBtn = findViewById(R.id.discoverableBtn);
        mPairedBtn = findViewById(R.id.pairedBtn);

        //adapter
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();

        //Check permissions for dangerous permissions
        //Bluetooth Advertise Permission
        if (ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, 2);
        }
        //Bluetooth Connect Permission
        if (ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
        }
        //Bluetooth Scan Permission
        if (ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
        }

        //Course Location Permissions
        if (ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
        }

        //check if bluetooth is available
        if (mBlueAdapter == null) {
            mStatusBlueTv.setText("Bluetooth is not available");
        } else {
            mStatusBlueTv.setText("Bluetooth is available");
        }

        //set image according to bluetooth status (on/off)
        if (mBlueAdapter.isEnabled()) {
            mBlueIv.setImageResource(R.drawable.ic_action_on);
        } else {
            mBlueIv.setImageResource(R.drawable.ic_action_off);
        }

        //on btn click
        mOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBlueAdapter.isEnabled()) {
                    showToast("Turning On Bluetooth...");
                    //intent to on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                } else {
                    showToast("Bluetooth is already on");
                }
            }
        });
        //discover bluetooth btn click
        mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBlueAdapter.isDiscovering()) {
                    showToast("Making Your Device Discoverable");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, REQUEST_DISCOVER_BT);
                }
            }
        });
        //off btn click
        mOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueAdapter.isEnabled()) {
                    mBlueAdapter.disable();
                    showToast("Turning Bluetooth Off");
                    mBlueIv.setImageResource(R.drawable.ic_action_off);
                } else {
                    showToast("Bluetooth is already off");
                }
            }
        });
        //get paired devices btn click
        mPairedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBlueAdapter.isEnabled()) {
                    mPairedTv.setText("Paired Devices");
                    Set<BluetoothDevice> devices = mBlueAdapter.getBondedDevices();
                    for (BluetoothDevice device : devices) {
                        mPairedTv.append("\nDevice" + device.getName() + "," + device);
                    }
                } else {
                    // bluetooth is off so can't get paired devices
                    showToast("Turn on bluetooth to get paired devices");
                }
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    //bluetooth is on
                    mBlueIv.setImageResource(R.drawable.ic_action_on);
                    showToast("Bluetooth is on");
                } else {
                    //user denied to turn bluetooth on
                    showToast("couldn't turn on bluetooth");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //toast message function
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private class connectionThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public connectionThread(BluetoothServerSocket serverSocket) {
            this.serverSocket = serverSocket;
            // Use a temporary object that is later assigned to serverSocket
            BluetoothServerSocket tmp = null;
            try {
                // APP_UUID is the app's UUID string
                BluetoothAdapter bluetoothAdapter = null;
                UUID APP_UUID = null;
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                BufferedInputStream serverSocket = null;
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

