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

data class Apartment(
    val location: String = "",
    val price: Int = 0,
    val description: String = "",
    val roommatesNeeded: Int = 0,
    val entryDate: String = "",
    val ownerId: String = ""
)

@Composable
fun ApartmentSearchScreen(
    onLogout: () -> Unit,
    auth: FirebaseAuth
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var apartments by remember { mutableStateOf(listOf<Apartment>()) }
    var loaded by remember { mutableStateOf(false) }

    if (!loaded) {
        LaunchedEffect(Unit) {
            db.collection("apartments")
                .get()
                .addOnSuccessListener { result ->
                    apartments = result.documents.mapNotNull { doc ->
                        try {
                            Apartment(
                                location = doc.getString("location") ?: "",
                                price = when (val rawPrice = doc.get("price")) {
                                    is Long -> rawPrice.toInt()
                                    is Double -> rawPrice.toInt()
                                    is String -> rawPrice.toIntOrNull() ?: 0
                                    else -> 0
                                },
                                description = doc.getString("description") ?: "",
                                roommatesNeeded = when (val rawRoommates = doc.get("roommatesNeeded")) {
                                    is Long -> rawRoommates.toInt()
                                    is Double -> rawRoommates.toInt()
                                    is String -> rawRoommates.toIntOrNull() ?: 0
                                    else -> 0
                                },
                                entryDate = doc.getString("entryDate") ?: "",
                                ownerId = doc.getString("ownerId") ?: ""
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "שגיאה בפרטי דירה: ${e.message}", Toast.LENGTH_SHORT).show()
                            null
                        }
                    }
                    loaded = true
                }
                .addOnFailureListener {
                    Toast.makeText(context, "שגיאה בטעינת דירות", Toast.LENGTH_SHORT).show()
                    loaded = true
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("דירות פנויות", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (!loaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (apartments.isEmpty()) {
            Text("אין דירות זמינות כרגע.")
        } else {
            LazyColumn {
                items(apartments) { apartment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("מיקום: ${apartment.location}")
                            Text("מחיר: ${apartment.price} ש\"ח")
                            Text("שותפים דרושים: ${apartment.roommatesNeeded}")
                            Text("כניסה: ${apartment.entryDate}")
                            Text("תיאור: ${apartment.description}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                Toast.makeText(context, "הודעה נשלחה לבעל הדירה", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("שלח הודעה לבעל הדירה")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onLogout, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("התנתק")
        }
    }
}
