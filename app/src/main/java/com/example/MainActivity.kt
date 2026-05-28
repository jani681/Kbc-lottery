package com.example

import android.app.Application
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.data.KbcPrank
import com.example.ui.KbcPrankViewModel
import com.example.ui.KbcPrankViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// Beautiful Custom KBC Palette
val KbcMidnightBlue = Color(0xFF0F0933)
val KbcDeepBackground = Color(0xFF040212)
val KbcRoyalBlue = Color(0xFF1B0E5A)
val KbcGoldPrimary = Color(0xFFE6C045)
val KbcGoldSecondary = Color(0xFFFFEA8A)
val KbcGoldDark = Color(0xFF917215)
val KbcPurpleGlow = Color(0xFF8138FF)
val KbcCoralAccent = Color(0xFFFF3B5C)

// High Density Theme Palette (Design system alignment)
val HdBackground = Color(0xFFFEF7FF)
val HdTextPrimary = Color(0xFF1D1B20)
val HdTextSecondary = Color(0xFF49454F)
val HdPrimaryPurple = Color(0xFF6750A4)
val HdSurfaceLight = Color(0xFFF3EDF7)
val HdBorderColor = Color(0xFFCAC4D0)
val HdBorderActive = Color(0xFF79747E)
val HdBadgeBg = Color(0xFFE8DEF8)
val HdBadgeText = Color(0xFF1D192B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = applicationContext as Application
        val viewModelFactory = KbcPrankViewModelFactory(app)
        val viewModelInstance = androidx.lifecycle.ViewModelProvider(this, viewModelFactory)[KbcPrankViewModel::class.java]
        
        // Handle deep link / intent parameters immediately in onCreate
        viewModelInstance.handleIntent(intent)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel = viewModelInstance

                val hasActivePrank = viewModel.activeRecipientPrank != null

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (hasActivePrank) Color(0xFF03010C) else HdBackground
                ) {
                    MainScreenContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = applicationContext as Application
        val viewModelFactory = KbcPrankViewModelFactory(app)
        val viewModel = androidx.lifecycle.ViewModelProvider(this, viewModelFactory)[KbcPrankViewModel::class.java]
        viewModel.handleIntent(intent)
    }
}

@Composable
fun MainScreenContent(viewModel: KbcPrankViewModel) {
    val activePrank = viewModel.activeRecipientPrank
    val hasActivePrank = activePrank != null

    // Intercept physical system/gesture back buttons to cleanly dismiss the winner screen 
    androidx.activity.compose.BackHandler(enabled = hasActivePrank) {
        viewModel.clearActivePrank()
    }

    // DIRECTLY SETTING YOUR GITHUB PAGES LIVE LINK HERE
    val baseSharedUrl = "https://jani681.github.io/Kbc-lottery/"

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = hasActivePrank,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
            label = "ScreenSwitch animate"
        ) { isPrankScreen ->
            if (isPrankScreen && activePrank != null) {
                RecipientCongratsScreen(
                    prank = activePrank,
                    onBackToApp = { viewModel.clearActivePrank() }
                )
            } else {
                CreatorDashboardScreen(
                    viewModel = viewModel,
                    baseSharedUrl = baseSharedUrl
                )
            }
        }
    }
}

/**
 * -------------------------------------------------------------
 * CREATOR DASHBOARD SCREEN
 * -------------------------------------------------------------
 */
