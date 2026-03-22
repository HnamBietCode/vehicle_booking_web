package com.example.driver_app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.driver_app.data.model.SoberBooking
import com.example.driver_app.data.model.VehicleRental
import com.example.driver_app.service.LocationForegroundService
import com.example.driver_app.ui.components.BookingAction
import com.example.driver_app.ui.components.BookingCard
import com.example.driver_app.viewmodel.DriverJobsViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: DriverJobsViewModel, onLogout: () -> Unit) {
    val ctx = LocalContext.current
    val isLoading by vm.isLoading.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val pendingSober by vm.pendingSober.collectAsState()
    val assignedSober by vm.assignedSober.collectAsState()
    val pendingRentals by vm.pendingRentals.collectAsState()
    val assignedRentals by vm.assignedRentals.collectAsState()

    var localActionError by remember { mutableStateOf<String?>(null) }
    var pendingLocationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun executeAction(action: () -> Unit) {
        vm.clearError()
        localActionError = null
        action()
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted) {
            if (ensureLocationServicesEnabled(ctx)) {
                pendingLocationAction?.invoke()
            }
        } else {
            localActionError = "Hay cap quyen vi tri nen (Allow all the time) trong cai dat de theo doi tai xe khi tat man hinh."
            openAppSettings(ctx)
        }
        pendingLocationAction = null
    }

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!granted) {
            localActionError = "Can cap quyen vi tri de gui toa do tai xe."
            pendingLocationAction = null
            return@rememberLauncherForActivityResult
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission(ctx)) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            return@rememberLauncherForActivityResult
        }

        if (ensureLocationServicesEnabled(ctx)) {
            pendingLocationAction?.invoke()
        }
        pendingLocationAction = null
    }

    fun runWithLocationRequirements(action: () -> Unit) {
        localActionError = null
        pendingLocationAction = action

        if (!hasForegroundLocationPermission(ctx)) {
            foregroundLocationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission(ctx)) {
            localActionError = "Hay cap quyen vi tri nen (Allow all the time) cho app tai xe."
            pendingLocationAction = null
            openAppSettings(ctx)
            return
        }

        if (ensureLocationServicesEnabled(ctx)) {
            action()
        } else {
            localActionError = "Hay bat GPS/vi tri tren dien thoai roi bam lai."
        }
        pendingLocationAction = null
    }

    LaunchedEffect(Unit) {
        while (true) {
            vm.loadAll()
            delay(10_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onLogout) {
                Text("Dang xuat")
            }
            Button(onClick = {
                vm.clearError()
                vm.loadAll()
            }) {
                Text("Tai lai")
            }
        }

        val combinedError = localActionError ?: errorMessage
        if (combinedError != null) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = combinedError,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        localActionError = null
                        vm.clearError()
                    }) {
                        Text("Dong")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Don moi", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        pendingSober.forEach {
            BookingCard(
                title = "Tai xe",
                id = it.id,
                status = it.status,
                subtitle = listOfNotNull(it.customerName, it.pickupLocation, it.dropoffLocation)
                    .joinToString(" | "),
                actions = listOf(
                    BookingAction("Nhan") { executeAction { vm.acceptSober(it.id) } }
                )
            )
        }

        pendingRentals.forEach {
            BookingCard(
                title = "Xe",
                id = it.id,
                status = it.status,
                subtitle = listOfNotNull(it.rentalMode, it.pickupLocation, it.dropoffLocation)
                    .joinToString(" | "),
                actions = listOf(
                    BookingAction("Nhan") { executeAction { vm.acceptRental(it.id) } }
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Don cua toi", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        assignedSober.forEach {
            BookingCard(
                title = "Tai xe",
                id = it.id,
                status = it.status,
                subtitle = listOfNotNull(it.customerName, it.pickupLocation, it.dropoffLocation)
                    .joinToString(" | "),
                actions = soberActions(vm, ctx, it, ::runWithLocationRequirements, ::executeAction)
            )
        }

        assignedRentals.forEach {
            BookingCard(
                title = "Xe",
                id = it.id,
                status = it.status,
                subtitle = listOfNotNull(it.rentalMode, it.pickupLocation, it.dropoffLocation)
                    .joinToString(" | "),
                actions = rentalActions(vm, ctx, it, ::runWithLocationRequirements, ::executeAction)
            )
        }
    }
}

