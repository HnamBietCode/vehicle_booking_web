package com.example.driver_app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.driver_app.BuildConfig
import com.example.driver_app.data.store.TokenStore
import com.example.driver_app.viewmodel.DriverJobsViewModel

@Composable
fun LoginScreen(vm: DriverJobsViewModel, onLoginSuccess: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var serverUrl by remember { mutableStateOf(TokenStore.getBaseUrl() ?: BuildConfig.API_BASE_URL) }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var locationReady by remember { mutableStateOf(isLoginLocationReady(ctx)) }

    DisposableEffect(lifecycleOwner, ctx) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationReady = isLoginLocationReady(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.padding(24.dp)) {
        Text("Driver Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (!locationReady) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Hay bat GPS va cap quyen vi tri cho app truoc khi dang nhap.",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (!hasForegroundLocationPermission(ctx)) {
                                openAppSettings(ctx)
                            } else {
                                openLocationSettings(ctx)
                            }
                        }
                    ) {
                        Text("Bat vi tri")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = serverUrl,
            onValueChange = {
                serverUrl = it
                error = null
            },
            label = { Text("Backend URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Neu dung dien thoai that, hay nhap IP LAN cua may backend, vi du http://192.168.1.126:8080/",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                error = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = {
                pass = it
                error = null
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (!locationReady) {
                    error = "Hay bat GPS va cap quyen vi tri cho app truoc khi dang nhap."
                    if (!hasForegroundLocationPermission(ctx)) {
                        openAppSettings(ctx)
                    } else {
                        openLocationSettings(ctx)
                    }
                    return@Button
                }

                if (serverUrl.isBlank()) {
                    error = "Vui long nhap Backend URL"
                    return@Button
                }

                TokenStore.saveBaseUrl(serverUrl)
                loading = true
                vm.login(email, pass) { ok, message ->
                    loading = false
                    if (ok) {
                        onLoginSuccess()
                    } else {
                        error = message ?: "Sai tai khoan hoac mat khau"
                    }
                }
            },
            enabled = !loading && locationReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Dang nhap")
            }
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun isLoginLocationReady(ctx: Context): Boolean {
    return hasForegroundLocationPermission(ctx) && isLocationEnabled(ctx)
}

private fun hasForegroundLocationPermission(ctx: Context): Boolean {
    return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED
}

private fun isLocationEnabled(ctx: Context): Boolean {
    val manager = ctx.getSystemService(LocationManager::class.java) ?: return false
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private fun openLocationSettings(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun openAppSettings(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