@Composable
fun CreatorDashboardScreen(
    viewModel: KbcPrankViewModel,
    baseSharedUrl: String
) {
    val context = LocalContext.current
    val pranksHistory by viewModel.allPranks.collectAsStateWithLifecycle()
    var showCopyConfirmDialog by remember { mutableStateOf(false) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isNearTop by remember { derivedStateOf { listState.firstVisibleItemIndex < 1 } }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = HdBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HdBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu icon",
                        tint = HdTextSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                Toast.makeText(context, "KBC Generator Menu", Toast.LENGTH_SHORT).show()
                            }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "KBC Link Generator",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = HdTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Account icon",
                        tint = HdTextSecondary,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                Toast.makeText(context, "Logged in as Host", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
                Divider(color = HdBorderColor, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HdBackground)
            ) {
                Divider(color = HdBorderColor, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Create Home Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Create / Home Icon",
                            tint = if (isNearTop) HdPrimaryPurple else HdTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Home / Create",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isNearTop) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isNearTop) HdPrimaryPurple else HdTextSecondary
                        )
                    }

                    // History Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    val targetIndex = if (viewModel.lastGeneratedPrank != null) 2 else 1
                                    listState.animateScrollToItem(targetIndex)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "History Icon",
                            tint = if (!isNearTop) HdPrimaryPurple else HdTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (!isNearTop) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (!isNearTop) HdPrimaryPurple else HdTextSecondary
                        )
                    }

                    // Settings Info Tab (Kept pure static according to boundaries)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .alpha(0.5f)
                            .clickable {
                                Toast.makeText(context, "KBC Special Edition Config active.", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Icon",
                            tint = HdTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = HdTextSecondary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Beautiful Input Form Container
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = HdSurfaceLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prank_creator_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "RECEIVER DETAILS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = HdPrimaryPurple,
                                letterSpacing = 1.5.sp
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Friend Name
                        OutlinedTextField(
                            value = viewModel.friendName,
                            onValueChange = { viewModel.friendName = it },
                            label = { Text("Friend's Name") },
                            placeholder = { Text("e.g. Rahul Kumar") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = HdPrimaryPurple) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = HdTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HdPrimaryPurple,
                                unfocusedBorderColor = HdBorderActive,
                                focusedLabelColor = HdPrimaryPurple,
                                unfocusedLabelColor = HdTextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("friend_name_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        // Friend Phone
                        OutlinedTextField(
                            value = viewModel.friendNumber,
                            onValueChange = { viewModel.friendNumber = it },
                            label = { Text("Mobile Number") },
                            placeholder = { Text("e.g. +91 98765 43210") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone", tint = HdPrimaryPurple) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = HdTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HdPrimaryPurple,
                                unfocusedBorderColor = HdBorderActive,
                                focusedLabelColor = HdPrimaryPurple,
                                unfocusedLabelColor = HdTextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("friend_phone_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        // Friend Location
                        OutlinedTextField(
                            value = viewModel.friendAddress,
                            onValueChange = { viewModel.friendAddress = it },
                            label = { Text("Home Address") },
                            placeholder = { Text("e.g. Lokhandwala, Mumbai") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = "Address", tint = HdPrimaryPurple) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = HdTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HdPrimaryPurple,
                                unfocusedBorderColor = HdBorderActive,
                                focusedLabelColor = HdPrimaryPurple,
                                unfocusedLabelColor = HdTextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("friend_address_input"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                        )

                        // Creator/Sender Name
                        OutlinedTextField(
                            value = viewModel.senderName,
                            onValueChange = { viewModel.senderName = it },
                            label = { Text("Your Name (Host/Sender)") },
                            placeholder = { Text("e.g. Amit (or 'A Friend')") },
                            leadingIcon = { Icon(Icons.Default.Face, contentDescription = "Sender", tint = HdPrimaryPurple) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = HdTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HdPrimaryPurple,
                                unfocusedBorderColor = HdBorderActive,
                                focusedLabelColor = HdPrimaryPurple,
                                unfocusedLabelColor = HdTextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("sender_name_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // High Density Template Preview Card Container
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TEMPLATE PREVIEW",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HdTextSecondary,
                                letterSpacing = 1.sp
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = HdBadgeBg,
                            border = BorderStroke(0.5.dp, HdPrimaryPurple.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "KBC Special Edition",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HdBadgeText
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HdBorderColor, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF001E2F))
                                    .border(1.dp, Color(0xFF004A77), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 3 Small avatars
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    ) {
                                        // Amitabh Small
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, Color(0xFFE6C045), CircleShape)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.img_amitabh),
                                                    contentDescription = "A.B.",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Text("A.B.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold), color = Color.LightGray)
                                        }

                                        // Ambani Small
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, Color(0xFFE6C045), CircleShape)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.img_ambani),
                                                    contentDescription = "AMBANI",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Text("AMBANI", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold), color = Color.LightGray)
                                        }

                                        // Modi Small
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, Color(0xFFE6C045), CircleShape)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.img_modi),
                                                    contentDescription = "MODI",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Text("MODI", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold), color = Color.LightGray)
                                        }
                                    }

                                    Text(
                                        text = "🏆 Congratulations! 🏆",
                                        color = Color(0xFFE6C045),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            fontSize = 11.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val displayName = if (viewModel.friendName.isNotBlank()) viewModel.friendName else "Rahul Kumar"
                                    val displayAddress = if (viewModel.friendAddress.isNotBlank()) viewModel.friendAddress else "Lokhandwala, Mumbai"

                                    Text(
                                        text = "$displayName, you have won INR 25,00,000 from Kaun Banega Crorepati Official.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 13.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.9f)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Address: $displayAddress",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = Color.White.copy(alpha = 0.7f))
                                        )
                                        Text(
                                            text = "ID: KBC-2026-DRAW",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = Color.White.copy(alpha = 0.7f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Generate Link Trigger Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { viewModel.clearInputs() },
                        colors = ButtonDefaults.textButtonColors(contentColor = HdTextSecondary),
                        modifier = Modifier.weight(0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear")
                    }

                    Button(
                        onClick = {
                            if (viewModel.friendName.isBlank()) {
                                Toast.makeText(context, "Please enter Friend's name!", Toast.LENGTH_SHORT).show()
                            } else {
                                val link = viewModel.generatePrankLink(baseSharedUrl)
                                // Save to system clipboard
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("KBC Prank Link", link)
                                clipboard.setPrimaryClip(clip)
                                showCopyConfirmDialog = true
                                Toast.makeText(context, "Link Copied & Generated!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HdPrimaryPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(100), // Rounded Full pill shape
                        modifier = Modifier
                            .weight(0.7f)
                            .height(56.dp)
                            .shadow(4.dp, RoundedCornerShape(100), clip = false)
                            .testTag("generate_link_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Link Icon", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Generate Prize Link",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Interactive Generated Output Display Box
            viewModel.lastGeneratedPrank?.let { prank ->
                item {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = HdBadgeBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, HdPrimaryPurple, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✨ Link Ready to Share! ✨",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = HdPrimaryPurple
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Ye link copy karke ap apne dost \"${prank.friendName}\" ko send kren. Jaise hi wo click krega kbc-land page open ho jaega aur wo prank ho jaega!",
                                style = MaterialTheme.typography.bodySmall,
                                color = HdTextPrimary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Copy Link Field box
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, HdBorderColor, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = prank.generatedUrl,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = HdPrimaryPurple,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val clip = ClipData.newPlainText("KBC Link", prank.generatedUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied in Clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HdSurfaceLight,
                                        contentColor = HdPrimaryPurple
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("📋 Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.simulateRecipientPreview(prank) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HdPrimaryPurple,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(44.dp)
                                ) {
                                    Text("⚡ Run Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Congratulations ${prank.friendName}! Your mobile number has been selected in KBC Draw. Check details here: ${prank.generatedUrl}"
                                            )
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Send via")
                                        context.startActivity(shareIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HdBadgeBg,
                                        contentColor = HdBadgeText
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(44.dp)
                                        .border(1.dp, HdPrimaryPurple, RoundedCornerShape(10.dp))
                                ) {
                                    Text("🔗 Send", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // History Layout Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History & Link Manager 📁",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HdPrimaryPurple
                        )
                    )

                    if (pranksHistory.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearHistory() },
                            colors = ButtonDefaults.textButtonColors(contentColor = KbcCoralAccent)
                        ) {
                            Text("🗑️ Delete All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Render local histories
            if (pranksHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "History is empty",
                                tint = HdTextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No links generated yet.",
                                color = HdTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Format inputs above to start the fun!",
                                color = HdTextSecondary.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                items(pranksHistory, key = { it.id }) { prank ->
                    HistoryItemCard(
                        prank = prank,
                        onDelete = { viewModel.deletePrank(prank) },
                        onSimulate = { viewModel.simulateRecipientPreview(prank) },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("KBC Prank", prank.generatedUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link Copied!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Success dialog prompt styled with light-purple M3 guidelines
    if (showCopyConfirmDialog) {
        Dialog(
            onDismissRequest = { showCopyConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = HdBackground),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(2.dp, HdPrimaryPurple, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(HdPrimaryPurple)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Success check icon",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Link Ready & Copied!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = HdPrimaryPurple
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${viewModel.friendName} ka prank link safaltapoorvak copy ho chuka hai. Is link ko physically copy karke WhatsApp/SMS par send karein:",
                        style = MaterialTheme.typography.bodySmall,
                        color = HdTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Physical link box with selection container so the user can easily select, view and manually copy
                    val generatedLink = viewModel.lastGeneratedPrank?.generatedUrl ?: ""
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HdSurfaceLight,
                            border = BorderStroke(1.dp, HdPrimaryPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = generatedLink,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = HdPrimaryPurple,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("KBC Link", generatedLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied inside clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = HdPrimaryPurple)
                    ) {
                        Text("📋 Click to Copy Link Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCopyConfirmDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HdPrimaryPurple),
                            border = BorderStroke(1.dp, HdPrimaryPurple),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("OK")
                        }

                        Button(
                            onClick = {
                                showCopyConfirmDialog = false
                                val prank = viewModel.lastGeneratedPrank
                                prank?.let { viewModel.simulateRecipientPreview(it) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HdPrimaryPurple,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🔮 Test View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    prank: KbcPrank,
    onDelete: () -> Unit,
    onSimulate: () -> Unit,
    onCopy: () -> Unit
) {
    val dateString = remember(prank.timestamp) {
        val date = Date(prank.timestamp)
        val format = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        format.format(date)
    }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = HdSurfaceLight
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HdBorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Target user icon",
                            tint = HdPrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = prank.friendName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HdTextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Created: $dateString",
                        style = MaterialTheme.typography.bodySmall,
                        color = HdTextSecondary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable specifications row
            if (prank.friendNumber.isNotBlank() || prank.friendAddress.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (prank.friendNumber.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, HdBorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📞 ${prank.friendNumber}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = HdTextSecondary))
                            }
                        }
                    }

                    if (prank.friendAddress.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, HdBorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📍 ${prank.friendAddress}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = HdTextSecondary))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selectable generated url inside History card
            androidx.compose.foundation.text.selection.SelectionContainer {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, HdBorderColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prank.generatedUrl,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = HdPrimaryPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(HdBorderColor)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = HdPrimaryPurple)
                ) {
                    Text("📋 Copy Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSimulate,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HdPrimaryPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("⚡ Test Prank", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}


/**
 * -------------------------------------------------------------
 * RECIPIENT CONGRATS SCREEN (THE WINNER LANDING PAGE)
 * -------------------------------------------------------------
 */
@Composable
fun RecipientCongratsScreen(
    prank: KbcPrank,
    onBackToApp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03010C))
    ) {
        // Deep atmosphere lights
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = KbcPurpleGlow.copy(alpha = 0.12f),
                radius = size.width,
                center = Offset(size.width / 2f, size.height / 3f)
            )
            drawCircle(
                color = KbcMidnightBlue,
                radius = size.width / 1.5f,
                center = Offset(size.width / 2f, size.height / 1.1f)
            )
        }

        // Custom flowing golden particles
        ConfettiRain()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header game stage background image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_kbc_bg),
                    contentDescription = "KBC Stage BG Backdrop",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF03010C))
                            )
                        )
                )

                // Return/Close test indicator (For creators to toggle screen easily)
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = onBackToApp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back back",
                            tint = Color.White
                        )
                    }
                }
            }

            // Congratulations block elements
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "badgeScaleAnim")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "badgeScale animate"
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = KbcMidnightBlue,
                    border = BorderStroke(2.dp, KbcGoldPrimary),
                    modifier = Modifier
                        .scale(scale)
                        .shadow(12.dp, RoundedCornerShape(50), ambientColor = KbcGoldSecondary)
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = "🏆 KBC BIG DRAW WINNER 🏆",
                        color = KbcGoldPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "CONGRATULATIONS!",
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    color = KbcGoldSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                // The prize statement requested by the user:
                // "congratulations you have won INR 25,00,000 from kbc"
                Text(
                    text = "Congratulations! You have won\nINR 25,00,000\nfrom KBC (Kaun Banega Crorepati)!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(KbcPurpleGlow.copy(alpha = 0.25f), KbcRoyalBlue.copy(alpha = 0.25f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.2.dp, KbcGoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Certificate containing parsed attributes (friend's address, name, number)
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = KbcRoyalBlue.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.5.dp, KbcGoldPrimary, RoundedCornerShape(18.dp))
                    .shadow(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "DRAW RECIPIENT PARTICULARS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = KbcGoldSecondary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Serif
                    )

                    Divider(color = KbcGoldPrimary.copy(alpha = 0.25f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    WinnerDetailRow(label = "Winner Name", value = prank.friendName.uppercase())

                    if (prank.friendNumber.isNotBlank()) {
                        WinnerDetailRow(label = "Registered Mobile", value = prank.friendNumber)
                    } else {
                        WinnerDetailRow(label = "Registered Mobile", value = "xxxx-xxx-xxx (Direct Link)")
                    }

                    if (prank.friendAddress.isNotBlank()) {
                        WinnerDetailRow(label = "Registered Address", value = prank.friendAddress)
                    } else {
                        WinnerDetailRow(label = "Registered Address", value = "Online Digital Node")
                    }

                    WinnerDetailRow(label = "Prize Cash Label", value = "INR 25,00,000 /- (Twenty Five Lakh)", valColor = KbcGoldPrimary)
                    WinnerDetailRow(label = "Draw Ref Code", value = "KBC-DRAW-2026", valColor = Color.LightGray.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Portraits Gallery Section (Preloaded custom illustrations of Amitabh, Ambani, and PM Modi)
            Text(
                text = "APPROVED BY KBC TRUST DIRECTORS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = KbcGoldPrimary,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Serif
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amitabh Bachchan
                DignitaryCard(
                    imageResId = R.drawable.img_amitabh,
                    name = "Amitabh Bachchan",
                    title = "KBC Official Host",
                    comment = "Congratulations! Aap 25 Lakh Rupees jeet chuke hain humare special drop me! Bahut-bahut badhaai!"
                )

                // Mukesh & Nita Ambani
                DignitaryCard(
                    imageResId = R.drawable.img_ambani,
                    name = "Mukesh & Nita Ambani",
                    title = "Reliance Jio & KBC Main Sponsors",
                    comment = "Our heartfelt congratulations to you! This event is part of Jio digital award. Your prize is secured."
                )

                // Narendra Modi
                DignitaryCard(
                    imageResId = R.drawable.img_modi,
                    name = "PM Narendra Modi",
                    title = "Special Guest of Honor",
                    comment = "Bhaiyo aur Behno, Digital India ka asar hai ki direct benefit scheme se apko KBC cash prize mila."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Beautiful Native Home Button at the bottom of the congratulations screen for easy return access
            Button(
                onClick = onBackToApp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KbcGoldPrimary,
                    contentColor = KbcMidnightBlue
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = KbcGoldPrimary)
                    .testTag("congrats_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home icon",
                    tint = KbcMidnightBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Go to Home / Back to App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.height(44.dp))
        }
    }
}

@Composable
fun WinnerDetailRow(label: String, value: String, valColor: Color = Color.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.LightGray.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valColor,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun DignitaryCard(
    imageResId: Int,
    name: String,
    title: String,
    comment: String
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = KbcMidnightBlue.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KbcGoldPrimary.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(2.dp, KbcGoldPrimary, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KbcGoldSecondary
                    ),
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "\"$comment\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 14.sp
                    ),
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun ConfettiRain() {
    val infiniteTransition = rememberInfiniteTransition(label = "particlesMain")
    val count = 25
    val particles = remember {
        List(count) {
            ConfettiParticle(
                xPercent = Random.nextFloat(),
                yOffsetMultiplier = Random.nextFloat() + 0.5f,
                speed = Random.nextFloat() * 1.5f + 1f,
                size = Random.nextInt(4, 9).dp,
                color = if (Random.nextBoolean()) KbcGoldPrimary else KbcPurpleGlow
            )
        }
    }

    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val currentY = ((tick * h * p.speed * p.yOffsetMultiplier) % h)
            val currentX = (p.xPercent * w) + (kotlin.math.sin((tick * 5f) + p.xPercent) * 20f)

            drawCircle(
                color = p.color.copy(alpha = 0.65f),
                radius = p.size.toPx(),
                center = Offset(currentX, currentY)
            )
        }
    }
}

data class ConfettiParticle(
    val xPercent: Float,
    val yOffsetMultiplier: Float,
    val speed: Float,
    val size: Dp,
    val color: Color
)
