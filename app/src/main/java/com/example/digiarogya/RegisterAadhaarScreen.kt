package com.example.digiarogya
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digiarogya.ui.theme.DigiArogyaTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun RegisterAadhaarScreen(
    onRegistered: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var aadhaar by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showBiometricPrompt by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as FragmentActivity

    Column(
        Modifier
            .padding(24.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Register Aadhaar", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = aadhaar,
            onValueChange = { aadhaar = it },
            label = { Text("Aadhaar Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            if (aadhaar.length != 12 || phone.length != 10) {
                Toast.makeText(context, "Invalid Aadhaar or Phone", Toast.LENGTH_SHORT).show()
                return@Button
            }

            viewModel.registerUser(aadhaar, "+91$phone",
                onSuccess = {
                    viewModel.loginAnonymously(
                        onSuccess = {
                            showBiometricPrompt = true // ask user if they want to enable biometric
                        },
                        onFailure = {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            onRegistered()
                        }
                    )
                },
                onFailure = {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            )
        }) {
            Text("Register")
        }
    }

    if (showBiometricPrompt) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    showBiometricPrompt = false
                    handleBiometricEnable(context, activity, viewModel, onRegistered)
                }) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBiometricPrompt = false
                    onRegistered()
                }) {
                    Text("Skip")
                }
            },
            title = { Text("Enable Fingerprint Login?") },
            text = { Text("Would you like to use fingerprint for faster login?") }
        )
    }
}
fun handleBiometricEnable(
    context: Context,
    activity: FragmentActivity,
    viewModel: AuthViewModel,
    onRegistered: () -> Unit
) {
    val biometricManager = BiometricManager.from(context)
    val canAuth = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )

    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
        Toast.makeText(context, "Biometric not supported or enabled on this device.", Toast.LENGTH_SHORT).show()
        onRegistered()
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Enable Fingerprint Login")
        .setSubtitle("Use fingerprint for future logins")
        .setNegativeButtonText("Cancel")
        .build()

    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.enableBiometricLogin(
                    onComplete = {
                        Toast.makeText(context, "Fingerprint login enabled!", Toast.LENGTH_SHORT).show()
                        onRegistered()
                    },
                    onError = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        onRegistered()
                    }
                )
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.e("BiometricAuth", "Error $errorCode: $errString")
                Toast.makeText(context, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                onRegistered()
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(context, "Fingerprint authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
    )

    biometricPrompt.authenticate(promptInfo)
}



@Preview
@Composable
fun RegisterAadhaarScreenPreview() {
    DigiArogyaTheme {
        RegisterAadhaarScreen(onRegistered = {})
    }
}