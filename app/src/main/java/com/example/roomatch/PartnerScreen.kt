package com.example.roomatch

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PartnerScreen(
    auth: FirebaseAuth,
    onLogout: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val currentUserId = auth.currentUser?.uid
    var partners by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var selectedProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showReportDialog by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var reportText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("userType", "seeker")
            .whereEqualTo("seekerType", "partner")
            .get()
            .addOnSuccessListener { documents ->
                val results = documents.filter { it.id != currentUserId }
                    .map { it.data + mapOf("id" to it.id) }
                partners = results
            }
            .addOnFailureListener {
                Toast.makeText(context, "שגיאה בטעינת שותפים", Toast.LENGTH_SHORT).show()
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("שותפים פוטנציאליים", style = MaterialTheme.typography.headlineMedium)
        }

        items(partners) { partner ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("שם: ${partner["fullName"] ?: "לא ידוע"}")
                    Text("גיל: ${partner["age"] ?: "לא צוין"}")
                    Text("תחומי עניין: ${partner["interests"] ?: "לא צוין"}")
                    Text("סגנון חיים: ${partner["lifestyle"] ?: "לא צוין"}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            Toast.makeText(context, "הוזמן לקבוצה!", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("הזמן לקבוצה")
                        }
                        Button(onClick = {
                            Toast.makeText(context, "הודעה נשלחה! (כאילו)", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("שלח הודעה")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { selectedProfile = partner }) {
                            Text("צפה בפרופיל")
                        }
                        TextButton(onClick = { showReportDialog = partner["fullName"].toString() to true }) {
                            Text("דווח")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onLogout) {
                    Text("התנתק")
                }
            }
        }
    }

    // צפייה בפרופיל
    selectedProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { selectedProfile = null },
            confirmButton = {
                TextButton(onClick = { selectedProfile = null }) {
                    Text("סגור")
                }
            },
            title = { Text("פרופיל: ${profile["fullName"] ?: "לא ידוע"}") },
            text = {
                Column {
                    Text("גיל: ${profile["age"] ?: "לא צוין"}")
                    Text("מגדר: ${profile["gender"] ?: "לא צוין"}")
                    Text("תחומי עניין: ${profile["interests"] ?: "לא צוין"}")
                    Text("סגנון חיים: ${profile["lifestyle"] ?: "לא צוין"}")
                }
            }
        )
    }

    // דיאלוג דיווח
    showReportDialog?.let { (name, show) ->
        if (show) {
            AlertDialog(
                onDismissRequest = { showReportDialog = null },
                confirmButton = {
                    TextButton(onClick = {
                        Toast.makeText(context, "דיווח נשלח על $name: $reportText", Toast.LENGTH_SHORT).show()
                        showReportDialog = null
                        reportText = ""
                    }) {
                        Text("שלח")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = null }) {
                        Text("ביטול")
                    }
                },
                title = { Text("דווח על $name") },
                text = {
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        label = { Text("סיבת הדיווח") }
                    )
                }
            )
        }
    }
}
