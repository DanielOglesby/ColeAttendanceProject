package com.example.coleattendanceproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
{
    //Buttons


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //Move to bluetooth activity
    public void configureBluetoothButton(View view)
    {
        startActivity(new Intent(MainActivity.this, BluetoothActivity.class));
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
                startActivity(new  Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
        }

        return false;
    }
}