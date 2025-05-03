package com.example.roomatch

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

@Composable
fun OwnerScreen(
    auth: FirebaseAuth,
    onLogout: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUid = auth.currentUser?.uid

    var address by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var roommatesNeeded by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    var apartments by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var selectedApartment by remember { mutableStateOf<Map<String, Any>?>(null) }
    var messages by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    fun loadApartments(uid: String) {
        db.collection("apartments")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { documents ->
                apartments = documents.map { it.data + ("id" to it.id) }
            }
    }

    fun loadMessages(uid: String) {
        db.collection("messages")
            .whereEqualTo("toUserId", uid)
            .get()
            .addOnSuccessListener { documents ->
                messages = documents.map { it.data }
            }
            .addOnFailureListener {
                Toast.makeText(context, "שגיאה בטעינת ההודעות", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(currentUid) {
        currentUid?.let {
            loadApartments(it)
            loadMessages(it)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("פרסום דירה חדשה", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("כתובת") })
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("מחיר (₪)") })
            OutlinedTextField(value = roommatesNeeded, onValueChange = { roommatesNeeded = it }, label = { Text("מספר שותפים דרוש") })
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("תיאור הדירה") })

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("בחר תמונה")
            }
            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (address.isNotEmpty() && price.isNotEmpty()) {
                    currentUid?.let { uid ->
                        val uploadAndSave = {
                            val apartment = hashMapOf(
                                "ownerId" to uid,
                                "address" to address,
                                "price" to price,
                                "roommatesNeeded" to roommatesNeeded,
                                "description" to description,
                                "imageUrl" to imageUrl
                            )
                            db.collection("apartments").add(apartment)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "הדירה נוספה", Toast.LENGTH_SHORT).show()
                                    address = ""; price = ""; roommatesNeeded = ""; description = ""; imageUri = null; imageUrl = ""
                                    loadApartments(uid)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "שגיאה בהוספה", Toast.LENGTH_SHORT).show()
                                }
                        }

                        if (imageUri != null) {
                            val filename = UUID.randomUUID().toString()
                            val ref = storage.reference.child("images/$filename")
                            ref.putFile(imageUri!!).continueWithTask { task ->
                                if (!task.isSuccessful) throw task.exception!!
                                ref.downloadUrl
                            }.addOnSuccessListener { uri ->
                                imageUrl = uri.toString()
                                uploadAndSave()
                            }.addOnFailureListener {
                                Toast.makeText(context, "שגיאה בהעלאת תמונה", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            uploadAndSave()
                        }
                    } ?: Toast.makeText(context, "שגיאה בזיהוי המשתמש", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("פרסם דירה")
            }
        }

        item {
            Text("הדירות שפרסמת", style = MaterialTheme.typography.titleLarge)
        }
        items(apartments) { apt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedApartment = apt },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("כתובת: ${apt["address"]}")
                    Text("מחיר: ${apt["price"]} ₪")
                }
            }
        }

        item {
            Text("הודעות שהתקבלו", style = MaterialTheme.typography.titleLarge)
        }
        items(messages) { msg ->
            val fromUserId = msg["fromUserId"] as? String ?: ""
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("הודעה: ${msg["text"]}")
                    Text("מאת: $fromUserId")
                    Text("עבור דירה: ${msg["apartmentId"]}")
                    Button(onClick = { onOpenChat(fromUserId) }) {
                        Text("פתח צ'אט")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("התנתק")
                }
            }
        }
    }

    selectedApartment?.let { apt ->
        AlertDialog(
            onDismissRequest = { selectedApartment = null },
            confirmButton = {
                TextButton(onClick = { selectedApartment = null }) {
                    Text("סגור")
                }
            },
            title = { Text("פרטי הדירה") },
            text = {
                Column {
                    apt["imageUrl"]?.let {
                        if (it is String && it.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(it),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Text("כתובת: ${apt["address"]}")
                    Text("מחיר: ${apt["price"]} ₪")
                    Text("שותפים דרושים: ${apt["roommatesNeeded"]}")
                    Text("תיאור: ${apt["description"]}")
                }
            }
        )
    }
}
