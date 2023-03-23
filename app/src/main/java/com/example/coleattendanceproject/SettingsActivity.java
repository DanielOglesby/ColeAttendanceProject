package com.example.coleattendanceproject;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button clrAttend = (Button)findViewById(R.id.clr_attend);
        Button clrSigned = (Button)findViewById(R.id.clear_signed);

        //Setting up button press functionality for buttons
        clrAttend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAttendanceBtn();
            }
        });

        clrSigned.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearStudentsBtn();
            }
        });
    }

    //Clears the array of student IDs requested from Desktop application
    private void clearAttendanceBtn()
    {
        showToast("TODO: clear attendance String array (requested from Desktop via BT)");
    }
    //Clears the array of student IDs that have been signed in from the Mobile application
    private void clearStudentsBtn()
    {
        showToast("TODO: clear students String array (Made in-app)");
    }

    //Temp function for toasts (TO BE REMOVED WHEN FUNCTIONALITY ADDED)
    private void showToast(String msg) {Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();}
}
