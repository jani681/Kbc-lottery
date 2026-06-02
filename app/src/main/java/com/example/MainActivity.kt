package com.example.kbclottery

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F7FB)
                ) {
                    if (!isAuthenticated) {
                        LoginScreen(onLoginSuccess = { isAuthenticated = true })
                    } else {
                        WebViewContainer(url = "https://govt-registry.vercel.app")
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
        modifier = Modifier.fillMaxSize(),
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(url: String) {
    val context = LocalContext.current
    
    // CRITICAL FIX: WebView instance is safely retained across recompositions via remember.
    // This stops the infinite re-initialization and hard-crashes.
    val memoizedWebView = remember {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, urlString: String?): Boolean {
                    if (urlString != null && (urlString.startsWith("http://") || urlString.startsWith("https://"))) {
                        return false // Load inside the webview internally
                    }
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                        context.startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return true
                }
            }

            webChromeClient = WebChromeClient()
            loadUrl(url) // Initial load executes exactly once.
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AndroidView(
            factory = { memoizedWebView },
            modifier = Modifier.fillMaxSize(),
            update = { /* Updates are safely managed by instance memory state */ }
        )
    }
}
