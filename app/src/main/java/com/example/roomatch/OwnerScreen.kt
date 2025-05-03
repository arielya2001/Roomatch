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
fun OwnerScreen(
    auth: FirebaseAuth,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    var address by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var apartments by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    // טוען את הדירות שכבר פורסמו ע"י המשתמש
    LaunchedEffect(uid) {
        db.collection("apartments")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { documents ->
                apartments = documents.map { it.data }
            }
            .addOnFailureListener {
                Toast.makeText(context, "שגיאה בטעינת דירות", Toast.LENGTH_SHORT).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("פרסום דירה חדשה", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("כתובת") }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("מחיר (₪)") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (address.isNotEmpty() && price.isNotEmpty()) {
                val apartment = hashMapOf(
                    "ownerId" to uid,
                    "address" to address,
                    "price" to price
                )
                db.collection("apartments").add(apartment)
                    .addOnSuccessListener {
                        Toast.makeText(context, "הדירה נוספה בהצלחה", Toast.LENGTH_SHORT).show()
                        address = ""
                        price = ""
                        // טען מחדש את הרשימה
                        db.collection("apartments")
                            .whereEqualTo("ownerId", uid)
                            .get()
                            .addOnSuccessListener { documents ->
                                apartments = documents.map { it.data }
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "שגיאה בהוספת הדירה", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("פרסם דירה")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("הדירות שפרסמת", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        apartments.forEach { apt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("כתובת: ${apt["address"]}")
                    Text("מחיר: ${apt["price"]} ₪")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("התנתק")
        }
    }
}
