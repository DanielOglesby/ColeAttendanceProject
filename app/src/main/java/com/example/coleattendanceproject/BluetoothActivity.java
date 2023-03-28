package com.example.coleattendanceproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 0;

    TextView mStatusBlueTv, mPairedTv, mUUID;
    ImageView mBlueIv;
    Button mOnBtn, mConnectUUID, mChangeUUID;

    BluetoothAdapter mBlueAdapter;

    //Used to connect to desktop attendance app
    private UUID uuid = UUID.fromString("e0cbf06c-cd8b-4647-bb8a-263b43f0f974");

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 2)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied by user
                if(ContextCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    showToast("Permission denied, missing nearby scan permission.");
                }
                else {
                    showToast("Permission denied, missing permissions");
                }
                finish();
            }
        }
    }

    @SuppressLint("MissingPermission")  //Handled using onRequestPermissionsResult
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mStatusBlueTv   = findViewById(R.id.statusBluetoothTv);
        mPairedTv       = findViewById(R.id.pairedTv);
        mUUID           = findViewById(R.id.uuid);
        mBlueIv         = findViewById(R.id.bluetoothIv);
        mOnBtn          = findViewById(R.id.onBtn);
        mConnectUUID    = findViewById(R.id.connectUUID);
        mChangeUUID     = findViewById(R.id.setUUID);

        //adapter
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        //Check if device has bluetooth functionality
        if (mBlueAdapter == null) {
            showToast("Bluetooth not supported");
            finish();
        } else {

            //Check permissions for dangerous permissions (Permissions can be thinned out by checking device API)
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(BluetoothActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(BluetoothActivity.this, permissionsToRequest.toArray(new String[0]), 2);
            }

            //check if bluetooth is available
            if (mBlueAdapter == null) {
                mStatusBlueTv.setText("Bluetooth is not available");
            } else {
                mStatusBlueTv.setText("Bluetooth is available");
            }

            //Set text to uuid
            mUUID.setText(uuid.toString());

            //set image according to bluetooth status (on/off)
            if (mBlueAdapter.isEnabled()) {
                mBlueIv.setImageResource(R.drawable.ic_action_on);
            } else {
                mBlueIv.setImageResource(R.drawable.ic_action_off);
            }

            //on btn connect to device click
            mConnectUUID.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Turn on bluetooth if not on
                    if (!mBlueAdapter.isEnabled()) {
                        //intent to on bluetooth
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, REQUEST_ENABLE_BT);
                    }

                    //Start discovering nearby bluetooth devices
                    mBlueAdapter.startDiscovery();

                    //Make a receiver to handle discovery
                    BroadcastReceiver mReceiver = new BroadcastReceiver()
                    {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String action = intent.getAction();

                            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                                //Device found
                                mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                                //Connect to device
                                //UUID taken from (Taken from Teams Attendance App Docx)
                                //TODO: IOException handling and figure out connect/disconnect
                                try {
                                    mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
                                    mSocket.connect();
                                    //TODO:Request attendance sheet?
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                //Stop discovery
                                mBlueAdapter.cancelDiscovery();
                                //Unregister receiver
                                unregisterReceiver(this);
                            }
                            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                                showToast("Device not found");
                            }
                        }
                    };
                    // Register the receiver to receive discovery events
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    registerReceiver(mReceiver, filter);
                }
            });

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

            //on change UUID btn click
            mChangeUUID.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothActivity.this);
                    builder.setTitle("Enter Text");

                    View view = getLayoutInflater().inflate(R.layout.edit_text, null);
                    builder.setView(view);

                    // set a positive button with a listener that retrieves the text entered in the EditText view
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText editText = view.findViewById(R.id.editText);
                            uuid = UUID.fromString(editText.getText().toString());
                            mUUID.setText(uuid.toString());
                            //TODO: Implement a way to save the new UUID for next launches
                        }
                    });

                    // set a negative button with a listener that dismisses the dialog
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    // show the dialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }
    }
    //Settings menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //Menu selection
        switch(item.getItemId())
        {
            case R.id.action_settings:
            {
                startActivity(new  Intent(BluetoothActivity.this, SettingsActivity.class));
                return true;
            }
        }

        return false;
    }

    //Helper code for bluetooth permissions
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) {
                    //bluetooth is on
                    mBlueIv.setImageResource(R.drawable.ic_action_on);
                    showToast("Bluetooth is on");
                }
                else {
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

}
