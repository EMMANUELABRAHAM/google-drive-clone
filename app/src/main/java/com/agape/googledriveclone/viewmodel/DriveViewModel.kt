package com.agape.googledriveclone.viewmodel

import android.Manifest
import android.accounts.Account
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agape.googledriveclone.model.FileDetails
import com.agape.googledriveclone.model.GoogleDriveFile
import com.agape.googledriveclone.utils.SingleLiveEvent
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DriveViewModel : ViewModel() {

    private val firebaseAuth = Firebase.auth
    private val scopes = listOf(Scope(DriveScopes.DRIVE_FILE))
    private val authorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(scopes).build()

    private val _files = MutableLiveData<List<File>>()
    val files: LiveData<List<File>> = _files

    private val _showToast = SingleLiveEvent<String>()
    val showToast: LiveData<String> = _showToast

    private val _initiateAuthorization = MutableLiveData<Int>()
    val initiateAuthorization: LiveData<Int> = _initiateAuthorization


    suspend fun signInGoogle(oneTap: SignInClient): IntentSender? {
        try {
            val signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true)
                    .setServerClientId(
                        "442006442670-jj6pu42mrsue6eiibgtids0i49caquqh.apps.googleusercontent.com"
                    ).setFilterByAuthorizedAccounts(false).build()
            ).setAutoSelectEnabled(true).build()
            return viewModelScope.async(Dispatchers.IO) {
                return@async oneTap.beginSignIn(signInRequest).await().pendingIntent.intentSender
            }.await()
        } catch (e: Exception) {
            e.handleException()
        }
        return null
    }

    fun getSignInResult(intent: Intent, oneTap: SignInClient) {
        try {
            val credential = oneTap.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val authResult = firebaseAuth.signInWithCredential(googleCredential).await()
                    Log.e("GD Clone", authResult.user!!.email!!)
                    _initiateAuthorization.postValue(1)
                } catch (e: Exception) {
                    e.handleException()
                }
            }
        } catch (e: Exception) {
            e.handleException()
        }
    }

    private fun Exception.handleException() {
        _showToast.postValue("Unexpected error occurred")
        Log.e("GD Clone", "Exception", this)
    }

    suspend fun authorizeGoogleDrive(authorizationClient: AuthorizationClient): AuthorizationResult? {
        try {
            return authorizationClient.authorize(authorizationRequest).await()
        } catch (e: Exception) {
            _showToast.postValue("Unexpected error occurred")
            Log.e("GD Clone", "Exception", e)
        }
        return null
    }

    fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun signOut(oneTap: SignInClient) {
        oneTap.signOut().await()
        firebaseAuth.signOut()
        resetValues()
    }

    private fun resetValues() {
        _initiateAuthorization.value = 0
    }

    suspend fun uploadFileToDrive(
        drive: Drive,
        fileDetails: FileDetails
    ) {
        //TODO: We can implement workManager to upload files. So that the file upload will be guaranteed.
        withContext(Dispatchers.IO) {
            try {
                _showToast.postValue("File Uploading...")
                val fileG = File().apply {
                    name = fileDetails.fileName
                }
                val mediaContent = FileContent(fileDetails.type, fileDetails.file)
                drive.files().create(fileG, mediaContent).execute()
                _showToast.postValue("File Uploaded")
            } catch (e: Exception) {
                e.handleException()
            }
        }
    }

    fun getGoogleDrive(credential: GoogleAccountCredential): Drive? {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser != null) {
            credential.selectedAccount = Account(currentUser.email, "google.com")
            Drive.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(),
                credential
            ).build()
        } else {
            null
        }
    }

    suspend fun listFiles(drive: Drive): List<GoogleDriveFile> = withContext(Dispatchers.IO) {
        val fileList: MutableList<GoogleDriveFile> = mutableListOf()
        var pageToken: String? = null
        try {
            do {
                val result: FileList = drive.files().list()
                    .setQ("trashed = false")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, mimeType)")
                    .setPageToken(pageToken)
                    .execute()

                fileList.addAll(result.files.map { file ->
                    GoogleDriveFile(
                        id = file.id,
                        name = file.name,
                        mimeType = file.mimeType
                    )
                })
                pageToken = result.nextPageToken
            } while (pageToken != null)
        } catch (e: Exception) {
            e.handleException()
        }
        fileList
    }

    fun getPermissionListForFileUpload(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
}

