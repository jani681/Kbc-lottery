package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KbcPrankApp()
                }
            }
        }
    }
}

// Data model matching the registration schema
data class Registration(
    val profileId: String = "",
    val name: String = "",
    val cnic: String = "",
    val mobile: String = "",
    val uc: String = "",
    val address: String = "",
    val trxStatus: String = "Pending",
    val timestamp: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KbcPrankApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Form Inputs
    var victimName by remember { mutableStateOf("") }
    var victimPhone by remember { mutableStateOf("") }
    var cnicByPass by remember { mutableStateOf("38202-1258063-3") } // Sample default
    var ucByPass by remember { mutableStateOf("Adhikot") }
    var addressByPass by remember { mutableStateOf("Tiutdjg") }
    
    // State UI
    var generatedLinkId by remember { mutableStateOf("") }
    var latestRegistrationState by remember { mutableStateOf<Registration?>(null) }
    
    // Firebase Database Reference
    val databaseRef = remember { FirebaseDatabase.getInstance().getReference("registrations") }

    // Realtime Listener to monitor incoming registry data
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the very last child added to the database
                if (snapshot.exists()) {
                    val lastChild = snapshot.children.lastOrNull()
                    val registration = lastChild?.getValue(Registration::class.java)
                    if (registration != null) {
                        latestRegistrationState = registration
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        databaseRef.addValueEventListener(listener)
        onDispose { databaseRef.removeEventListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "KBC Lottery Prank Link Generator",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Enter details to generate a working custom prize link",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Victim Name Input
            OutlinedTextField(
                value = victimName,
                onValueChange = { victimName = it },
                label = { Text("Victim Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Victim Phone Input
            OutlinedTextField(
                value = victimPhone,
                onValueChange = { victimPhone = it },
                label = { Text("Victim Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Action Button: Generate Prize Link & Push to Firebase
            Button(
                onClick = {
                    if (victimName.isBlank() || victimPhone.isBlank()) {
                        Toast.makeText(context, "Please enter all details!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Generate Unique Profile ID (e.g., BISP-2606-XXXX)
                    val randomId = (1000..9999).random()
                    val sdf = SimpleDateFormat("ddMM", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    val uniqueProfileId = "BISP-$currentDate-$randomId"
                    
                    val timestampStr = SimpleDateFormat("2026-MM-dd @ HH:mm:ss", Locale.getDefault()).format(Date())

                    val newRegistry = Registration(
                        profileId = uniqueProfileId,
                        name = victimName,
                        cnic = cnicByPass,
                        mobile = victimPhone,
                        uc = ucByPass,
                        address = addressByPass,
                        trxStatus = "Pending",
                        timestamp = timestampStr
                    )

                    // Push to Firebase Realtime Database
                    databaseRef.child(uniqueProfileId).setValue(newRegistry)
                        .addOnSuccessListener {
                            generatedLinkId = uniqueProfileId
                            Toast.makeText(context, "Link Generated Successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Firebase Upload Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⚙  Generate Prize Link", fontSize = 16.sp)
            }

            // Section to display live history updates
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "🔄 Live Generated Links History",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            if (latestRegistrationState == null) {
                Text(
                    text = "No history yet. Generated links will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                // Card showing latest synchronized data item
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), arrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Profile ID: ${latestRegistrationState?.profileId}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Name: ${latestRegistrationState?.name}")
                        Text(text = "Phone: ${latestRegistrationState?.mobile}")
                        Text(text = "Status: ${latestRegistrationState?.trxStatus}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        Text(text = "Time: ${latestRegistrationState?.timestamp}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
