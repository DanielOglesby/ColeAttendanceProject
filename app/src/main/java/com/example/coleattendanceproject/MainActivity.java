package com.example.coleattendanceproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements Serializable
{
    //Variables
    private UUID myUUID;
    private Boolean btPermissions;

    //Buttons
    EditText editText;
    TextView textView;
    TextView btStatus, connectStatus;
    ImageView btButton;

    //Bluetooth components
    private BluetoothAdapter mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
    //Used for attempting connections to devices paired or discovered
    private final ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();
    private ConnectThread mConnection;
    //Status codes for handler
    private static final int CONNECTED = 1;
    private static final int ERROR = 2;
    private static final int FINISHED = 3;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case CONNECTED:
                    // Connection successful, do something
                    Log.d("BT", "Connection successful!");
                    connectStatus.setText(R.string.connected);
                    editText.setEnabled(true);
                    mConnection.getAttendance();
                    break;
                case ERROR:
                    // Connection failed, do something
                    Log.e("BT", "Connection failed");
                    editText.setEnabled(false);
                    break;
                case FINISHED:
                    btButton.setEnabled(true);
                    btStatus.setText(R.string.click_the_icon_to_scan);
                    connectStatus.setText(R.string.currently_not_connected);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    //TODO: Cleanup on app close


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textNotification);
        btStatus = findViewById(R.id.btStatus);
        connectStatus = findViewById(R.id.connectStatus);
        btButton = findViewById(R.id.bluetoothButton);

        //Get image
        setIcon();

        //Result launcher to replace deprecated startActivityForResult call
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        setIcon();      //Changes bluetooth icon to status of bluetooth
                    }
                });

        // Get the saved UUID string value from SharedPreferences
        String uuidString = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("UUID_KEY", "e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
        btPermissions = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("PERMISSIONS", false);
        // If a valid UUID string is retrieved, update uuid
        if (!uuidString.isEmpty()) {
            try {
                myUUID = UUID.fromString(uuidString);
                Log.d("UUID", myUUID.toString());
            } catch (IllegalArgumentException e) {
                // Handle the case where the saved string is not a valid UUID
                myUUID = UUID.fromString("e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
                Log.d("UUID", "Invalid UUID input, resetting. . .");
            }
        }

        //adapter
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        //Check if device has bluetooth functionality
        if (mBlueAdapter == null) {
            showToast("Bluetooth not supported");
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
                if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(MainActivity.this, permissionsToRequest.toArray(new String[0]), 2);
            } else {
                Set<BluetoothDevice> pairedDevices = mBlueAdapter.getBondedDevices();
                if(pairedDevices.size() > 0) {
                    mDeviceList.addAll(pairedDevices);
                }
                btButton.setEnabled(false);
                connectStatus.setText(R.string.checking_paired_devices);
                mConnection = new ConnectThread(mDeviceList, myUUID, mHandler);
                mConnection.start();
            }
        }

        //on btn connect to device(UUID) click
        btButton.setOnClickListener(v -> {
            //Disable button until finished discovering
            btButton.setEnabled(false);
            btStatus.setText("");
            connectStatus.setText(R.string.checking_discoverable_devices);

            //Turn on bluetooth if not on
            if (!mBlueAdapter.isEnabled()) {
                //intent to on bluetooth
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activityResultLauncher.launch(intent);
            }

            //Register the receiver to receive broadcasts
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);        //May not be needed. TODO: Test without this filter and see if devices found
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);

            //Start discovering nearby bluetooth devices
            mBlueAdapter.startDiscovery();
        });

        /*if(mConnection.getConnectionStatus() == false) {
            editText.setEnabled(false);
        }
        else {
            editText.setEnabled(true);
        }*/
        //TODO SCANNER HERE
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int keyCode, KeyEvent keyEvent) {
                if (false) {
                    //mConnection.write(editText.getText().toString());
                    return true;
                }
            return false;
        }
        });
        //String for attendance sheet from Attend.exe
        //String for sign-ins made from mobile attendance app
        //Use editText for newline
        //mConnection.write();      //Use this to write to Attend.exe   parameter should take a string
    }

    //Settings menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //TODO: Add menu selection to move to BluetoothActivity (need to adjust xmls)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //Menu selection
        switch(id)
        {
            case R.id.action_settings:
            {
                startActivity(new  Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            /*
            case R.id.bluetooth_settings:
            {
                Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
                intent.putExtra("mainContext", MainActivity.this);
                //Pass it whatever else such as bluetooth components
                startActivity(intent);
            }
            */
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 2) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                btPermissions = true;
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    // Permission denied
                    Log.d("PERMISSIONS", "Permission denied for: " + permission);
                    btPermissions = false;
                }
            }
            if(!btPermissions) {
                btButton.setEnabled(false);
                btStatus.setText(R.string.button_disabled_insufficient_permissions);
                showToast("Missing permissions for bluetooth functionality.");
            }
            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("PERMISSIONS", btPermissions).apply();
        }
    }

        //Make a receiver to handle discovery
        //Whenever a scan is made, the function is called.
        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            @Override
            public void onReceive(Context context, Intent intent) {
                //If scan found device, add to device list (mDeviceList)
                if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    //Bluetooth device found
                    BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDeviceList.add(mDevice);
                    //Logging found devices for testing
                    Log.d("DEVICE", "Found device: " + mDevice.getName() + " with MAC address " + mDevice.getAddress());
                }
                //Once finished scanning, parse through list of devices found (mDeviceList) and try to connect on UUID
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                    // discovery has finished, try to connect to all found devices
                    connectStatus.setText(R.string.checking_discoverable_devices);
                    mConnection = new ConnectThread(mDeviceList, myUUID, mHandler);
                    mConnection.start();
                    //Stop discovery
                    Log.d("BT", "Discovery cancelled properly");
                    mDeviceList.clear();
                    mBlueAdapter.cancelDiscovery();
                    btButton.setEnabled(true);
                    //Unregister receiver
                    unregisterReceiver(mReceiver);
                }
            }
        };

    private void setIcon()
    {
        //set image according to bluetooth status (on/off)
        if (mBlueAdapter.isEnabled()) {
            btButton.setImageResource(R.drawable.ic_action_on);
        } else {
            btButton.setImageResource(R.drawable.ic_action_off);
        }
    }

    //toast message helper function
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}