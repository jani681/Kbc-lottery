package com.example

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MyApplicationTheme
import org.json.JSONArray
import org.json.JSONObject

data class LocalPrankModel(
    val id: Long,
    val victimName: String,
    val victimNumber: String,
    val generatedLink: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KbcPrankApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun KbcPrankApp(modifier: Modifier = Modifier) {
    var isAuthenticated by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        LoginDialog(onLoginSuccess = { isAuthenticated = true })
    } else {
        // Aapka original logic yahan hai
        MainContent(modifier = modifier)
    }
}

@Composable
fun LoginDialog(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Admin Access", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (username == "jani681" && password == "kbc5800/") {
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Login") }
            }
        }
    }
}

@Composable
fun MainContent(modifier: Modifier) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("kbc_prank_prefs", Context.MODE_PRIVATE) }
    var victimName by remember { mutableStateOf("") }
    var victimNumber by remember { mutableStateOf("") }
    var generatedLink by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val historyList = remember { mutableStateListOf<LocalPrankModel>() }

    LaunchedEffect(Unit) {
        val savedJson = sharedPreferences.getString("prank_list_json", "[]") ?: "[]"
        val jsonArray = JSONArray(savedJson)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            historyList.add(LocalPrankModel(obj.getLong("id"), obj.getString("name"), obj.getString("num"), obj.getString("link")))
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF5F7FB))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // ... (Aapka original UI code yahan wahi rahega)
            // Note: Maincontent mein baaki code purana wala hi paste kar dein jo aapne bheja tha.
            // Main space bacha raha hoon yahan, bas structure upar wala copy karein.
        }
    }
}
