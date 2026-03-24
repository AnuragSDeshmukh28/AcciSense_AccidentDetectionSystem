package com.example.accisense;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    EditText email, password;
    Button loginBtn;
    TextView goRegister;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.loginEmail);
        password = findViewById(R.id.loginPass);
        loginBtn = findViewById(R.id.btnLogin);
        goRegister = findViewById(R.id.goRegister);

        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(v -> {

            String userEmail = email.getText().toString().trim();
            String userPass = password.getText().toString().trim();

            if(userEmail.isEmpty() || userPass.isEmpty()){
                Toast.makeText(this,"Enter Email and Password",Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(userEmail,userPass)
                    .addOnCompleteListener(task -> {

                        if(task.isSuccessful()){

                            FirebaseUser user = mAuth.getCurrentUser();

                            if(user != null && user.isEmailVerified()){

                                Toast.makeText(this,"Login Successful",Toast.LENGTH_SHORT).show();

                                startActivity(new Intent(Login.this, Home.class));
                                finish();

                            }else{

                                Toast.makeText(this,
                                        "Please verify your email first",
                                        Toast.LENGTH_LONG).show();

                                mAuth.signOut();
                            }

                        }else{

                            Toast.makeText(this,
                                    "Login Failed: "+task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }

                    });

        });

        goRegister.setOnClickListener(v ->
                startActivity(new Intent(Login.this, Register.class))
        );
    }
    @Override
    protected void onStart() {
        super.onStart();
        if(FirebaseAuth.getInstance().getCurrentUser() != null){
            Intent intent = new Intent(Login.this, Home.class);
            startActivity(intent);
            finish();
        }
    }
}