package com.example.accisense;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(() -> {

            if(FirebaseAuth.getInstance().getCurrentUser() != null){

                // User already logged in
                startActivity(new Intent(Splash.this, Home.class));

            }else{

                // User not logged in
                startActivity(new Intent(Splash.this, Login.class));

            }

            finish();

        }, 2500); // 2.5 seconds splash

    }
}