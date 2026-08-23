package com.example.data

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.example.MainActivity

class WebAppInterface(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val onPermissionRequested: () -> Unit,
    private val onOpenDeveloperConfig: () -> Unit
) {

    @JavascriptInterface
    fun requestPermissions() {
        onPermissionRequested()
    }

    @JavascriptInterface
    fun checkPermissions(): Boolean {
        if (context is MainActivity) {
            return context.hasRequiredPermissions()
        }
        return false
    }

    @JavascriptInterface
    fun openDeveloperConfig() {
        onOpenDeveloperConfig()
    }

    @JavascriptInterface
    fun getTargetNumber(): String {
        return settingsManager.targetNumber
    }

    @JavascriptInterface
    fun saveTargetNumber(newNumber: String) {
        settingsManager.targetNumber = newNumber
        Toast.makeText(context, "Config Saved: $newNumber", Toast.LENGTH_SHORT).show()
    }
}
