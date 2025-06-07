package com.example.roomatch.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roomatch.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class AuthActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editUsername;
    private Button buttonAction;
    private TextView switchModeText, titleText;
    private boolean isLoginMode = true;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editUsername = findViewById(R.id.editUsername);
        buttonAction = findViewById(R.id.buttonAction);
        switchModeText = findViewById(R.id.switchModeText);
        titleText = findViewById(R.id.textTitle);

        updateMode();

        switchModeText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateMode();
        });

        buttonAction.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode) {
                login(email, password);
            } else {
                String username = editUsername.getText().toString().trim();
                if (TextUtils.isEmpty(username)) {
                    Toast.makeText(this, "יש להזין שם משתמש", Toast.LENGTH_SHORT).show();
                    return;
                }
                register(email, password, username);
            }
        });
    }

    private void updateMode() {
        if (isLoginMode) {
            titleText.setText("התחברות");
            editUsername.setVisibility(View.GONE);
            buttonAction.setText("התחבר");
            switchModeText.setText("אין לך חשבון? הרשם");
        } else {
            titleText.setText("הרשמה");
            editUsername.setVisibility(View.VISIBLE);
            buttonAction.setText("הרשם");
            switchModeText.setText("יש לך חשבון? התחבר");
        }
    }

    private void login(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "התחברת בהצלחה", Toast.LENGTH_SHORT).show();
                    startMain();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void register(String email, String password, String username) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("username", username);
                    userMap.put("userType", "");  // ריק עד ליצירת פרופיל

                    db.collection("users").document(uid).set(userMap)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "נרשמת בהצלחה", Toast.LENGTH_SHORT).show();
                                startMain();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "שגיאה בפרופיל: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בהרשמה: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
