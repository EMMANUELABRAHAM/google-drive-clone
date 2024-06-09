package com.agape.googledriveclone.viewmodel

import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DriveViewModel : ViewModel() {

    private val firebaseAuth = Firebase.auth
    private val scopes = listOf(Scope(DriveScopes.DRIVE_FILE))
    private val authorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(scopes).build()

    private val _files = MutableLiveData<List<File>>()
    val files: LiveData<List<File>> = _files

    private val _uploadResult = MutableLiveData<String>()
    val uploadResult: LiveData<String> = _uploadResult

    private val _showToast = MutableLiveData<String>()
    val showToast: LiveData<String> = _showToast

    private val _initiateAuthorization = MutableLiveData<Int>()
    val initiateAuthorization: LiveData<Int> = _initiateAuthorization


    suspend fun signInGoogle(oneTap: SignInClient): IntentSender {
        val signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true)
                .setServerClientId(
                    "442006442670-jj6pu42mrsue6eiibgtids0i49caquqh.apps.googleusercontent.com"
                ).setFilterByAuthorizedAccounts(false).build()
        ).setAutoSelectEnabled(true).build()
        return viewModelScope.async(Dispatchers.IO) {
            return@async oneTap.beginSignIn(signInRequest).await().pendingIntent.intentSender
        }.await()
    }

    fun getSignInResult(intent: Intent, oneTap: SignInClient) {
        try {
            val credential = oneTap.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            viewModelScope.launch(Dispatchers.IO) {
                val authResult = firebaseAuth.signInWithCredential(googleCredential).await()
                Log.e("GD Clone", authResult.user!!.email!!)
                _initiateAuthorization.postValue(1)
            }
        } catch (e: Exception){
            _showToast.postValue("Unexpected error occurred")
            Log.e("GD Clone", "Exception", e)
        }

    }

    suspend fun authorizeGoogleDrive(authorizationClient: AuthorizationClient): AuthorizationResult? {
        try {
            return authorizationClient.authorize(authorizationRequest).await()
        }catch (e: Exception){
            _showToast.postValue("Unexpected error occurred")
            Log.e("GD Clone", "Exception", e)
        }
        return null
    }

    suspend fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun signOut(oneTap: SignInClient) {
        oneTap.signOut().await()
        firebaseAuth.signOut()
        resetValues()
    }

    private fun resetValues(){
        _initiateAuthorization.value = 0
    }
}

