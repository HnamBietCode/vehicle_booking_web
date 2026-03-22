package com.example.driver_app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class BookingAction(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun BookingCard(
    title: String,
    id: Long,
    status: String?,
    subtitle: String = "",
    actions: List<BookingAction> = emptyList()
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("$title #$id", style = MaterialTheme.typography.titleSmall)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
            }
            Text("Trang thai: ${status ?: "-"}")
            if (actions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    actions.forEach { action ->
                        Button(onClick = action.onClick) {
                            Text(action.label)
                        }
                    }
                }
            }
        }
    }
}
