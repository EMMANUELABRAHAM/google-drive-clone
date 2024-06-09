package com.agape.googledriveclone

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agape.googledriveclone.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var signInLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var authLauncher: ActivityResultLauncher<IntentSenderRequest>
    val firebaseAuth = Firebase.auth
    private val scopes = listOf(Scope(DriveScopes.DRIVE_FILE))
    private val authorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(scopes).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        signInLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                val data = result.data ?: return@registerForActivityResult
                data.dataString?.let { Log.d("GD Clone", it) }
                //TODO: Add try catch to avoid the crash when the authorization dialogue closes.
                getSignInResult(data)
                showToast("Activity result")
            }

        authLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                val data = result.data ?: return@registerForActivityResult
//                getSignInResult(data)
                showToast("Auth Result")

                val authorize = Identity.getAuthorizationClient(this@MainActivity)
                authorize.getAuthorizationResultFromIntent(data)
            }

        binding.signInButton.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val result = signInGoogle()
                    signInLauncher.launch(IntentSenderRequest.Builder(result).build())
                }
            }
        }

        binding.getFilesButton.setOnClickListener {
            lifecycleScope.launch {
                val files = getGoogleDrive()?.let { it1 -> listFiles(it1)
                }
                Log.d("GD Clone", files.toString())
            }
        }

        binding.isLoggedIn.setOnClickListener {
            lifecycleScope.launch {
                showToast("is signed in ${isSignedIn()}")
            }
        }

        binding.signOut.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    signOut()
                }
            }
        }

        binding.uploadBtn.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    getGoogleDrive()?.let { it1 -> uploadFileToDrive(it1, createTextFile(this@MainActivity, "abc", "sjdfhbsjgbkjsdbgsdbgjs sjgisdg"), "abc.txt") }
                }
            }
        }
    }

    private suspend fun signInGoogle(): IntentSender {
        val oneTap = Identity.getSignInClient(this)
        val signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true)
                .setServerClientId(
                    "442006442670-jj6pu42mrsue6eiibgtids0i49caquqh.apps.googleusercontent.com"
                ).setFilterByAuthorizedAccounts(false).build()
        ).setAutoSelectEnabled(true).build()
        withContext(Dispatchers.Main) {
            showToast("Sign In...")
        }
        return oneTap.beginSignIn(signInRequest).await().pendingIntent.intentSender
    }

    fun getSignInResult(intent: Intent) {
        val oneTap = Identity.getSignInClient(this)
        val firebaseAuth = Firebase.auth
        val credential = oneTap.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        lifecycleScope.launch(Dispatchers.IO) {
            val authResult = firebaseAuth.signInWithCredential(googleCredential).await()
            withContext(Dispatchers.Main) {
                showToast(authResult.user!!.email!!)
            }
            val authorizeGoogleDrive = authorizeGoogleDrive()
            authorizeGoogleDrive.pendingIntent?.let {
                authLauncher.launch(
                    IntentSenderRequest.Builder(it.intentSender)
                        .build()
                )
            }


        }
    }

    private fun showToast(str: String) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show()
    }

    // request authorize after sign in
    suspend fun authorizeGoogleDrive(): AuthorizationResult {
        val authorize = Identity.getAuthorizationClient(this)
        return authorize.authorize(authorizationRequest).await()

    }

    suspend fun signOut() {
        val oneTap = Identity.getSignInClient(this)
        oneTap.signOut().await()
        firebaseAuth.signOut()
    }

    suspend fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun getAllBackupFiles(drive: Drive): List<File> {
        return withContext(Dispatchers.IO) {
            val result =
                drive.files().list().setSpaces("drive").setFields("*")
                    .execute()
            result.files
        }
    }

    suspend fun listFiles(googleDrive: Drive): List<File> = withContext(Dispatchers.IO) {
        val fileList: MutableList<File> = mutableListOf()
        var pageToken: String? = null

        do {
            val result: FileList = googleDrive.files().list()
                .setQ("trashed = false") // Optional: to filter out trashed files
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, mimeType)")
                .setPageToken(pageToken)
                .execute()

            fileList.addAll(result.files)
            pageToken = result.nextPageToken
        } while (pageToken != null)

        fileList
    }

    fun getGoogleDrive(): Drive? {
        val credential = GoogleAccountCredential.usingOAuth2(this, listOf(DriveScopes.DRIVE_FILE))
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

    private fun createTextFile(context: Context , fileName: String, content: String): java.io.File {
        val file = java.io.File(context.filesDir, fileName)
        FileOutputStream(file).use {
            it.write(content.toByteArray())
        }
        return file
    }
    suspend fun uploadFileToDrive(
    drive: Drive,
    file: java.io.File,
    fileName:String
    ){
        withContext(Dispatchers.IO){
            val fileG = File().apply {
                name = fileName
            }
            val mediaContent = FileContent("text/plain",file)
            drive.files().create(fileG,mediaContent).execute()
        }
    }

}