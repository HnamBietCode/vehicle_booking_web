package com.example.driver_app

import android.Manifest
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.driver_app.data.store.TokenStore
import com.example.driver_app.ui.screens.HomeScreen
import com.example.driver_app.ui.screens.LoginScreen
import com.example.driver_app.viewmodel.DriverJobsViewModel

class MainActivity : ComponentActivity() {
    private val requestBackgroundLocation = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val hasForegroundLocation = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasForegroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestBackgroundLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        requestPermissions.launch(permissions.toTypedArray())

        setContent {
            MaterialTheme {
                Surface {
                    val vm: DriverJobsViewModel = viewModel()
                    val lifecycleOwner = LocalLifecycleOwner.current
                    var hasToken by remember { mutableStateOf(TokenStore.getToken() != null) }
                    var locationReady by remember { mutableStateOf(isDriverLocationReady()) }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                locationReady = isDriverLocationReady()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    if (!locationReady) {
                        LocationRequiredScreen(
                            onEnableLocation = {
                                if (!hasForegroundLocationPermission()) {
                                    requestPermissions.launch(permissions.toTypedArray())
                                } else {
                                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                }
                            },
                            onRetry = {
                                locationReady = isDriverLocationReady()
                            }
                        )
                    } else if (!hasToken) {
                        LoginScreen(vm) {
                            hasToken = TokenStore.getToken() != null
                        }
                    } else {
                        HomeScreen(
                            vm = vm,
                            onLogout = {
                                TokenStore.clear()
                                vm.logout()
                                hasToken = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun hasForegroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED
    }

    private fun isDriverLocationReady(): Boolean {
        val manager = getSystemService(LocationManager::class.java) ?: return false
        val locationEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return hasForegroundLocationPermission() && locationEnabled
    }
}

@androidx.compose.runtime.Composable
private fun LocationRequiredScreen(
    onEnableLocation: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Can bat vi tri", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text("App tai xe chi cho phep vao dang nhap va homescreen khi GPS va quyen vi tri da bat.")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onEnableLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bat vi tri")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Toi da bat xong")
                }
            }
        }
    }
}
