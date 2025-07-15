package com.example.digiarogya

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.digiarogya.ui.theme.DigiArogyaTheme

sealed class DigiRoute(val route: String) {
    object Login : DigiRoute("login")
    object Dashboard : DigiRoute("dashboard")
    object Appointment : DigiRoute("appointments")
    object Register : DigiRoute("register")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DigiArogyaAppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = DigiRoute.Login.route) {
        composable(DigiRoute.Login.route) {
            DigiArogyaLoginScreen(
                onLoginSuccess = { navController.navigate(DigiRoute.Dashboard.route) },
                onRegisterClick = { navController.navigate(DigiRoute.Register.route) }
            )
        }
        composable(DigiRoute.Dashboard.route) {
            HealthDashboardScreen(
                onBackPress = { navController.popBackStack() },
                onAppointmentsClick = { navController.navigate("appointments") }
            )
        }
        composable(DigiRoute.Appointment.route) {
            AppointmentScreen(onBackPress = { navController.popBackStack() })
        }
        composable(DigiRoute.Register.route) {
            RegisterAadhaarScreen(onRegistered = { navController.popBackStack() })
        }
    }
}

@Composable
fun DigiArogyaLoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var aadhaar by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var showBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkBiometricLogin(
            onAllowed = { showBiometric = true },
            onDenied = { showBiometric = false }
        )
    }

    Scaffold(topBar = { DigiBar() }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Your Health,\nOne Touch Away", fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Securely access your health records", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Login via Aadhaar", fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = aadhaar,
                onValueChange = { aadhaar = it },
                label = { Text("Enter Aadhaar Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isOtpSent) {
                Button(
                    onClick = {
                        if (aadhaar.length != 12) {
                            Toast.makeText(context, "Invalid Aadhaar Number", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.fetchPhoneByAadhaar(
                                aadhaar,
                                onPhoneFound = { number ->
                                    phone = number
                                    viewModel.sendOTP(
                                        phone = number,
                                        activity = activity,
                                        onCodeSent = {
                                            isOtpSent = true
                                            Toast.makeText(context, "OTP Sent", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = {
                                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onNotFound = {
                                    Toast.makeText(context, "Aadhaar Not Found", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Send OTP")
                }
            } else {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it },
                    label = { Text("Enter OTP") },
                    visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(
                                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isVisible) "Hide OTP" else "Show OTP"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.verifyOTP(
                            code = otp,
                            onSuccess = onLoginSuccess,
                            onFailure = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Verify & Login")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (showBiometric) {
                Text("OR", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Use Fingerprint to login")
                Spacer(modifier = Modifier.height(16.dp))

                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint",
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            authenticateWithBiometric(
                                context = context,
                                activity = activity,
                                onSuccess = {
                                    viewModel.loginAnonymously(
                                        onSuccess = onLoginSuccess,
                                        onFailure = {
                                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onError = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row {
                Text("Don't have an account? ")
                Text(
                    "Sign Up",
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onRegisterClick()
                    }
                )
            }
        }
    }
}
fun authenticateWithBiometric(
    context: Context,
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val biometricManager = BiometricManager.from(context)
    val canAuth = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )

    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        onError("Biometric authentication is not available or not set up.")
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Fingerprint Login")
        .setSubtitle("Use fingerprint to access DigiArogya")
        .setNegativeButtonText("Cancel")
        .build()

    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError("Auth error: $errString")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Fingerprint authentication failed.")
            }
        }
    )

    biometricPrompt.authenticate(promptInfo)
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DigiBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.image_size))
                        .padding(dimensionResource(id = R.dimen.padding_small)),
                    painter = painterResource(R.drawable.head),
                    contentDescription = null
                )
            }
        }
    )
}
