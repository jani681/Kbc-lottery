package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.KbcPrank
import com.example.data.KbcPrankRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder

class KbcPrankViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: KbcPrankRepository

    // History Flow from Room database
    val allPranks: StateFlow<List<KbcPrank>>

    // Form inputs for generating a prank link
    var friendName by mutableStateOf("")
    var friendNumber by mutableStateOf("")
    var friendAddress by mutableStateOf("")
    var senderName by mutableStateOf("")

    // Last generated prank link details
    var lastGeneratedPrank by mutableStateOf<KbcPrank?>(null)

    // Active recipient details (set when opened via Deep Link or Simulated)
    var activeRecipientPrank by mutableStateOf<KbcPrank?>(null)

    // Dynamic base URL derived from launch intent to ensure correct hosting environment match
    var dynamicBaseUrl by mutableStateOf("https://ais-pre-umhzqhhnbywijbtyijaxgo-854712989427.asia-southeast1.run.app")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KbcPrankRepository(database.kbcPrankDao())
        allPranks = repository.allPranks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Parse incoming deep link Uri and trigger the Congratulations landing screen
     */
    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        
        // Extract parameters from deep-link (handles kbc-winner://congrats?name=... OR browser URLs)
        val nameParam = uri.getQueryParameter("name") ?: uri.getQueryParameter("friendName")
        val numParam = uri.getQueryParameter("number") ?: uri.getQueryParameter("friendNumber")
        val addrParam = uri.getQueryParameter("address") ?: uri.getQueryParameter("friendAddress")
        val senderParam = uri.getQueryParameter("sender") ?: uri.getQueryParameter("senderName") ?: "A Friend"

        if (!nameParam.isNullOrBlank()) {
            activeRecipientPrank = KbcPrank(
                friendName = nameParam,
                friendNumber = numParam ?: "",
                friendAddress = addrParam ?: "",
                senderName = senderParam,
                generatedUrl = uri.toString()
            )
        }
    }

    /**
     * Handle incoming launcher Intent (checks data Uri, raw extras, and converts query params)
     */
    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        // 1. Dynamic host/domain detection from deep link launcher
        val uri = intent.data
        if (uri != null) {
            val scheme = uri.scheme
            val host = uri.host
            if (scheme != null && host != null && host.endsWith("run.app")) {
                dynamicBaseUrl = "$scheme://$host"
            }
            handleDeepLink(uri)
            if (activeRecipientPrank != null) return
        }
        
        // 2. Fallback check for extras (handles raw browser-bridged values or parameter maps)
        val extras = intent.extras
        if (extras != null) {
            val urlString = extras.get("url")?.toString() ?: extras.get("uri")?.toString() ?: extras.get("link")?.toString()
            if (!urlString.isNullOrBlank()) {
                try {
                    val parsedUri = Uri.parse(urlString)
                    val scheme = parsedUri.scheme
                    val host = parsedUri.host
                    if (scheme != null && host != null && host.endsWith("run.app")) {
                        dynamicBaseUrl = "$scheme://$host"
                    }
                    handleDeepLink(parsedUri)
                    if (activeRecipientPrank != null) return
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }
            
            val nameParam = extras.get("name")?.toString() ?: extras.get("friendName")?.toString()
            val numParam = extras.get("number")?.toString() ?: extras.get("friendNumber")?.toString()
            val addrParam = extras.get("address")?.toString() ?: extras.get("friendAddress")?.toString()
            val senderParam = extras.get("sender")?.toString() ?: extras.get("senderName")?.toString() ?: "A Friend"
            
            if (!nameParam.isNullOrBlank()) {
                activeRecipientPrank = KbcPrank(
                    friendName = nameParam,
                    friendNumber = numParam ?: "",
                    friendAddress = addrParam ?: "",
                    senderName = senderParam,
                    generatedUrl = urlString ?: ""
                )
            }
        }
    }

    /**
     * Generate the link and save to local history database
     */
    fun generatePrankLink(baseSharedUrl: String): String {
        if (friendName.isBlank()) return ""

        val encName = URLEncoder.encode(friendName.trim(), "UTF-8")
        val encNumber = URLEncoder.encode(friendNumber.trim(), "UTF-8")
        val encAddress = URLEncoder.encode(friendAddress.trim(), "UTF-8")
        val encSender = URLEncoder.encode(if (senderName.isBlank()) "A Friend" else senderName.trim(), "UTF-8")

        // Build standard HTTPS Link (works as universal custom deep link and safe sharing text)
        // Format: sharedUrl/congrats?name=XYZ&number=123&address=ABC&sender=MQR
        val cleanBase = if (baseSharedUrl.endsWith("/")) baseSharedUrl else "$baseSharedUrl/"
        val link = "${cleanBase}congrats?name=$encName&number=$encNumber&address=$encAddress&sender=$encSender"

        val prank = KbcPrank(
            friendName = friendName.trim(),
            friendNumber = friendNumber.trim(),
            friendAddress = friendAddress.trim(),
            senderName = if (senderName.isBlank()) "A Friend" else senderName.trim(),
            generatedUrl = link
        )

        viewModelScope.launch {
            repository.insertPrank(prank)
        }

        lastGeneratedPrank = prank
        return link
    }

    /**
     * Set active prank directly to trigger recipient preview (In-app simulation)
     */
    fun simulateRecipientPreview(prank: KbcPrank) {
        activeRecipientPrank = prank
    }

    /**
     * Close the prank landing screen and return to generator dashboard
     */
    fun clearActivePrank() {
        activeRecipientPrank = null
    }

    /**
     * Delete a prank record from history
     */
    fun deletePrank(prank: KbcPrank) {
        viewModelScope.launch {
            repository.deletePrankById(prank.id)
            if (lastGeneratedPrank?.id == prank.id) {
                lastGeneratedPrank = null
            }
        }
    }

    /**
     * Clear all generated pranks history
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            lastGeneratedPrank = null
        }
    }

    /**
     * Helper to clear inputs
     */
    fun clearInputs() {
        friendName = ""
        friendNumber = ""
        friendAddress = ""
        senderName = ""
    }
}

// Factory to simplify ViewModel instantiation
class KbcPrankViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KbcPrankViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KbcPrankViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
