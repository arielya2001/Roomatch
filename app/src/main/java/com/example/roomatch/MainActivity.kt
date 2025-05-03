package com.example.roomatch
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color




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

                    var showMissingTypeDialog by remember { mutableStateOf(false) }
                    LaunchedEffect(isUserLoggedIn) {
                        if (!isUserLoggedIn) {
                            currentScreen = "auth"
                            return@LaunchedEffect
                        }

                        val uid = auth.currentUser?.uid
                        val db = FirebaseFirestore.getInstance()

                        if (uid == null) {
                            auth.signOut()
                            isUserLoggedIn = false
                            currentScreen = "auth"
                            return@LaunchedEffect
                        }

                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // המשתמש מחובר, אבל אין לו פרופיל => ננתק אותו
                                    auth.signOut()
                                    isUserLoggedIn = false
                                    currentScreen = "auth"
                                    return@addOnSuccessListener
                                }

                                val userType = document.getString("userType")
                                if (userType.isNullOrEmpty()) {
                                    showMissingTypeDialog = true
                                } else {
                                    currentScreen = when (userType) {
                                        "owner" -> "owner"
                                        "seeker" -> {
                                            val seekerType = document.getString("seekerType")
                                            when (seekerType) {
                                                "partner" -> "partner"
                                                "apartment" -> "apartment"
                                                else -> "profile"
                                            }
                                        }
                                        else -> "profile"
                                    }
                                }
                            }
                            .addOnFailureListener {
                                auth.signOut()
                                isUserLoggedIn = false
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
                            onLoginClicked = { email, password ->
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener {
                                        val uid = auth.currentUser?.uid
                                        val db = FirebaseFirestore.getInstance()
                                        if (uid != null) {
                                            db.collection("users").document(uid).get()
                                                .addOnSuccessListener { document ->
                                                    val userType = document.getString("userType")
                                                    currentScreen = when (userType) {
                                                        "owner" -> "owner"
                                                        "seeker" -> {
                                                            when (document.getString("seekerType")) {
                                                                "partner" -> "partner"
                                                                "apartment" -> "apartment"
                                                                else -> "profile"
                                                            }
                                                        }
                                                        else -> "profile"
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "שגיאה בעת טעינת פרופיל", Toast.LENGTH_SHORT).show()
                                                    currentScreen = "auth"
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "שגיאת התחברות: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            onRegisterClicked = { email, password, username ->
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener {
                                        val uid = auth.currentUser?.uid
                                        val db = FirebaseFirestore.getInstance()
                                        if (uid != null) {
                                            db.collection("users").document(uid).set(
                                                mapOf("username" to username, "userType" to "") // משאיר userType ריק עד יצירת פרופיל
                                            ).addOnSuccessListener {
                                                currentScreen = "profile"
                                            }.addOnFailureListener {
                                                Toast.makeText(context, "שגיאה ביצירת פרופיל", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "שגיאת הרשמה: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
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
                                                "seeker" -> {
                                                    when (document.getString("seekerType")) {
                                                        "partner" -> "partner"
                                                        "apartment" -> "apartment"
                                                        else -> "profile"
                                                    }
                                                }
                                                else -> "profile"
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
                            auth = auth,
                            onLogout = {
                                auth.signOut()
                                isUserLoggedIn = false
                                currentScreen = "auth"
                            }
                        )
                        "partner" -> PartnerScreen(
                            auth = auth,
                            onLogout = {
                                auth.signOut()
                                isUserLoggedIn = false
                                currentScreen = "auth"
                            }
                        )
                        "apartment" -> ApartmentSearchScreen(
                            auth = auth,
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
fun AuthScreen(
    onLoginClicked: (email: String, password: String) -> Unit,
    onRegisterClicked: (email: String, password: String, username: String) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAF3FB))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color(0xFF1E5C9A), radius = 300f, center = Offset(200f, 0f))
            drawCircle(color = Color(0xFF1E5C9A), radius = 150f, center = Offset(80f, size.height))
            drawCircle(color = Color(0xFF1E5C9A), radius = 80f, center = Offset(size.width - 60f, size.height - 200f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ הכותרת החדשה למעלה
            Text(
                text = "Roomatch",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF1E5C9A)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row {
                TextButton(onClick = { isLogin = true }) {
                    Text("Login", color = if (isLogin) Color.Blue else Color.Gray)
                }
                TextButton(onClick = { isLogin = false }) {
                    Text("Register", color = if (!isLogin) Color.Blue else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (!isLogin) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("User Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (isLogin) {
                                onLoginClicked(email, password)
                            } else {
                                onRegisterClicked(email, password, username)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (isLogin) "Login" else "Register")
                    }
                }
            }
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

