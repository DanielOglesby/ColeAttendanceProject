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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
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
    //Test Button
    Button testBtn;

    //Bluetooth components
    private BluetoothAdapter mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
    //Used for attempting connections to devices paired or discovered
    private final ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();
    private ConnectThread mConnection;
    //Status codes for handler
    private static final int CONNECTED = 1;
    private static final int ERROR = 2;
    private static final int FINISHED = 3;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case CONNECTED:
                    // Connection successful, do something
                    Log.d("BT", "Connection successful!");
                    connectStatus.setText(R.string.connected);
                    editText.setEnabled(true);
                    mConnection.getAttendance();
                    Log.d("IO", "Messages: " + mConnection.getMessages());
                    attendanceList();
                    break;
                case ERROR:
                    // Connection failed, do something
                    Log.e("BT", "Connection failed");
                    editText.setEnabled(false);
                    break;
                case FINISHED:
                    mConnection.stopThread();
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

    //Variables
    ArrayList<String> attendance = new ArrayList<String>();         //Taken from Attend.exe
    ArrayList<String> signIns = new ArrayList<String>();        //Saved when a student swipes card on phone





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
        String uuidString = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("UUID_KEY", "e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
        btPermissions = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("PERMISSIONS", false);
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
                connectPaired();
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
            //filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);        //May not be needed. TODO: Test without this filter and see if devices found
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);

            //Start discovering nearby bluetooth devices
            mBlueAdapter.startDiscovery();

            //Wait for initial thread to finish before restarting(made when attempted to connect to paired devices)
            try {
                if(mConnection != null) {
                    mConnection.join();
                }
            } catch (InterruptedException e) {
                Log.e("THREADS", "Something happened when calling mConnection.join() in MainActivity.java");        //Hardcoded error message
            }
        });

        //Scanner to write to Attend.exe
        EditText editText = findViewById(R.id.editText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This method is called before the text is changed.
                // nothing happens here
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This method is called when the text is changed.
                // runs myFunction method when newline is encountered
                if (s.toString().contains("\n")) {
                    //Write to Attend.exe (TODO: add checks before writing)
                    Log.d("IO", "EditText: " + editText.getText().toString());
                    if(mConnection != null) {
                        mConnection.write(editText.getText().toString());
                    }
                    //Clear editText
                    editText.setText("");
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                // This method is called after the text is changed.
                // nothing happens here

            }
        });


    }

    private void myFunction() {
        //This method runs when newline is encountered in editText
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
            case R.id.action_disconnect:
            {
                mConnection.stopThread();
                //The following could be shrunken into a helper function
                attendance.clear();
                signIns.clear();
                btButton.setEnabled(true);
                btStatus.setText(R.string.click_the_icon_to_scan);
                connectStatus.setText(R.string.currently_not_connected);
                return true;
            }
            case R.id.retry_paired:
            {
                if(!connectStatus.getText().toString().equals("Connected!")) {
                    connectPaired();
                }
                else {
                    showToast("Already connected!");
                }
                return true;
            }
            case R.id.action_settings:
            {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                //Pass attendance, and signIns for clearing if user desires
                intent.putStringArrayListExtra("attendance", attendance);
                intent.putStringArrayListExtra("signIns", signIns);
                startActivity(intent);
                return true;
            }
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
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("PERMISSIONS", btPermissions).apply();
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
                    mBlueAdapter.cancelDiscovery();
                    btButton.setEnabled(true);
                    //Unregister receiver
                    unregisterReceiver(mReceiver);
                }
            }
        };

    private void connectPaired() {
        btStatus.setText("");
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = mBlueAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            mDeviceList.addAll(pairedDevices);
        }
        btButton.setEnabled(false);
        editText.setEnabled(false);
        connectStatus.setText(R.string.checking_paired_devices);
        mConnection = new ConnectThread(mDeviceList, myUUID, mHandler);
        mConnection.start();
    }

    private void setIcon()
    {
        //set image according to bluetooth status (on/off)
        if (mBlueAdapter.isEnabled()) {
            btButton.setImageResource(R.drawable.ic_action_on);
        } else {
            btButton.setImageResource(R.drawable.ic_action_off);
        }
    }
    //Add the attendance onto arraylist
    public void attendanceList() {

        mConnection.getAttendance();
        String[] messagesArray = mConnection.getMessages().split("\n");

        for (String message : messagesArray) {
            // Check if the message contains an asterisk
            if (message.contains("*")) {
                // Stop adding messages to the attendance list
                break;
            }
            attendance.add(message);
        }
        Log.d("IO", "IDs in ArrayList: " + attendance);
    }





    //toast message helper function
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}