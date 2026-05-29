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
import com.example.ui.theme.MyApplicationTheme
import org.json.JSONArray
import org.json.JSONObject

data class LocalPrankModel(val id: Long, val victimName: String, val victimNumber: String, val generatedLink: String)

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
        LoginScreen(onLoginSuccess = { isAuthenticated = true })
    } else {
        MainAppContent(modifier)
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(16.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Admin Access", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    if (username == "jani681" && password == "kbc5800/") {
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Login") }
            }
        }
    }
}

@Composable
fun MainAppContent(modifier: Modifier) {
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
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("KBC Lottery Prank Link Generator", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(value = victimName, onValueChange = { victimName = it }, label = { Text("Victim Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = victimNumber, onValueChange = { victimNumber = it }, label = { Text("Victim Phone Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        if (victimName.isNotBlank() && victimNumber.isNotBlank()) {
                            val finalLink = "https://kbc-lottery.vercel.app/?name=${Uri.encode(victimName)}&num=${Uri.encode(victimNumber)}"
                            generatedLink = finalLink
                            showSuccessDialog = true
                            historyList.add(0, LocalPrankModel(System.currentTimeMillis(), victimName, victimNumber, finalLink))
                        }
                    }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Generate Prize Link") }
                }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(historyList) { prank ->
                    Text("Name: ${prank.victimName}, Num: ${prank.victimNumber}", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
