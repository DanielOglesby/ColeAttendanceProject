package com.example.coleattendanceproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {
    private Context mainActivityContext;
    TextView mStatusBlueTv, mPairedTv, mUUID;
    ImageView mBlueIv;
    Button mOnBtn, mChangeUUID;

    BluetoothAdapter mBlueAdapter;

    //Used to connect to desktop attendance app
    private UUID myUUID;

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
        String uuidString = PreferenceManager.getDefaultSharedPreferences(mainActivityContext).getString("UUID_KEY", "e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
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
        mOnBtn          = findViewById(R.id.onBtn);
        mChangeUUID     = findViewById(R.id.setUUID);

        //adapter
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        //Check if device has bluetooth functionality
        if (mBlueAdapter == null) {
            showToast("Bluetooth not supported");
            finish();
        }
        //Permission checks can be removed.
        else {
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
                        PreferenceManager.getDefaultSharedPreferences(mainActivityContext).edit().putString("UUID_KEY", (myText.getText().toString())).apply();
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
