package com.example.digiarogya

import com.google.firebase.firestore.ktx.firestore
import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.firestore
import java.util.concurrent.TimeUnit



class AuthViewModel(application: Application) : AndroidViewModel(application)
 {
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    var verificationId by mutableStateOf<String?>(null)
        private set

    fun registerUser(aadhaar: String, phone: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = hashMapOf(
            "aadhaar" to aadhaar,
            "phone" to phone
        )
        db.collection("users").document(aadhaar).set(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Failed to register") }
    }

    fun fetchPhoneByAadhaar(aadhaar: String, onPhoneFound: (String) -> Unit, onNotFound: () -> Unit) {
        db.collection("users").document(aadhaar).get()
            .addOnSuccessListener { doc ->
                val phone = doc.getString("phone")
                if (!phone.isNullOrBlank()) onPhoneFound(phone) else onNotFound()
            }
            .addOnFailureListener { onNotFound() }
    }
    fun loginAnonymously(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInAnonymously()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Anonymous login failed") }
    }
    fun enableBiometricLogin(onComplete: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onError("User not logged in")
        db.collection("biometric_users").document(uid)
            .set(mapOf("biometricEnabled" to true))
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onError(it.message ?: "Failed to enable biometric") }
    }
     fun checkBiometricLogin(onAllowed: () -> Unit, onDenied: () -> Unit) {
         val context = getApplication<Application>().applicationContext
         val biometricManager = androidx.biometric.BiometricManager.from(context)

         val canAuthenticate = biometricManager.canAuthenticate(
             androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
         )

         val isBiometricAvailable = (canAuthenticate == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS)

         val uid = FirebaseAuth.getInstance().currentUser?.uid
         if (!isBiometricAvailable || uid == null) {
             onDenied()
             return
         }

         db.collection("biometric_users").document(uid).get()
             .addOnSuccessListener { document ->
                 if (document.getBoolean("biometricEnabled") == true) {
                     onAllowed()
                 } else {
                     onDenied()
                 }
             }
             .addOnFailureListener { onDenied() }
     }





     fun sendOTP(phone: String, activity: Activity, onCodeSent: () -> Unit, onError: (String) -> Unit) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                    auth.signInWithCredential(cred).addOnSuccessListener { onCodeSent() }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("PhoneAuth", "onVerificationFailed: ${e.message}") // <-- Log added here
                    onError(e.message ?: "Verification failed")
                }

                override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = vid
                    onCodeSent()
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    fun verifyOTP(code: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val credential = verificationId?.let {
            PhoneAuthProvider.getCredential(it, code)
        } ?: return onFailure("Verification ID missing")

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess()
                else onFailure(task.exception?.message ?: "OTP verification failed")
            }
    }

}
