package com.example.driver_app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.driver_app.ui.components.BookingAction
import com.example.driver_app.ui.components.BookingCard
import com.example.driver_app.viewmodel.DriverJobsViewModel

@Composable
fun DriverJobsScreen(vm: DriverJobsViewModel) {
    val pendingSober by vm.pendingSober.collectAsState()
    val pendingRentals by vm.pendingRentals.collectAsState()

    Column(Modifier.padding(16.dp)) {
        Text("Don thue tai xe", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        pendingSober.forEach { item ->
            BookingCard(
                title = "Tai xe",
                id = item.id,
                status = item.status,
                subtitle = listOfNotNull(item.customerName, item.pickupLocation, item.dropoffLocation)
                    .joinToString(" | "),
                actions = listOf(
                    BookingAction("Nhan") { vm.acceptSober(item.id) }
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Don thue xe", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        pendingRentals.forEach { item ->
            BookingCard(
                title = "Xe",
                id = item.id,
                status = item.status,
                subtitle = listOfNotNull(item.rentalMode, item.pickupLocation, item.dropoffLocation)
                    .joinToString(" | "),
                actions = listOf(
                    BookingAction("Nhan") { vm.acceptRental(item.id) }
                )
            )
        }
    }
}
