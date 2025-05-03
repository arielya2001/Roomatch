package com.example.roomatch

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

data class Apartment(
    val location: String = "",
    val price: Int = 0,
    val description: String = "",
    val roommatesNeeded: Int = 0,
    val entryDate: String = "",
    val ownerId: String = "",
    val imageUrl: String = ""
)

@Composable
fun ApartmentSearchScreen(
    onLogout: () -> Unit,
    auth: FirebaseAuth,
    onOpenChat: (String) -> Unit // הוספה של ניווט לצ'אט
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var apartments by remember { mutableStateOf(listOf<Apartment>()) }
    var loaded by remember { mutableStateOf(false) }
    var selectedApartment by remember { mutableStateOf<Apartment?>(null) }
    var existingMessage by remember { mutableStateOf(false) }

    fun checkIfMessageExists(apartment: Apartment) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("messages")
            .whereEqualTo("fromUserId", currentUserId)
            .whereEqualTo("toUserId", apartment.ownerId)
            .whereEqualTo("apartmentId", apartment.location)
            .get()
            .addOnSuccessListener { docs ->
                existingMessage = !docs.isEmpty
            }
    }

    LaunchedEffect(Unit) {
        if (!loaded) {
            db.collection("apartments")
                .get()
                .addOnSuccessListener { result ->
                    apartments = result.documents.mapNotNull { doc ->
                        try {
                            Apartment(
                                location = doc.getString("location") ?: doc.getString("address") ?: "",
                                price = (doc.get("price") as? Number)?.toInt() ?: 0,
                                description = doc.getString("description") ?: "",
                                roommatesNeeded = (doc.get("roommatesNeeded") as? Number)?.toInt() ?: 0,
                                entryDate = doc.getString("entryDate") ?: "",
                                ownerId = doc.getString("ownerId") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: ""
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("דירות פנויות", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (!loaded) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (apartments.isEmpty()) {
                Text("אין דירות זמינות כרגע.", modifier = Modifier.weight(1f))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(apartments) { apartment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    selectedApartment = apartment
                                    checkIfMessageExists(apartment)
                                }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("מיקום: ${apartment.location}")
                                Text("מחיר: ${apartment.price} ש\"ח")
                                Text("שותפים דרושים: ${apartment.roommatesNeeded}")
                                Text("כניסה: ${apartment.entryDate}")
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("התנתק")
        }
    }

    selectedApartment?.let { apt ->
        AlertDialog(
            onDismissRequest = { selectedApartment = null },
            confirmButton = {
                Column {
                    if (existingMessage) {
                        Button(
                            onClick = {
                                onOpenChat(apt.ownerId) // ניווט לצ'אט עם בעל הדירה
                                selectedApartment = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("המשך לצ'אט")
                        }
                    } else {
                        Button(
                            onClick = {
                                val currentUserId = auth.currentUser?.uid
                                if (currentUserId == null) {
                                    Toast.makeText(context, "שגיאה בזיהוי המשתמש", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val message = hashMapOf(
                                    "fromUserId" to currentUserId,
                                    "toUserId" to apt.ownerId,
                                    "timestamp" to System.currentTimeMillis(),
                                    "text" to "היי! ראיתי את הדירה שפרסמת. אני מעוניין בפרטים נוספים.",
                                    "apartmentId" to apt.location
                                )

                                db.collection("messages")
                                    .add(message)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "ההודעה נשלחה לבעל הדירה", Toast.LENGTH_SHORT).show()
                                        selectedApartment = null
                                        onOpenChat(apt.ownerId)
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "שגיאה בשליחת ההודעה", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("שלח הודעה לבעל הדירה")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { selectedApartment = null }) {
                        Text("סגור")
                    }
                }
            },
            title = { Text("פרטי הדירה") },
            text = {
                Column {
                    if (apt.imageUrl.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(apt.imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text("מיקום: ${apt.location}")
                    Text("מחיר: ${apt.price} ש\"ח")
                    Text("שותפים דרושים: ${apt.roommatesNeeded}")
                    Text("כניסה: ${apt.entryDate}")
                    Text("תיאור: ${apt.description}")
                }
            }
        )
    }
}

