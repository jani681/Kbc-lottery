package com.touqeer.kbclottery

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonContainer: LinearLayout
    
    // Buttons
    private lateinit var btnGenerator: Button
    private lateinit var btnChecker: Button
    private lateinit var btnRegistry: Button // Teesra Button Registry Ke Liye

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Views Initialization
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        buttonContainer = findViewById(R.id.buttonContainer)
        
        btnGenerator = findViewById(R.id.btnGenerator)
        btnChecker = findViewById(R.id.btnChecker)
        btnRegistry = findViewById(R.id.btnRegistry)

        // WebView Settings Configuration
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        // Custom WebViewClient to manage visibility states
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                buttonContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }
        }

        // 1. Generator Button Click - Loads main generator page
        btnGenerator.setOnClickListener {
            webView.loadUrl("https://kbc-lottery.vercel.app/generator.html")
        }

        // 2. Checker Button Click - Loads checker page if any
        btnChecker.setOnClickListener {
            webView.loadUrl("https://kbc-lottery.vercel.app/")
        }

        // 3. Registry Button Click - Directly loads the newly built registry schema form
        btnRegistry.setOnClickListener {
            webView.loadUrl("https://kbc-lottery.vercel.app/registry.html")
        }

        // System Back Press Navigation Handler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.visibility == View.VISIBLE) {
                    // If WebView is open, reset to native menu layout
                    webView.visibility = View.GONE
                    webView.loadUrl("about:blank") // Clear webview stack
                    buttonContainer.visibility = View.VISIBLE
                } else {
                    // Exit app normally if already on menu
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
