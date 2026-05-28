package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Transaction
import com.example.data.UserAccount

// Core Aesthetic Color Palette (Professional Polish Theme)
val PhonePayPurple = Color(0xFF4F46E5)       // Indigo 600 Primaries
val PhonePayPurpleLight = Color(0xFFEEF2FF)  // Indigo 50 Ambient
val PhonePayPurpleAccent = Color(0xFF6366F1) // Indigo 500 Accents
val PhonePayEmerald = Color(0xFF16A34A)      // Green 600
val PhonePayEmeraldLight = Color(0xFFDCFCE7) // Green 100
val AlertRed = Color(0xFFF97316)             // Safety Orange (per theme style instructions)
val AlertRedLight = Color(0xFFFFF7ED)        // Warm Orange Ambient
val WarningOrange = Color(0xFFEA580C)        // Deep Orange
val DarkNavy = Color(0xFF0F172A)             // Slate 900 Neutral text
val GraySubtitle = Color(0xFF64748B)         // Slate 500 Metadata
val LightSurface = Color(0xFFF3F4F9)         // Main Window background (#F3F4F9)

// Preset mocked lists of contacts to speed up sending payments
data class MockContact(val name: String, val upiId: String, val phone: String, val initial: String, val color: Color)

val PRESET_CONTACTS = listOf(
    MockContact("Jack Mercer", "jackmercer@upi", "+91 98455 01010", "JM", Color(0xFFF44336)),
    MockContact("Aria Chen", "ariachen@axl", "+91 97412 28415", "AC", Color(0xFF9C27B0)),
    MockContact("Vikram Rathore", "vikram@ybl", "+91 99004 55301", "VR", Color(0xFF009688)),
    MockContact("Sanya Patel", "spatel@upi", "+91 91522 66321", "SP", Color(0xFF3F51B5)),
    MockContact("Rajesh Kumar", "rajeshk@okicici", "+91 81234 56789", "RK", Color(0xFF4CAF50))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PaymentViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    if (currentScreen == AppScreen.Login) {
        LoginScreen(viewModel = viewModel)
        return
    }
    if (currentScreen == AppScreen.SignUp) {
        SignUpScreen(viewModel = viewModel)
        return
    }

    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showSendMoneySheet by remember { mutableStateOf(false) }
    var showAddFundsDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showUpiPinDialog by remember { mutableStateOf(false) }
    var showTransactionsHelp by remember { mutableStateOf(false) }

    // Forms fields state
    var recipientName by remember { mutableStateOf("") }
    var recipientUpi by remember { mutableStateOf("") }
    var payAmountString by remember { mutableStateOf("") }

    // PIN state
    var inputPinString by remember { mutableStateOf("") }

    // Track active payments for transaction status warning banner
    val activeFrozenTransfers = remember(transactions) {
        transactions.filter { it.status == "FROZEN" }
    }
    val totalFrozenAmount = remember(activeFrozenTransfers) {
        activeFrozenTransfers.sumOf { it.amount }
    }

    // Toggle balance visibility
    var isBalanceVisible by remember { mutableStateOf(true) }

    // Handle incoming notification or errors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = LightSurface,
        topBar = {
            // Elegant customized PhonePe Deep Purple app header with Professional Polish curve
            TopAppBar(
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PhonePayPurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "s payments",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = if (userAccount?.isTestAccount == true) "Test Account Mode • Sandbox Funds" else "Real Bank Connection • Verified Secure",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .size(38.dp)
                            .border(1.5.dp, Color(0xFFC7D2FE), CircleShape) // Indigo 300
                            .clip(CircleShape)
                            .background(Color(0xFF818CF8)) // Indigo 400
                            .clickable { showAddFundsDialog = true }
                            .testTag("user_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userAccount?.username?.take(2)?.uppercase() ?: "JD",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTransactionsHelp = true },
                        modifier = Modifier.testTag("help_icon_btn")
                    ) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Help")
                    }
                    IconButton(
                        onClick = { viewModel.resetDemoState() },
                        modifier = Modifier.testTag("refresh_demo_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Demo")
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_icon_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Balance Details Panel
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("balance_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)), // Slate 200 border
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = PhonePayPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Primary Bank Balance",
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkNavy,
                                    fontSize = 15.sp
                                )
                            }

                            Icon(
                                imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Balance",
                                tint = GraySubtitle,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { isBalanceVisible = !isBalanceVisible }
                                    .testTag("toggle_balance_btn")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Amount display
                        Text(
                            text = if (isBalanceVisible) "₹${String.format("%.2f", userAccount?.balance ?: 5500.0)}" else "••••••",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PhonePayPurple,
                            style = MaterialTheme.typography.headlineLarge
                        )

                        // If any frozen amounts exist, display banner warning inside balance card
                        if (totalFrozenAmount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AlertRedLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Frozen Warning",
                                        tint = AlertRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⚠️ ₹${String.format("%.1f", totalFrozenAmount)} is frozen in Escrow protector. Tap 'Oops, wrong payment' below to refund!",
                                        fontSize = 11.5.sp,
                                        color = AlertRed,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transfer Money Core grid (PhonePe look-alike)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)), // Slate 200 border
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            "Transfer Money",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            TransferGridItem(
                                icon = Icons.Default.PhoneAndroid,
                                label = "To Mobile",
                                contentDescription = "To Mobile Icon",
                                onClick = {
                                    recipientName = ""
                                    recipientUpi = ""
                                    payAmountString = ""
                                    showSendMoneySheet = true
                                },
                                testTag = "pay_mobile_btn"
                            )

                            TransferGridItem(
                                icon = Icons.Default.AccountBalance,
                                label = "To Bank/UPI",
                                contentDescription = "To Bank/UPI icon",
                                onClick = {
                                    recipientName = ""
                                    recipientUpi = ""
                                    payAmountString = ""
                                    showSendMoneySheet = true
                                },
                                testTag = "pay_bank_btn"
                            )

                            TransferGridItem(
                                icon = Icons.Default.AccountCircle,
                                label = "To Self",
                                contentDescription = "Self Account Transfer Icon",
                                onClick = {
                                    recipientName = "My Self Account"
                                    recipientUpi = "self@saving"
                                    payAmountString = "1000"
                                    showSendMoneySheet = true
                                },
                                testTag = "pay_self_btn"
                            )

                            TransferGridItem(
                                icon = Icons.Default.Savings,
                                label = "Add Funds",
                                contentDescription = "Check Bank Balance Icon",
                                onClick = { showAddFundsDialog = true },
                                testTag = "add_funds_shortcut"
                            )
                        }
                    }
                }
            }

            // Escrow explanation banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PhonePayPurpleLight),
                    border = BorderStroke(1.dp, PhonePayPurple.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Shield Guard",
                            tint = PhonePayPurple,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "PhonePay SafeShield Active",
                                fontWeight = FontWeight.Bold,
                                color = PhonePayPurple,
                                fontSize = 13.sp
                            )
                            Text(
                                "Any sent amount is securely frozen for 15 mins. If you transfer in error, tap 'Oops!' below to pull the coins back instantly.",
                                color = DarkNavy.copy(alpha = 0.8f),
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Time Machine simulation controller
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)), // Slate 200 border
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timelapse,
                                contentDescription = "Timer Machine",
                                tint = WarningOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Escrow Demo Time Machine",
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            "Simulate aging. In actual use, escrow updates over real-world seconds. Tap below to accelerate virtual elapsed clocks:",
                            color = GraySubtitle,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.fastForwardTime(5) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ff_5mins_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("+5 Mins Aged", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.fastForwardTime(15) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ff_15mins_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("+15 Mins Expirer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Transactions Log",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkNavy
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(PhonePayEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "SQLite Database Active (Room Persistent)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PhonePayEmerald
                             )
                        }
                    }

                    Text(
                        "${transactions.size} records",
                        fontSize = 12.sp,
                        color = GraySubtitle
                    )
                }
            }

            // Fallback for Empty State
            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("empty_state_card"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Empty Log",
                                tint = PhonePayPurple.copy(alpha = 0.3f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "No Transactions Yet",
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Make your first PhonePay protected payment above to witness real-time escrow protection in active play!",
                                color = GraySubtitle,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Transactions list items with timers and buttons
            items(transactions, key = { it.id }) { tx ->
                TransactionListItem(
                    tx = tx,
                    currentTime = currentTime,
                    onReverseClick = { viewModel.reversePayment(tx) },
                    onCompleteClick = { viewModel.forceFinishPayment(tx) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // POPUPS IMPLEMENTATION

        // --- Bottom Sheet / Dialog for sending funds ---
        if (showSendMoneySheet) {
            Dialog(
                onDismissRequest = { showSendMoneySheet = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(top = 48.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        // Header bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Send protected payment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PhonePayPurple
                            )
                            IconButton(onClick = { showSendMoneySheet = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Quick Recipient presets selection rows
                        Text(
                            "Quick Presets contacts:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GraySubtitle
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PRESET_CONTACTS.forEach { c ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            recipientName = c.name
                                            recipientUpi = c.upiId
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(c.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(c.initial, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        c.name.substringBefore(" "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Input Forms
                        OutlinedTextField(
                            value = recipientName,
                            onValueChange = { recipientName = it },
                            label = { Text("Recipient Full Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_name_input"),
                            shape = RoundedCornerShape(8.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = recipientUpi,
                            onValueChange = { recipientUpi = it },
                            label = { Text("Receiver UPI ID / Phone") },
                            placeholder = { Text("example@upi") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_upi_input"),
                            shape = RoundedCornerShape(8.dp),
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = payAmountString,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                                    payAmountString = it
                                }
                            },
                            label = { Text("Amount (₹)") },
                            prefix = { Text("₹") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_amount_input"),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Escrow notice block
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PhonePayPurpleLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Safe Shield icon",
                                    tint = PhonePayPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Safeshield lock applies: In case of wrong entries or a scam, you get 15 entire minutes to undo and restore funds.",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = DarkNavy
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                val amtNum = payAmountString.toDoubleOrNull() ?: 0.0
                                if (recipientName.isBlank() || recipientUpi.isBlank() || amtNum <= 0) {
                                    // Let viewModel handle error message
                                    viewModel.makePayment("", "", 0.0)
                                } else {
                                    // Go to simulated secure PIN entry dialog!
                                    showUpiPinDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("pay_securely_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = PhonePayPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "PAY ₹${if (payAmountString.isNotBlank()) payAmountString else "0"} SECURELY",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- Custom Secure UPI PIN Pad Overlay ---
        if (showUpiPinDialog) {
            Dialog(
                onDismissRequest = { showUpiPinDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E2830) // Dark Slate bank style background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bank banner header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Unified Payments Interface",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "SECURE BANKING PORTAL",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            // Bank generic logo placeholder
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    "NPCI",
                                    color = Color.White,
                                    modifier = Modifier.padding(4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Tx specs
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Paying to:", color = Color.Gray, fontSize = 13.sp)
                                    Text(recipientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("UPI ID:", color = Color.Gray, fontSize = 13.sp)
                                    Text(recipientUpi, color = Color.White, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("AMOUNT:", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("₹$payAmountString", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            "ENTER 4-DIGIT UPI PIN",
                            color = Color.White,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // PIN Input bubbles indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 4) {
                                val filled = i < inputPinString.length
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (filled) Color.White else Color.White.copy(alpha = 0.15f))
                                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Secure Custom Numeric Pad
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val rows = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("X", "0", "✔")
                            )

                            rows.forEach { rowList ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowList.forEach { char ->
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (char == "✔") PhonePayEmerald else if (char == "X") AlertRed.copy(
                                                        alpha = 0.3f
                                                    ) else Color.White.copy(alpha = 0.08f)
                                                )
                                                .clickable {
                                                    when (char) {
                                                        "X" -> {
                                                            if (inputPinString.isNotEmpty()) {
                                                                inputPinString = inputPinString.dropLast(1)
                                                            }
                                                        }

                                                        "✔" -> {
                                                            if (inputPinString.length == 4) {
                                                                // Submit!
                                                                viewModel.makePayment(
                                                                    recipientName,
                                                                    recipientUpi,
                                                                    payAmountString.toDoubleOrNull() ?: 0.0
                                                                )
                                                                showUpiPinDialog = false
                                                                showSendMoneySheet = false
                                                                inputPinString = ""
                                                            }
                                                        }

                                                        else -> {
                                                            if (inputPinString.length < 4) {
                                                                inputPinString += char
                                                            }
                                                        }
                                                    }
                                                }
                                                .testTag("pin_key_$char"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (char == "X") {
                                                Icon(Icons.Default.Backspace, contentDescription = "Back", tint = Color.White)
                                            } else if (char == "✔") {
                                                Icon(Icons.Default.Done, contentDescription = "Done", tint = Color.White)
                                            } else {
                                                Text(char, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // --- Demo Add/Withdraw Funds Dialog ---
        if (showAddFundsDialog) {
            var actionTab by remember { mutableStateOf("DEPOSIT") } // "DEPOSIT" or "WITHDRAW"
            var transferAmountText by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showAddFundsDialog = false },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Linked Bank Account Simulator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkNavy
                        )
                        Text(
                            text = "Automated deposits & instant withdrawals",
                            style = MaterialTheme.typography.labelSmall,
                            color = GraySubtitle
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Custom tab bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { actionTab = "DEPOSIT" }
                                    .background(if (actionTab == "DEPOSIT") PhonePayPurple else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Deposit",
                                    color = if (actionTab == "DEPOSIT") Color.White else GraySubtitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { actionTab = "WITHDRAW" }
                                    .background(if (actionTab == "WITHDRAW") PhonePayPurple else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Withdraw",
                                    color = if (actionTab == "WITHDRAW") Color.White else GraySubtitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Connected Bank Details Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Bank",
                                    tint = PhonePayPurpleAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (userAccount?.isTestAccount == true) "State Bank Sandbox" else "Verified Real Bank Link",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = DarkNavy
                                    )
                                    Text(
                                        text = "Account: SB-XXXXXX7842 (Connected)",
                                        fontSize = 10.sp,
                                        color = GraySubtitle
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = transferAmountText,
                            onValueChange = { transferAmountText = it },
                            label = { Text("Enter Amount (₹)") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                        )

                        // Quick pre-fills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(500, 2000, 5000).forEach { amt ->
                                OutlinedButton(
                                    onClick = { transferAmountText = amt.toString() },
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text("₹$amt", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (actionTab == "DEPOSIT") {
                            Button(
                                onClick = {
                                    val amt = transferAmountText.toDoubleOrNull() ?: 0.0
                                    viewModel.depositFunds(amt)
                                    showAddFundsDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("action_deposit_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = PhonePayEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("DEPOSIT SECURELY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val amt = transferAmountText.toDoubleOrNull() ?: 0.0
                                    viewModel.withdrawFunds(amt)
                                    showAddFundsDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("action_withdraw_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = PhonePayPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("WITHDRAW INSTANTLY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddFundsDialog = false }) {
                        Text("Close", color = Color.Gray)
                    }
                }
            )
        }

        // --- Core Transactions Help dialog ---
        if (showTransactionsHelp) {
            AlertDialog(
                onDismissRequest = { showTransactionsHelp = false },
                title = { Text("SafePay Verification Guide") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "This system mimics PhonePay but adds an active Escrow protection buffer of 15 minutes.",
                            fontWeight = FontWeight.Bold
                        )
                        Text("How it protects transfer transactions:")
                        Text("1. Send a payment to any contact. Your primary balance is instantly debited so funds are reserved.")
                        Text("2. The system flags the item as 'FROZEN' for exactly 15 minutes. It will feature a countdown ticker.")
                        Text("3. If you clicks 'Oops, I made a wrong payment', the amount returns to your wallet and the status updates to 'REVERSED'.")
                        Text("4. Once 15 mins lapse (simulation accelerates this), the funds transfer becomes 'COMPLETED' permanently and can no longer be reversed.")
                    }
                },
                confirmButton = {
                    Button(onClick = { showTransactionsHelp = false }) {
                        Text("Got it!")
                    }
                }
            )
        }
    }
}

@Composable
fun TransferGridItem(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)) // Slate 200 border
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = PhonePayPurple, // Indigo 600
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569) // Slate 600
        )
    }
}

@Composable
fun TransactionListItem(
    tx: Transaction,
    currentTime: Long,
    onReverseClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    // Escrow holding specifications
    val elapsed = currentTime - tx.timestamp
    val maxHoldingMs = 15 * 60 * 1000L
    val remainingMs = maxHoldingMs - elapsed

    val isStillFrozen = tx.status == "FROZEN" && remainingMs > 0

    // Card border and visual indicators change based on transaction escrow protection state
    val cardBorder = when {
        tx.status == "REVERSED" -> BorderStroke(1.dp, Color(0xFFE2E8F0)) // slate-200
        isStillFrozen -> BorderStroke(1.dp, Color(0xFFFED7AA)) // orange-100
        else -> BorderStroke(1.dp, Color(0xFFE2E8F0)) // slate-200
    }

    val cardBackground = when {
        tx.status == "REVERSED" -> Color(0xFFF8FAFC) // Slate 50
        isStillFrozen -> Color.White
        else -> Color.White
    }

    val initialColor = when {
        tx.isIncoming -> PhonePayEmerald
        tx.status == "REVERSED" -> Color(0xFF94A3B8) // Slate 400
        else -> PhonePayPurpleAccent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${tx.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(24.dp), // Polished extra rounded corner (24.dp)
        border = cardBorder
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Optional: Absolute badge at the top-right for safety hold active
            if (isStillFrozen) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 24.dp),
                    color = Color(0xFFFFEDD5), // orange-100
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "SAFETY NET ACTIVE",
                        color = Color(0xFFC2410C), // orange-700
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Contact avatar placeholder with initial
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(initialColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tx.recipientName.length >= 2) tx.recipientName.take(2).uppercase() else "TX",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (tx.isIncoming) "From: ${tx.recipientName}" else "To: ${tx.recipientName}",
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tx.recipientUpi,
                                color = GraySubtitle,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Cost Amount label
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = (if (tx.isIncoming) "+ ₹" else "- ₹") + String.format("%.2f", tx.amount),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (tx.isIncoming) PhonePayEmerald else if (tx.status == "REVERSED") Color.Gray else DarkNavy,
                            fontSize = 17.sp
                        )
                        
                        Text(
                            text = if (tx.isIncoming) "Credit" else "Debit",
                            fontSize = 10.sp,
                            color = GraySubtitle
                        )
                    }
                }

                // Escrow protection indicators
                AnimatedVisibility(
                    visible = isStillFrozen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        // Slate 50 Countdown box per the HTML mockup design
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC), // Slate 50
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timelapse,
                                        contentDescription = "Timer Icon",
                                        tint = PhonePayPurpleAccent, // indigo-500
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    
                                    val secondsTotal = (remainingMs / 1000).toInt()
                                    val m = secondsTotal / 60
                                    val s = secondsTotal % 60
                                    val countdownLabel = "Releasing in ${m}:${String.format("%02d", s)} min"
                                    
                                    Text(
                                        text = countdownLabel,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155), // Slate 700
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.testTag("item_countdown_${tx.id}")
                                    )
                                }
                                
                                val progressFraction = animateFloatAsState(
                                    targetValue = (remainingMs.toFloat() / maxHoldingMs.toFloat()).coerceIn(0f, 1f),
                                    label = "ProgressBar"
                                )
                                LinearProgressIndicator(
                                    progress = { progressFraction.value },
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = PhonePayPurple, // indigo-600/700
                                    trackColor = Color(0xFFE2E8F0) // slate-200
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Refund or Settle Actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onReverseClick,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("btn_wrong_payment_${tx.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed), // Orange 500
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(14.dp), // rounded-2xl style,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancel Icon",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "OOPS, WRONG PAYMENT?",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = onCompleteClick,
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(44.dp)
                                    .testTag("btn_complete_${tx.id}"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155)),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)), // slate-300
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Settle Now", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        
                        Text(
                            text = "Click to cancel & credit back instantly",
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8), // slate-400
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                    }
                }

            AnimatedVisibility(
                visible = tx.status == "COMPLETED" || (tx.status == "FROZEN" && !isStillFrozen),
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(PhonePayEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Checked",
                                tint = Color.White,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tx.isIncoming) "Credited directly/Settled" else "Completed • Sent successfully to Bank",
                            color = PhonePayEmerald,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = tx.status == "REVERSED",
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Recalled",
                                tint = Color.White,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wrong payment recalled • Safety refund completed",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
  }
}
