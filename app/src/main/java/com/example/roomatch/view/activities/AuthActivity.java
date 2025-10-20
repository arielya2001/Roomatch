package com.example.roomatch.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roomatch.R;
import com.example.roomatch.model.UserProfile;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class AuthActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editUsername;
    private Button buttonAction;
    private TextView switchModeText, titleText;
    private boolean isLoginMode = true;

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public static boolean isTestMode = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
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
                Toast.makeText(this, ".נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode) {
                login(email, password);
            } else {
                String username = editUsername.getText().toString().trim();
                if (TextUtils.isEmpty(username)) {
                    Toast.makeText(this, ".יש להזין שם משתמש", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (username.length() < 3) {
                    Toast.makeText(this, "שם משתמש קצר מדי (צריך מינימום 3 תווים)", Toast.LENGTH_SHORT).show();
                    return;
                }
                register(email, password, username);
            }
        });
    }

    private void signInWithGoogle() {
        Log.d("AuthActivity", "Google Sign-In button clicked");
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d("AuthActivity", "Signed out from Google, starting sign-in intent");
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Log.d("AuthActivity", "Received Google Sign-In result, resultCode=" + resultCode);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("AuthActivity", "Google Sign-In successful, account=" + account.getEmail());
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.e("AuthActivity", "Google sign-in failed: StatusCode=" + e.getStatusCode() + ", Message=" + e.getMessage(), e);
                Toast.makeText(this, "שגיאה בהתחברות עם גוגל: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("AuthActivity", "Authenticating with Firebase for account: " + acct.getEmail());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthActivity", "Firebase authentication successful");
                        String uid = auth.getCurrentUser().getUid();
                        db.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
                            if (!snapshot.exists()) {
                                Log.d("AuthActivity", "New user, creating profile");
                                UserProfile userProfile = new UserProfile();
                                userProfile.setFullName(acct.getDisplayName());
                                userProfile.setUserType("");

                                db.collection("users").document(uid)
                                        .set(userProfile)
                                        .addOnSuccessListener(unused -> {
                                            Log.d("AuthActivity", "User profile created successfully");
                                            saveFcmToken();
                                            db.collection("users").document(uid).get()
                                                    .addOnSuccessListener(createdSnapshot -> {
                                                        if (createdSnapshot.exists()) {
                                                            Toast.makeText(this, "נרשמת בהצלחה עם גוגל", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(this, MainActivity.class)
                                                                    .putExtra("fragment", "create_profile"));
                                                            finish();
                                                        } else {
                                                            Log.e("AuthActivity", "Failed to verify user profile creation");
                                                            Toast.makeText(this, "שגיאה: לא נוצר משתמש חדש", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("AuthActivity", "Failed to create user profile: " + e.getMessage(), e);
                                            Toast.makeText(this, "שגיאה ביצירת משתמש", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Log.d("AuthActivity", "Existing user, proceeding to main");
                                startMain();
                            }
                        });
                    } else {
                        Log.e("AuthActivity", "Firebase authentication failed: " + task.getException().getMessage(), task.getException());
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
        if (isTestMode) {
            showTestStatus("התחברת בהצלחה");
            return;
        }


        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    saveFcmToken();
                    Toast.makeText(this, "התחברת בהצלחה", Toast.LENGTH_SHORT).show();
                    startMain();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "הסיסמה או שם המשתמש שהזנת שגויים", Toast.LENGTH_LONG).show()
                );
    }

    private void register(String email, String password, String username) {
        if (isTestMode) {
            showTestStatus("נרשמת בהצלחה");
            return;
        }


        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    UserProfile userProfile = new UserProfile();
                    userProfile.setFullName(username);
                    userProfile.setUserType("");

                    db.collection("users").document(uid).set(userProfile)
                            .addOnSuccessListener(unused -> {
                                saveFcmToken();
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
    private void saveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    String uid = FirebaseAuth.getInstance().getUid();
                    if (uid != null) {
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("fcmToken", token)
                                .addOnSuccessListener(unused -> Log.d("FCM", "Token saved successfully"))
                                .addOnFailureListener(e -> Log.e("FCM", "Failed to save FCM token", e));
                    }
                });
    }

    private void showTestStatus(String message) {
        TextView testStatus = findViewById(R.id.testStatusTextView);
        testStatus.setVisibility(View.VISIBLE);
        testStatus.setText(message);
    }


}
