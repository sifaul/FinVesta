package com.example.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.security.BiometricAuthManager
import com.example.data.security.BiometricStatus
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.DarkBg
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose500
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PIN_LENGTH = 6

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
private fun MinimalBackspaceIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Geometric outline backspace tag matching user reference image
        val path = Path().apply {
            moveTo(w * 0.30f, h * 0.16f)
            lineTo(w * 0.90f, h * 0.16f)
            quadraticTo(w * 0.98f, h * 0.16f, w * 0.98f, h * 0.25f)
            lineTo(w * 0.98f, h * 0.75f)
            quadraticTo(w * 0.98f, h * 0.84f, w * 0.90f, h * 0.84f)
            lineTo(w * 0.30f, h * 0.84f)
            lineTo(w * 0.06f, h * 0.50f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Clean 'X' mark inside
        val cx = w * 0.62f
        val cy = h * 0.50f
        val crossR = w * 0.13f
        drawLine(
            color = color,
            start = Offset(cx - crossR, cy - crossR),
            end = Offset(cx + crossR, cy + crossR),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx - crossR, cy + crossR),
            end = Offset(cx + crossR, cy - crossR),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun KeypadCircleButton(
    digit: String,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBg = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1B4441),
                Color(0xFF113230)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFC7F5E5),
                Color(0xFFA5ECD0)
            )
        )
    }

    val buttonBorder = BorderStroke(
        width = 1.dp,
        color = if (isDark) Color(0xFF34D399).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.75f)
    )

    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F4D3C)

    Box(
        modifier = modifier
            .size(76.dp)
            .shadow(
                elevation = 5.dp,
                shape = CircleShape,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF065F46).copy(alpha = 0.25f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF065F46).copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .background(buttonBg)
            .border(buttonBorder, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = true,
                    radius = 38.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFF065F46).copy(alpha = 0.2f)
                ),
                onClick = onClick
            )
            .testTag("keypad_btn_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BiometricLockScreen(
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val scope = rememberCoroutineScope()

    var biometricStatus by remember { mutableStateOf(BiometricAuthManager.checkBiometricAvailability(context)) }
    val modality = remember { BiometricAuthManager.getAvailableBiometricModalities(context) }
    var enteredPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var authFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var recoveryMethodTab by remember { mutableIntStateOf(0) } // 0: Email OTP, 1: Security Question
    var recoveryStep by remember { mutableIntStateOf(0) } // 0: Verify identity, 1: Set new PIN
    var recoveryEmailInput by remember { mutableStateOf(BiometricAuthManager.getRecoveryEmail(context)) }
    var recoveryOtpInput by remember { mutableStateOf("") }
    var otpSentMessage by remember { mutableStateOf<String?>(null) }
    var securityAnswerInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var recoveryErrorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (showForgotPinDialog) {
            showForgotPinDialog = false
            recoveryErrorMessage = null
            recoveryOtpInput = ""
        } else {
            (context as? android.app.Activity)?.moveTaskToBack(true)
        }
    }

    // Shake animation for incorrect PIN
    val shakeOffset = remember { Animatable(0f) }

    // Pulse animation for biometric scanner button
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val fingerPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "finger_scale"
    )

    // Subtle aurora glow transition
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    fun triggerBiometric() {
        authFeedbackMessage = null
        val targetActivity = activity ?: context.findFragmentActivity()
        if (targetActivity == null) {
            authFeedbackMessage = "Tidak dapat mengakses modul biometrik perangkat."
            return
        }

        val currentStatus = BiometricAuthManager.checkBiometricAvailability(context)
        biometricStatus = currentStatus

        if (currentStatus == BiometricStatus.NO_HARDWARE) {
            authFeedbackMessage = "Perangkat tidak memiliki sensor biometrik. Gunakan PIN."
            return
        }

        BiometricAuthManager.promptBiometric(
            activity = targetActivity,
            title = "Autentikasi FinVesta",
            subtitle = "Pindai sidik jari atau wajah untuk membuka brankas keuangan Anda",
            negativeButtonText = "Gunakan PIN",
            onSuccess = {
                authFeedbackMessage = null
                onUnlockSuccess()
            },
            onError = { code, err ->
                when (code) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        authFeedbackMessage = null
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        authFeedbackMessage = "Biometrik belum didaftarkan di perangkat. Gunakan PIN."
                    }
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                        authFeedbackMessage = "Sensor biometrik sedang tidak siap. Masukkan PIN."
                    }
                    BiometricPrompt.ERROR_LOCKOUT -> {
                        authFeedbackMessage = "Terlalu banyak percobaan gagal. Silakan masukkan PIN 6 digit."
                    }
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        authFeedbackMessage = "Sensor biometrik terkunci permanen. Gunakan PIN 6 digit."
                    }
                    else -> {
                        val message = err.toString()
                        if (!message.contains("cancel", ignoreCase = true) && !message.contains("PIN", ignoreCase = true)) {
                            authFeedbackMessage = message
                        }
                    }
                }
            },
            onFailed = {
                authFeedbackMessage = "Sidik jari atau wajah tidak cocok. Coba lagi atau gunakan PIN."
            }
        )
    }

    LaunchedEffect(Unit) {
        if (biometricStatus == BiometricStatus.AVAILABLE) {
            triggerBiometric()
        }
    }

    fun handleDigitPress(digit: String) {
        if (enteredPin.length < PIN_LENGTH) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            isPinError = false
            authFeedbackMessage = null

            if (newPin.length == PIN_LENGTH) {
                scope.launch {
                    delay(80)
                    if (BiometricAuthManager.verifyPin(context, newPin)) {
                        onUnlockSuccess()
                    } else {
                        isPinError = true
                        authFeedbackMessage = "PIN tidak sesuai. Silakan coba kembali."
                        shakeOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 400
                                -22f at 50
                                22f at 100
                                -16f at 150
                                16f at 200
                                -10f at 250
                                10f at 300
                                -4f at 350
                                0f at 400
                            }
                        )
                        delay(250)
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isPinError = false
            authFeedbackMessage = null
        }
    }

    val isDark = MaterialTheme.colorScheme.background == DarkBg || MaterialTheme.colorScheme.surface.hashCode() < 0xFF303030

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF0F3634),
                            Color(0xFF0A2226),
                            Color(0xFF06141C)
                        )
                    } else {
                        listOf(
                            Color(0xFFD4F8EB),
                            Color(0xFFAEF3DA),
                            Color(0xFF7DE6C2)
                        )
                    }
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Overhead radiant ambient glow effect matching user reference image
        Box(
            modifier = Modifier
                .size(420.dp)
                .offset(y = (-150).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Emerald500.copy(alpha = 0.32f * auraAlpha),
                                Cyan500.copy(alpha = 0.15f * auraAlpha),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.75f * auraAlpha),
                                Color(0xFFE6FAF2).copy(alpha = 0.35f * auraAlpha),
                                Color.Transparent
                            )
                        }
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flexible top spacer to keep the header gracefully raised
            Spacer(modifier = Modifier.weight(0.35f))

            // ================= TOP / TITLE & DOTS SECTION =================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Official FinVesta Logo Branding
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.size(70.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_finvesta_logo),
                        contentDescription = "Logo FinVesta",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val hasConfiguredPin = remember(context) { BiometricAuthManager.getBackupPin(context).isNotEmpty() }
                
                Text(
                    text = if (hasConfiguredPin) "Enter 6-digit PIN" else "Create a 6-digit PIN",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                    letterSpacing = 0.2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (biometricStatus.isReady) {
                        if (modality.hasFace && modality.hasFingerprint) {
                            "Pindai sidik jari, wajah, atau masukkan PIN"
                        } else if (modality.hasFace) {
                            "Gunakan Face Unlock atau masukkan PIN"
                        } else {
                            "Sentuh sensor sidik jari atau masukkan PIN"
                        }
                    } else {
                        "Masukkan 6 digit kode PIN brankas keuangan"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF0F4D3C).copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ================= 6 PIN INDICATOR DOTS =================
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
                        .padding(vertical = 3.dp)
                ) {
                    for (i in 0 until PIN_LENGTH) {
                        val isFilled = i < enteredPin.length

                        val dotScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.15f else 1.0f,
                            animationSpec = tween(120),
                            label = "dot_scale_$i"
                        )

                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isPinError -> Rose500
                                isFilled -> Color.White
                                else -> if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.65f)
                            },
                            animationSpec = tween(120),
                            label = "dot_color_$i"
                        )

                        val dotBorderColor by animateColorAsState(
                            targetValue = when {
                                isPinError -> Rose500
                                isFilled -> Color.White
                                else -> if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF065F46).copy(alpha = 0.3f)
                            },
                            animationSpec = tween(120),
                            label = "dot_border_$i"
                        )

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .scale(dotScale)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(BorderStroke(1.2.dp, dotBorderColor), CircleShape)
                                .testTag("pin_dot_$i")
                        )
                    }
                }

                // Error or Status message banner (tight and animated)
                androidx.compose.animation.AnimatedVisibility(
                    visible = authFeedbackMessage != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.95f),
                    exit = fadeOut() + scaleOut(targetScale = 0.95f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPinError) Rose500.copy(alpha = 0.14f) else Emerald500.copy(alpha = 0.14f),
                        border = BorderStroke(
                            1.dp,
                            if (isPinError) Rose500.copy(alpha = 0.35f) else Emerald500.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPinError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isPinError) Rose500 else Emerald500,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = authFeedbackMessage ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPinError) Rose500 else if (isDark) Emerald400 else Color(0xFF065F46)
                            )
                        }
                    }
                }
            }

            // Flexible breathing space between PIN dots and keypad
            Spacer(modifier = Modifier.weight(0.65f))

            // ================= NUMERIC KEYPAD MATCHING REFERENCE IMAGE =================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (row in keypadRows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(26.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (digit in row) {
                                KeypadCircleButton(
                                    digit = digit,
                                    isDark = isDark,
                                    onClick = { handleDigitPress(digit) }
                                )
                            }
                        }
                    }

                    // Row 4: [Biometric], [0], [Backspace]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(26.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slot 1: Biometric scanner button (always rendered for accessibility & direct interaction)
                        val isBiometricReady = biometricStatus.isReady
                        val biometricIconTint = when {
                            isBiometricReady -> if (isDark) Emerald400 else Color(0xFF0F4D3C)
                            else -> if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF0F4D3C).copy(alpha = 0.35f)
                        }

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isBiometricReady) {
                                        if (isDark) Emerald500.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.45f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isBiometricReady) {
                                            if (isDark) Emerald500.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.6f)
                                        } else {
                                            if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFF0F4D3C).copy(alpha = 0.1f)
                                        }
                                    ),
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 38.dp,
                                        color = Emerald500.copy(alpha = 0.3f)
                                    ),
                                    onClick = { triggerBiometric() }
                                )
                                .testTag("keypad_btn_biometric"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (modality.hasFace && modality.hasFingerprint) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.scale(if (isBiometricReady) fingerPulse else 1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Pindai Biometrik",
                                        tint = biometricIconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Face Unlock",
                                        tint = biometricIconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else if (modality.hasFace && !modality.hasFingerprint) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Face Unlock",
                                    tint = biometricIconTint,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .scale(if (isBiometricReady) fingerPulse else 1f)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Pindai Sidik Jari",
                                    tint = biometricIconTint,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .scale(if (isBiometricReady) fingerPulse else 1f)
                                )
                            }
                        }

                        // Slot 2: '0' button
                        KeypadCircleButton(
                            digit = "0",
                            isDark = isDark,
                            onClick = { handleDigitPress("0") }
                        )

                        // Slot 3: Minimalist Outline Backspace Button matching reference image
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 38.dp,
                                        color = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFF065F46).copy(alpha = 0.2f)
                                    ),
                                    enabled = enteredPin.isNotEmpty(),
                                    onClick = { handleBackspace() }
                                )
                                .testTag("keypad_btn_backspace"),
                            contentAlignment = Alignment.Center
                        ) {
                            val backspaceColor = if (enteredPin.isNotEmpty()) {
                                if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFF0F172A).copy(alpha = 0.25f)
                            }

                            MinimalBackspaceIcon(
                                color = backspaceColor,
                                modifier = Modifier.size(32.dp, 24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Elegant 'Lupa PIN?' Action
                TextButton(
                    onClick = {
                        showForgotPinDialog = true
                        recoveryMethodTab = 0
                        recoveryStep = 0
                        recoveryEmailInput = BiometricAuthManager.getRecoveryEmail(context)
                        recoveryOtpInput = ""
                        otpSentMessage = null
                        securityAnswerInput = ""
                        newPinInput = ""
                        confirmPinInput = ""
                        recoveryErrorMessage = null
                    },
                    modifier = Modifier.testTag("forgot_pin_button")
                ) {
                    Text(
                        text = "Lupa PIN?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Emerald400.copy(alpha = 0.9f) else Color(0xFF065F46).copy(alpha = 0.85f)
                    )
                }
            }

            // Flexible bottom spacer for comfortable keypad placement
            Spacer(modifier = Modifier.weight(0.35f))
        }
    }

    // ================= MODERN RESET PIN / RECOVERY DIALOG =================
    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Emerald500.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (recoveryStep == 0) Icons.Default.Security else Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (recoveryStep == 0) "Pemulihan Akses PIN" else "Buat PIN 6 Digit Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (recoveryStep == 0) {
                        Text(
                            text = "Verifikasi identitas Anda untuk mengamankan dan mengatur ulang PIN FinVesta:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Segmented Tab Selector
                        TabRow(
                            selectedTabIndex = recoveryMethodTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            contentColor = Emerald500,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[recoveryMethodTab]),
                                    color = Emerald500
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                        ) {
                            Tab(
                                selected = recoveryMethodTab == 0,
                                onClick = {
                                    recoveryMethodTab = 0
                                    recoveryErrorMessage = null
                                },
                                text = {
                                    Text("Email OTP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                },
                                modifier = Modifier.testTag("tab_recovery_email")
                            )
                            Tab(
                                selected = recoveryMethodTab == 1,
                                onClick = {
                                    recoveryMethodTab = 1
                                    recoveryErrorMessage = null
                                },
                                text = {
                                    Text("Pertanyaan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                },
                                modifier = Modifier.testTag("tab_recovery_question")
                            )
                        }

                        if (recoveryMethodTab == 0) {
                            // METODE EMAIL OTP
                            OutlinedTextField(
                                value = recoveryEmailInput,
                                onValueChange = { recoveryEmailInput = it },
                                label = { Text("Email Terdaftar") },
                                singleLine = true,
                                shape = CircleShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recovery_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald500,
                                    focusedLabelColor = Emerald500
                                )
                            )

                            Button(
                                onClick = {
                                    recoveryErrorMessage = null
                                    val otp = BiometricAuthManager.generateAndSendRecoveryOtp(context, recoveryEmailInput)
                                    otpSentMessage = "Kode verifikasi 6 digit telah dikirim ke $recoveryEmailInput & notifikasi HP!"
                                    Toast.makeText(context, "Kode OTP FinVesta: $otp", Toast.LENGTH_SHORT).show()
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_otp_button")
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kirim Kode Verifikasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (otpSentMessage != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald500.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Emerald500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = otpSentMessage ?: "",
                                            fontSize = 11.sp,
                                            color = Emerald500,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = recoveryOtpInput,
                                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) recoveryOtpInput = it },
                                    label = { Text("Kode OTP (6 Digit)") },
                                    placeholder = { Text("123456") },
                                    singleLine = true,
                                    shape = CircleShape,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("recovery_otp_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald500,
                                        focusedLabelColor = Emerald500
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (BiometricAuthManager.verifyRecoveryOtp(context, recoveryOtpInput)) {
                                            recoveryStep = 1
                                            recoveryErrorMessage = null
                                        } else {
                                            recoveryErrorMessage = "Kode OTP tidak sesuai atau telah kedaluwarsa."
                                        }
                                    },
                                    enabled = recoveryOtpInput.length == 6,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("verify_otp_button")
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verifikasi & Lanjut Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                            }
                        } else {
                            // METODE PERTANYAAN KEAMANAN
                            val question = BiometricAuthManager.getSecurityQuestion(context)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = Emerald500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pertanyaan Keamanan:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald500
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = question,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = securityAnswerInput,
                                onValueChange = { securityAnswerInput = it },
                                label = { Text("Jawaban Anda") },
                                placeholder = { Text("Masukkan jawaban...") },
                                singleLine = true,
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("security_answer_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald500,
                                    focusedLabelColor = Emerald500
                                )
                            )

                            Button(
                                onClick = {
                                    if (BiometricAuthManager.verifySecurityAnswer(context, securityAnswerInput)) {
                                        recoveryStep = 1
                                        recoveryErrorMessage = null
                                    } else {
                                        recoveryErrorMessage = "Jawaban keamanan tidak cocok. Silakan coba kembali."
                                    }
                                },
                                enabled = securityAnswerInput.isNotBlank(),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("verify_answer_button")
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verifikasi Jawaban", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // TAHAP 1: ATUR ULANG PIN
                        Text(
                            text = "Identitas terverifikasi. Masukkan 6 digit kode PIN baru untuk mengamankan akun:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                            label = { Text("PIN Baru (6 Angka)") },
                            singleLine = true,
                            shape = CircleShape,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald500,
                                focusedLabelColor = Emerald500
                            )
                        )

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPinInput = it },
                            label = { Text("Konfirmasi PIN Baru (6 Angka)") },
                            singleLine = true,
                            shape = CircleShape,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_new_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald500,
                                focusedLabelColor = Emerald500
                            )
                        )

                        Button(
                            onClick = {
                                if (newPinInput.length != 6) {
                                    recoveryErrorMessage = "PIN harus terdiri dari tepat 6 angka."
                                } else if (newPinInput != confirmPinInput) {
                                    recoveryErrorMessage = "Konfirmasi PIN tidak cocok dengan PIN baru."
                                } else {
                                    BiometricAuthManager.resetPin(context, newPinInput)
                                    enteredPin = newPinInput
                                    Toast.makeText(context, "PIN Baru Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                                    showForgotPinDialog = false
                                    scope.launch {
                                        delay(150)
                                        onUnlockSuccess()
                                    }
                                }
                            },
                            enabled = newPinInput.length == 6 && confirmPinInput.length == 6,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_new_pin_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Recovery Error Message
                    if (recoveryErrorMessage != null) {
                        Surface(
                            shape = CircleShape,
                            color = Rose500.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Rose500.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Rose500,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = recoveryErrorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = Rose500
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (recoveryStep == 1) {
                    TextButton(onClick = { recoveryStep = 0 }) {
                        Text("Kembali", color = Emerald500, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showForgotPinDialog = false },
                    shape = CircleShape
                ) {
                    Text("Tutup")
                }
            }
        )
    }
}
