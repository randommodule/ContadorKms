package com.example.contadorkms;


import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button start;
    private Button exit;
    private MediaPlayer mainsong;
    private MediaPlayer startsound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start=findViewById(R.id.startButton);
        exit=findViewById(R.id.exitButton);
        mainsong=MediaPlayer.create(this,R.raw.main_menu_track);
        startsound =MediaPlayer.create(this,R.raw.startsound);
        mainsong.start();
        mainsong.setLooping(true);





    }
    public void closeApplication(View view) {
        startsound.start();
        mainsong.stop();
        finishAffinity();
    }
    public void openSecondActivity(View view) {
        startsound.start();
        mainsong.stop();
        Intent intent = new Intent(this, PrincipalMenu.class);
        startActivity(intent);
    }
    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mainsong != null && mainsong.isPlaying()) {
            mainsong.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainsong != null && !mainsong.isPlaying()) {
            mainsong.start();
        }
    }



}

