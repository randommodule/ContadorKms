package com.example.contadorkms;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button start;
    private Button exit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start=findViewById(R.id.startButton);
        exit=findViewById(R.id.exitButton);




    }
    public void closeApplication(View view) {
        finishAffinity();
    }
    public void openSecondActivity(View view) {
        Intent intent = new Intent(this, PrincipalMenu.class);
        startActivity(intent);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



}

