package com.example.roomatch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class UserProfile(
    val fullName: String = "",
    val age: Int = 0,
    val lifestyle: String = "",
    val interests: String = ""
)

@Composable
fun HomeScreen(auth: FirebaseAuth, onLogout: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var otherUsers by remember { mutableStateOf(listOf<UserProfile>()) }

    LaunchedEffect(Unit) {
        // שלוף את המשתמש הנוכחי
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    document?.let {
                        val profile = UserProfile(
                            fullName = it.getString("fullName") ?: "",
                            age = it.getLong("age")?.toInt() ?: 0,
                            lifestyle = it.getString("lifestyle") ?: "",
                            interests = it.getString("interests") ?: ""
                        )
                        currentUserProfile = profile
                    }
                }

            // שלוף את שאר המשתמשים
            db.collection("users").get()
                .addOnSuccessListener { result ->
                    val others = result.documents
                        .filter { it.id != uid }
                        .map {
                            UserProfile(
                                fullName = it.getString("fullName") ?: "",
                                age = it.getLong("age")?.toInt() ?: 0,
                                lifestyle = it.getString("lifestyle") ?: "",
                                interests = it.getString("interests") ?: ""
                            )
                        }
                    otherUsers = others
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        currentUserProfile?.let { user ->
            Text("שלום, ${user.fullName}!", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("שותפים אפשריים:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(otherUsers) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("שם: ${user.fullName}")
                        Text("גיל: ${user.age}")
                        Text("סגנון חיים: ${user.lifestyle}")
                        Text("תחומי עניין: ${user.interests}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("התנתק")
        }
    }
}
