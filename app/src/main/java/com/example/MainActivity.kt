package com.example.kbclottery

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var loginLayout: LinearLayout
    private lateinit var webViewContainer: LinearLayout
    private lateinit var webView: WebView
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup traditional clean programmatic layout to avoid layout XML missing issues
        setupPureOldSchoolLayout()

        // Core Login Trigger Logic
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username == "jani681" && password == "kbc5800/") {
                loginLayout.visibility = View.GONE
                webViewContainer.visibility = View.VISIBLE
                initializeAndLoadWebView("https://govt-registry.vercel.app")
            } else {
                Toast.makeText(this, "Invalid ID or Password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeAndLoadWebView(url: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, urlString: String?): Boolean {
                if (urlString != null && (urlString.startsWith("http://") || urlString.startsWith("https://"))) {
                    return false // Keep running inside the app frame safely
                }
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                    startActivity(intent)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(url)
    }

    // Programmatic view builder that doesn't depend on Compose context or specific styles.xml themes
    private fun setupPureOldSchoolLayout() {
        val mainRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 1. LOGIN SCREEN CONTAINER
        loginLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#F5F7FB"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(60, 60, 60, 60)
        }

        etUsername = EditText(this).apply {
            hint = "Enter ID"
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 30) }
        }

        etPassword = EditText(this).apply {
            hint = "Enter Password"
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.BLACK)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 40) }
        }

        btnLogin = Button(this).apply {
            text = "Login"
            setBackgroundColor(android.graphics.Color.parseColor("#1E3A8A"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        loginLayout.addView(etUsername)
        loginLayout.addView(etPassword)
        loginLayout.addView(btnLogin)

        // 2. WEBVIEW CONTAINER
        webViewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        webViewContainer.addView(webView)

        // Attach views to parent frame
        mainRoot.addView(loginLayout)
        mainRoot.addView(webViewContainer)
        setContentView(mainRoot)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack() && webViewContainer.visibility == View.VISIBLE) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
