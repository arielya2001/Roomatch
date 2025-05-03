package com.example.roomatch

import android.util.Log
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
import com.google.firebase.firestore.Query

@Composable
fun ChatScreen(
    auth: FirebaseAuth,
    otherUserId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUid = auth.currentUser?.uid ?: return

    val chatId = listOf(currentUid, otherUserId).sorted().joinToString("_")

    var messages by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var newMessage by remember { mutableStateOf("") }

    fun loadChatMessages() {
        db.collection("messages")
            .document(chatId)
            .collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                messages = documents.map { it.data }
            }
            .addOnFailureListener {
                Log.e("ChatScreen", "Error loading chat", it)
                Toast.makeText(context, "שגיאה בטעינת הצ'אט", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(Unit) {
        loadChatMessages()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("צ'אט עם $otherUserId", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                val from = msg["fromUserId"] as? String ?: ""
                val text = msg["text"] as? String ?: ""
                val alignToEnd = from == currentUid

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (alignToEnd) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (alignToEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(text, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                modifier = Modifier.weight(1f),
                label = { Text("הודעה") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newMessage.isNotBlank()) {
                    val message = hashMapOf(
                        "fromUserId" to currentUid,
                        "toUserId" to otherUserId,
                        "text" to newMessage,
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("messages")
                        .document(chatId)
                        .collection("chat")
                        .add(message)
                        .addOnSuccessListener {
                            newMessage = ""
                            loadChatMessages()
                        }
                        .addOnFailureListener {
                            Log.e("ChatScreen", "Error sending message", it)
                            Toast.makeText(context, "שגיאה בשליחת הודעה", Toast.LENGTH_SHORT).show()
                        }
                }
            }) {
                Text("שלח")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("חזור")
        }
    }
}
