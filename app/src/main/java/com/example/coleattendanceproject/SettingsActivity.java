package com.example.coleattendanceproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.UUID;

public class SettingsActivity extends AppCompatActivity
{
    //Result codes for ActivityResultLauncher
    public static final int ATTEND_CODE = 1;
    public static final int SIGN_CODE = 2;
    public static final int CLEAR_CODE = 3;
    public static final int REQ_CODE = 4;
    //Boolean to determine what was cleared
    public boolean attendCleared = false;
    public boolean signCleared = false;
    public boolean attendReq = false;
    //String arraylist to be passed from MainActivity
    private ArrayList<String> attendance;
    private ArrayList<String> signIns;
    //Used to connect to desktop attendance app
    private UUID myUUID;

    //When back button pressed, finish activity.
    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        if (attendReq) {
            setResult(REQ_CODE, resultIntent);
        }
        else if(attendCleared && signCleared) {
            setResult(CLEAR_CODE, resultIntent);
        }
        else if(attendCleared && !signCleared) {
            setResult(ATTEND_CODE, resultIntent);
        }
        else if (!attendCleared && signCleared) {
            setResult(SIGN_CODE, resultIntent);
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //ArrayLists from MainActivity
        attendance = getIntent().getStringArrayListExtra("attendance");
        signIns = getIntent().getStringArrayListExtra("signIns");

        TextView uuid = (TextView)findViewById(R.id.uuid);
        TextView attendNum = (TextView)findViewById(R.id.attendNumber);
        TextView signNum = (TextView)findViewById(R.id.signedInNumber);

        Button clrAttend = (Button)findViewById(R.id.clr_attend);
        Button clrSigned = (Button)findViewById(R.id.clear_signed);
        Button changeUUID = (Button)findViewById(R.id.setUUID);
        Button reqAttend = (Button)findViewById(R.id.reqAttend);

        // Get the saved UUID string value from SharedPreferences
        String uuidString = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("UUID_KEY", "e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
        // If a valid UUID string is retrieved, update uuid
        if (!uuidString.isEmpty()) {
            try {
                myUUID = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                // Handle the case where the saved string is not a valid UUID
                myUUID = UUID.fromString("e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
                Log.e("SETTINGS", "Invalid UUID, resetting to default");
            }
        }

        //Set text to uuid
        uuid.setText(myUUID.toString());
        //Set text to number of students in attendance sheet
        attendNum.setText(String.format(getString(R.string.number_of_students), attendance.size()));
        //Set text to number of signed in students
        signNum.setText(String.format(getString(R.string.number_of_sign_ins), signIns.size()));

        //Clear attendance sheet on button press
        clrAttend.setOnClickListener(v ->
                {
                    clearAttendanceBtn();
                    //Set text to number of students in attendance sheet
                    attendNum.setText(String.format(getString(R.string.number_of_students), attendance.size()));
                });
        //Clear student sign-ins on button press
        clrSigned.setOnClickListener(v ->
                {
                    clearStudentsBtn();
                    //Set text to number of signed in students
                    signNum.setText(String.format(getString(R.string.number_of_sign_ins), signIns.size()));
                }
                );
        //Request new Attendance Sheet on button press
        reqAttend.setOnClickListener(v ->
                {
                    attendReq = true;
                    attendNum.setText(R.string.attendance_sheet_will_be_updated_in_main);
                }
        );

        //Change uuid on button click
        changeUUID.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
            builder.setTitle("Enter Text");

            View view = getLayoutInflater().inflate(R.layout.edit_text, null);
            builder.setView(view);

            EditText myText = view.findViewById(R.id.editText);
            myText.setText(myUUID.toString());

            // set a positive button with a listener that retrieves the text entered in the EditText view
            builder.setPositiveButton("OK", (dialog, which) -> {
                try {
                    myUUID = UUID.fromString(myText.getText().toString());
                    uuid.setText(myUUID.toString());

                    // Save the new UUID to SharedPreferences
                    androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("UUID_KEY", (myText.getText().toString())).apply();
                } catch (IllegalArgumentException e) {
                    // Handle the case where the saved string is not a valid UUID
                    myUUID = UUID.fromString("e0cbf06c-cd8b-4647-bb8a-263b43f0f974");
                    Log.e("SETTINGS", "Invalid UUID, resetting to default");
                }
            });

            // set a negative button with a listener that dismisses the dialog
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            // show the dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    //Clears the array of student IDs requested from Desktop application
    private void clearAttendanceBtn()
    {
        //Consider passing mConnection and disconnecting if button is pressed OR grabbing new attendance sheet?
        attendance.clear();
        attendCleared = true;
    }
    //Clears the array of student IDs that have been signed in from the Mobile application
    private void clearStudentsBtn()
    {
        signIns.clear();
        signCleared = true;
    }
}
