package com.example.roomatch

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CreateProfileScreen(
    auth: FirebaseAuth,
    onProfileSaved: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var lifestyle by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("seeker") }
    var seekerType by remember { mutableStateOf("apartment") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("צור פרופיל", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("שם מלא") })
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("גיל") })
        OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("מגדר") })
        OutlinedTextField(value = lifestyle, onValueChange = { lifestyle = it }, label = { Text("סגנון חיים") })
        OutlinedTextField(value = interests, onValueChange = { interests = it }, label = { Text("תחומי עניין") })

        Spacer(modifier = Modifier.height(16.dp))
        Text("סוג משתמש:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RadioButton(
                selected = userType == "seeker",
                onClick = { userType = "seeker" }
            )
            Text("מחפש דירה / שותף")

            RadioButton(
                selected = userType == "owner",
                onClick = { userType = "owner" }
            )
            Text("בעל דירה")
        }

        if (userType == "seeker") {
            Spacer(modifier = Modifier.height(16.dp))
            Text("סוג חיפוש:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RadioButton(
                    selected = seekerType == "apartment",
                    onClick = { seekerType = "apartment" }
                )
                Text("מחפש דירה")

                RadioButton(
                    selected = seekerType == "partner",
                    onClick = { seekerType = "partner" }
                )
                Text("מחפש שותף")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val profile = hashMapOf(
                    "fullName" to fullName,
                    "age" to age.toIntOrNull(),
                    "gender" to gender,
                    "lifestyle" to lifestyle,
                    "interests" to interests,
                    "userType" to userType
                )
                if (userType == "seeker") {
                    profile["seekerType"] = seekerType
                }

                db.collection("users").document(uid).set(profile)
                    .addOnSuccessListener {
                        Toast.makeText(context, "הפרופיל נשמר בהצלחה!", Toast.LENGTH_SHORT).show()
                        onProfileSaved()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "שגיאה בשמירת הפרופיל: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }) {
            Text("שמור פרופיל")
        }
    }
}
