package com.example.roomatch
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.roomatch.ui.theme.RoomatchTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        setContent {
            RoomatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = this
                    var isUserLoggedIn by remember {
                        mutableStateOf(auth.currentUser != null)
                    }

                    var currentScreen by remember { mutableStateOf("loading") }

                    LaunchedEffect(isUserLoggedIn) {
                        if (isUserLoggedIn) {
                            val uid = auth.currentUser?.uid
                            val db = FirebaseFirestore.getInstance()
                            if (uid != null) {
                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val userType = document.getString("userType")
                                            currentScreen = when (userType) {
                                                "owner" -> "owner"
                                                "seeker" -> "welcome"
                                                else -> "profile" // fallback אם חסר
                                            }
                                        } else {
                                            currentScreen = "profile"
                                        }
                                    }
                                    .addOnFailureListener {
                                        currentScreen = "auth"
                                    }
                            } else {
                                currentScreen = "auth"
                            }
                        } else {
                            currentScreen = "auth"
                        }
                    }
                    var showMissingTypeDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(isUserLoggedIn) {
                        if (isUserLoggedIn) {
                            val uid = auth.currentUser?.uid
                            val db = FirebaseFirestore.getInstance()
                            if (uid != null) {
                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val userType = document.getString("userType")
                                            if (userType.isNullOrEmpty()) {
                                                showMissingTypeDialog = true
                                            } else {
                                                currentScreen = when (userType) {
                                                    "owner" -> "owner"
                                                    "seeker" -> "welcome"
                                                    else -> "profile"
                                                }
                                            }
                                        } else {
                                            currentScreen = "profile"
                                        }
                                    }
                                    .addOnFailureListener {
                                        currentScreen = "auth"
                                    }
                            } else {
                                currentScreen = "auth"
                            }
                        } else {
                            currentScreen = "auth"
                        }
                    }
                    if (showMissingTypeDialog) {
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {
                                TextButton(onClick = {
                                    showMissingTypeDialog = false
                                    currentScreen = "profile"
                                }) {
                                    Text("למלא פרופיל מחדש")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    auth.signOut()
                                    showMissingTypeDialog = false
                                    isUserLoggedIn = false
                                    currentScreen = "auth"
                                }) {
                                    Text("התנתק")
                                }
                            },
                            title = { Text("פרופיל לא שלם") },
                            text = { Text("נראה שחסרים פרטים בפרופיל שלך. מה תרצה לעשות?") }
                        )
                    }

                    when (currentScreen) {
                        "loading" -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        "auth" -> AuthScreen(
                            auth = auth,
                            onAuthSuccess = {
                                isUserLoggedIn = false
                                isUserLoggedIn = true
                            },
                            context = context
                        )

                        "profile" -> CreateProfileScreen(
                            auth = auth,
                            onProfileSaved = {
                                val uid = auth.currentUser?.uid
                                val db = FirebaseFirestore.getInstance()
                                if (uid != null) {
                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { document ->
                                            val userType = document.getString("userType")
                                            currentScreen = when (userType) {
                                                "owner" -> "owner"
                                                "seeker" -> "welcome"
                                                else -> "profile"//d
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "שגיאה בעת טעינת פרופיל", Toast.LENGTH_SHORT).show()
                                            currentScreen = "auth"
                                        }
                                } else {
                                    currentScreen = "auth"
                                }
                            }
                        )


                        "welcome" -> HomeScreen(
                            auth = auth,
                            onLogout = {
                                auth.signOut()
                                isUserLoggedIn = false
                                currentScreen = "auth"
                            }
                        )
                        "owner" -> OwnerScreen(
                            onLogout = {
                                auth.signOut()
                                isUserLoggedIn = false
                                currentScreen = "auth"
                            }
                        )

                    }
                }
            }
        }

    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth, onAuthSuccess: () -> Unit, context: android.content.Context) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLoginMode) "התחברות" else "הרשמה",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("אימייל") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("סיסמה") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isLoginMode) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            } else {
                                Toast.makeText(context, "שגיאה בהתחברות: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            } else {
                                Toast.makeText(context, "שגיאה בהרשמה: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
        ) {
            Text(if (isLoginMode) "התחבר" else "הרשם")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(if (isLoginMode) "אין לך חשבון? לחץ כאן להרשמה"
            else "כבר יש לך חשבון? התחבר")
        }
    }
}

@Composable
fun WelcomeScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ברוך הבא לאפליקציית Roomatch!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onLogout) {
            Text("התנתק")
        }
    }
}