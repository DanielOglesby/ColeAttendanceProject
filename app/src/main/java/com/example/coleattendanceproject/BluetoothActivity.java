package com.example.coleattendanceproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private UUID myUUID;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    //Make a receiver to handle discovery
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                //Bluetooth device found
                mDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //Logging found devices for testing
                Log.d("DEVICE", "Found device: " + mDevice.getName() + " with MAC address " + mDevice.getAddress());
                //Check device UUID
                if(mDevice.getUuids() != null)
                {
                    showToast("getUuids worked");
                    for(ParcelUuid uuid : mDevice.getUuids()) {
                        //Logging uuid found from devices
                        Log.d("BTUUID",uuid.toString());

                        if(uuid.getUuid().equals(myUUID)) {
                            //Connect to device
                            //UUID taken from (Taken from Teams Attendance App Docx)
                            try {
                                mSocket = mDevice.createRfcommSocketToServiceRecord(myUUID);
                                mSocket.connect();
                                showToast("Connection Successful");
                                requestInformation();   //Not tested yet
                                //TODO:Request attendance sheet?
                            } catch (IOException e) {
                                showToast("Failed to connect");
                            }
                            //Stop discovery
                            mBlueAdapter.cancelDiscovery();
                            //Unregister receiver
                            unregisterReceiver(mReceiver);
                            break;
                        }
                    }
                }
            }
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
            {
                  //Unregister receiver
                  unregisterReceiver(mReceiver);
            }
        }
    };

    //Ensures mReceiver is unregistered when done
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

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

    //When back button pressed, finish activity.
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @SuppressLint("MissingPermission")  //Handled using onRequestPermissionsResult
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        // Get the saved UUID string value from SharedPreferences
        String uuidString = PreferenceManager.getDefaultSharedPreferences(BluetoothActivity.this).getString("UUID_KEY", "e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
        // If a valid UUID string is retrieved, update uuid
        if (!uuidString.isEmpty()) {
            try {
                myUUID = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                // Handle the case where the saved string is not a valid UUID
                myUUID = UUID.fromString("e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
                showToast("Invalid UUID input, resetting. . .");
            }
        }

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
            //Check permissions for dangerous permissions
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Check permissions for dangerous permissions
                permissions = new String[]{
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };
            } else {
                // Check permissions for dangerous permissions
                permissions = new String[]{
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };
            }
            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(BluetoothActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(BluetoothActivity.this, permissionsToRequest.toArray(new String[0]), 2);
            }

            //check if bluetooth is available and change icon accordingly
            setIcon();

            //Set text to uuid
            mUUID.setText(myUUID.toString());

            //set image according to bluetooth status (on/off)
            if (mBlueAdapter.isEnabled()) {
                mBlueIv.setImageResource(R.drawable.ic_action_on);
            } else {
                mBlueIv.setImageResource(R.drawable.ic_action_off);
            }

            //Result launcher to replace deprecated startActivityForResult call
            ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            setIcon();      //Changes bluetooth icon to status of bluetooth
                        }
                    });

            //on btn connect to device(UUID) click
            mConnectUUID.setOnClickListener(v -> {
                //Turn on bluetooth if not on
                if (!mBlueAdapter.isEnabled()) {
                    //intent to on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    activityResultLauncher.launch(intent);
                }

                //Register the receiver to receive broadcasts
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(mReceiver, filter);

                //Start discovering nearby bluetooth devices
                mBlueAdapter.startDiscovery();
            });

            //on btn click
            mOnBtn.setOnClickListener(v -> {
                if (!mBlueAdapter.isEnabled()) {
                    showToast("Turning On Bluetooth...");
                    //intent to on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //startActivityForResult(intent,REQUEST_ENABLE_BT);     //Deprecated for activityResultLauncher
                    activityResultLauncher.launch(intent);
                } else {
                    showToast("Bluetooth is already on");
                }
            });

            //on change UUID btn click
            mChangeUUID.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothActivity.this);
                builder.setTitle("Enter Text");

                View view = getLayoutInflater().inflate(R.layout.edit_text, null);
                builder.setView(view);

                EditText myText = view.findViewById(R.id.editText);
                myText.setText(myUUID.toString());

                // set a positive button with a listener that retrieves the text entered in the EditText view
                builder.setPositiveButton("OK", (dialog, which) -> {
                    try {
                        myUUID = UUID.fromString(myText.getText().toString());
                        mUUID.setText(myUUID.toString());

                        // Save the new UUID to SharedPreferences
                        PreferenceManager.getDefaultSharedPreferences(BluetoothActivity.this).edit().putString("UUID_KEY", (myText.getText().toString())).apply();
                    }
                    catch (IllegalArgumentException e) {
                        showToast("Invalid UUID input. Resetting. . .");

                    }
                });

                // set a negative button with a listener that dismisses the dialog
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                // show the dialog
                AlertDialog dialog = builder.create();
                dialog.show();
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
        int id = item.getItemId();

        //Menu selection
        switch(id)
        {
            case R.id.action_settings:
            {
                startActivity(new  Intent(BluetoothActivity.this, SettingsActivity.class));
                return true;
            }
        }

        return false;
    }

    //Helper code for data transmission between desktop and mobile applications
    public void requestInformation() throws IOException {
        //Variables
        InputStream inputStream = mSocket.getInputStream();
        OutputStream outputStream = mSocket.getOutputStream();

        //Send request for data
        String message = "req here";
        outputStream.write(message.getBytes());

        byte[] buffer = new byte[1024];
        int numBytes = inputStream.read(buffer);
        String receivedMessage = new String(buffer, 0, numBytes);
    }

    //Helper code for bluetooth permissions
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //More cases can be added for new menu item
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

    private void setIcon()
    {
        //set image according to bluetooth status (on/off)
        if (mBlueAdapter.isEnabled()) {
            mBlueIv.setImageResource(R.drawable.ic_action_on);
        } else {
            mBlueIv.setImageResource(R.drawable.ic_action_off);
        }
    }

    //toast message function
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