private fun soberActions(
    vm: DriverJobsViewModel,
    ctx: Context,
    booking: SoberBooking,
    requireLocation: ((() -> Unit) -> Unit),
    executeAction: ((() -> Unit) -> Unit)
): List<BookingAction> {
    return when (booking.status?.uppercase()) {
        "ACCEPTED" -> listOf(
            BookingAction("Khoi hanh") {
                executeAction {
                    requireLocation { startTrip(ctx, "SOBER", booking.id) }
                }
            },
            BookingAction("Da toi noi") { executeAction { vm.arriveSober(booking.id) } }
        )

        "ARRIVED" -> listOf(
            BookingAction("Bat dau chuyen") {
                executeAction {
                    requireLocation {
                        vm.startSober(booking.id) { ok ->
                            if (ok) {
                                startTrip(ctx, "SOBER", booking.id)
                            }
                        }
                    }
                }
            },
            BookingAction("Bat chia se vi tri") {
                executeAction {
                    requireLocation { startTrip(ctx, "SOBER", booking.id) }
                }
            }
        )

        "IN_PROGRESS" -> listOf(
            BookingAction("Bat chia se vi tri") {
                executeAction {
                    requireLocation { startTrip(ctx, "SOBER", booking.id) }
                }
            },
            BookingAction("Hoan thanh") {
                executeAction {
                    vm.completeSober(booking.id) { ok ->
                        if (ok) {
                            stopTrip(ctx)
                        }
                    }
                }
            }
        )

        else -> emptyList()
    }
}

private fun rentalActions(
    vm: DriverJobsViewModel,
    ctx: Context,
    rental: VehicleRental,
    requireLocation: ((() -> Unit) -> Unit),
    executeAction: ((() -> Unit) -> Unit)
): List<BookingAction> {
    return when (rental.status?.uppercase()) {
        "PENDING" -> listOf(
            BookingAction("Nhan don") { executeAction { vm.acceptRental(rental.id) } }
        )

        "CONFIRMED" -> listOf(
            BookingAction("Khoi hanh") {
                executeAction {
                    requireLocation {
                        vm.startRental(rental.id) { ok ->
                            if (ok) {
                                startTrip(ctx, "RENTAL", rental.id)
                            }
                        }
                    }
                }
            }
        )

        "ACTIVE" -> listOf(
            BookingAction("Bat chia se vi tri") {
                executeAction {
                    requireLocation { startTrip(ctx, "RENTAL", rental.id) }
                }
            },
            BookingAction("Hoan thanh") {
                executeAction {
                    vm.completeRental(rental.id) { ok ->
                        if (ok) {
                            stopTrip(ctx)
                        }
                    }
                }
            }
        )

        else -> emptyList()
    }
}

private fun startTrip(ctx: Context, tripType: String, tripId: Long) {
    val intent = Intent(ctx, LocationForegroundService::class.java)
    intent.putExtra("tripType", tripType)
    intent.putExtra("tripId", tripId)
    ContextCompat.startForegroundService(ctx, intent)
}

private fun stopTrip(ctx: Context) {
    val intent = Intent(ctx, LocationForegroundService::class.java)
    ctx.stopService(intent)
}

private fun hasForegroundLocationPermission(ctx: Context): Boolean {
    return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED
}

private fun hasBackgroundLocationPermission(ctx: Context): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        true
    } else {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PermissionChecker.PERMISSION_GRANTED
    }
}

private fun ensureLocationServicesEnabled(ctx: Context): Boolean {
    val manager = ctx.getSystemService(LocationManager::class.java) ?: return false
    val enabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    if (!enabled) {
        ctx.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
    return enabled
}

private fun openAppSettings(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
