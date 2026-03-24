package com.example.accisense;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Register extends AppCompatActivity {

    EditText name,email,password,phone,vehicle,emergencyName,emergency,blood;
    TextView goLogin;
    Button registerBtn;

    FirebaseAuth mAuth;
    DatabaseReference databaseReference;
    private static final int LOCATION_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.regName);
        email = findViewById(R.id.regEmail);
        password = findViewById(R.id.regPass);
        phone = findViewById(R.id.regPhone);
        vehicle = findViewById(R.id.regVehicle);
        emergencyName = findViewById(R.id.regEmergencyName);
        emergency = findViewById(R.id.regEmergency);
        blood = findViewById(R.id.regBlood);

        registerBtn = findViewById(R.id.btnFinalRegister);
        goLogin = findViewById(R.id.logintxt);

        mAuth = FirebaseAuth.getInstance();

        databaseReference = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child("Driver");

        registerBtn.setOnClickListener(v -> {
            // Get input values
            String userName = name.getText().toString().trim();
            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();
            String userPhone = phone.getText().toString().trim();
            String userVehicle = vehicle.getText().toString().trim();
            String userEmergencyName = emergencyName.getText().toString().trim();
            String userEmergencyPhone = emergency.getText().toString().trim();
            String userBlood = blood.getText().toString().trim();

            // ===== VALIDATIONS =====

            if(userName.isEmpty()){
                name.setError("Name is required");
                name.requestFocus();
                return;
            }

            if(userEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()){
                email.setError("Enter a valid email");
                email.requestFocus();
                return;
            }

            if(userPassword.isEmpty() || !userPassword.matches("^[A-Z](?=.*[0-9])(?=.*[!@#$%^&*]).{7,}$")){
                password.setError("Password must start with uppercase, contain a digit and a special character, min 6 chars");
                password.requestFocus();
                return;
            }

            if(userPhone.isEmpty() || !userPhone.matches("\\d{10}")){
                phone.setError("Enter a valid 10-digit phone number");
                phone.requestFocus();
                return;
            }

            if(userVehicle.isEmpty()){
                vehicle.setError("Vehicle details required");
                vehicle.requestFocus();
                return;
            }

            if(userEmergencyName.isEmpty()){
                emergencyName.setError("Emergency contact name required");
                emergencyName.requestFocus();
                return;
            }

            if(userEmergencyPhone.isEmpty() || !userEmergencyPhone.matches("\\d{10}")){
                emergency.setError("Enter valid 10-digit emergency number");
                emergency.requestFocus();
                return;
            }

            if(userBlood.isEmpty() || !userBlood.matches("^(A|B|AB|O)[+-]$")){
                blood.setError("Enter valid blood group (e.g., A+, O-, AB+)");
                blood.requestFocus();
                return;
            }

            // ===== REGISTRATION =====
            mAuth.createUserWithEmailAndPassword(userEmail,userPassword)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            FirebaseUser user = mAuth.getCurrentUser();
                            if(user != null){
                                String userId = user.getUid();
                                HashMap<String,Object> driverMap = new HashMap<>();
                                driverMap.put("name",userName);
                                driverMap.put("email",userEmail);
                                driverMap.put("phone",userPhone);
                                driverMap.put("vehicle",userVehicle);
                                driverMap.put("emergencyName",userEmergencyName);
                                driverMap.put("emergencyPhone",userEmergencyPhone);
                                driverMap.put("blood",userBlood);

                                databaseReference.child(userId).setValue(driverMap);

                                user.sendEmailVerification()
                                        .addOnCompleteListener(task1 -> {
                                            if(task1.isSuccessful()){
                                                Toast.makeText(Register.this,
                                                        "Verification email sent. Check inbox.",
                                                        Toast.LENGTH_LONG).show();
                                                startActivity(new Intent(Register.this, Login.class));
                                                finish();
                                            } else {
                                                Toast.makeText(Register.this,
                                                        "Verification failed: " + task1.getException().getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(Register.this,
                                    "Registration Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        goLogin.setOnClickListener(v -> startActivity(new Intent(Register.this, Login.class)));
    }
}