// OwnerScreen.kt - כולל תמונה לדירה

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
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var address by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var roommatesNeeded by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    var apartments by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var selectedApartment by remember { mutableStateOf<Map<String, Any>?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    // טען דירות מהמאגר
    fun loadApartments() {
        db.collection("apartments")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { documents ->
                apartments = documents.map { it.data + ("id" to it.id) }
            }
    }

    LaunchedEffect(uid) { loadApartments() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("פרסום דירה חדשה", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

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
                            loadApartments()
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
            } else {
                Toast.makeText(context, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("פרסם דירה")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("הדירות שפרסמת", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(apartments) { apt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedApartment = apt },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("כתובת: ${apt["address"]}")
                        Text("מחיר: ${apt["price"]} ₪")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onLogout, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("התנתק")
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
