package com.example.roomatch.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roomatch.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;


import java.util.HashMap;

public class AuthActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editUsername;
    private Button buttonAction;
    private TextView switchModeText, titleText;
    private boolean isLoginMode = true;

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;

    private FirebaseAuth auth;
    private FirebaseFirestore db;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // נמצא ב־strings.xml
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.customGoogleButton).setOnClickListener(v -> signInWithGoogle());


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

    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "שגיאה בהתחברות עם גוגל", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = auth.getCurrentUser().getUid();
                        db.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
                            if (!snapshot.exists()) {
                                // משתמש חדש – צור מסמך ריק בפרופיל
                                HashMap<String, Object> userData = new HashMap<>();
                                userData.put("username", acct.getDisplayName());
                                userData.put("userType", "");

                                db.collection("users").document(uid)
                                        .set(userData)
                                        .addOnSuccessListener(unused -> {
                                            // רק אחרי שהמסמך נוצר, טען אותו מחדש לוודא שהוא קיים
                                            db.collection("users").document(uid).get()
                                                    .addOnSuccessListener(createdSnapshot -> {
                                                        if (createdSnapshot.exists()) {
                                                            Toast.makeText(this, "נרשמת בהצלחה עם גוגל", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(this, MainActivity.class)
                                                                    .putExtra("fragment", "create_profile"));
                                                            finish();
                                                        } else {
                                                            Toast.makeText(this, "שגיאה: לא נוצר משתמש חדש", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "שגיאה ביצירת משתמש", Toast.LENGTH_SHORT).show()
                                        );

                            } else {
                                // משתמש קיים – המשך רגיל
                                startMain();
                            }
                        });
                    } else {
                        Toast.makeText(this, "שגיאה באימות עם גוגל", Toast.LENGTH_SHORT).show();
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
