package com.example.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AlertCondition
import com.example.data.model.AlertItem
import com.example.data.model.AlertSoundType
import com.example.sound.AlertSoundPlayer
import com.example.ui.theme.AlertLineColor
import com.example.ui.theme.BearRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BullGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TVBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsDialog(
    symbol: String,
    currentPrice: Double,
    alerts: List<AlertItem>,
    onAddAlert: (AlertItem) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onToggleAlert: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isCreating by remember { mutableStateOf(false) }

    var targetPriceInput by remember { mutableStateOf(if (currentPrice > 0) currentPrice.toString() else "") }
    var selectedCondition by remember { mutableStateOf(AlertCondition.CROSSES_ABOVE) }
    var selectedSound by remember { mutableStateOf(AlertSoundType.RADAR_PING) }
    var noteInput by remember { mutableStateOf("") }

    var conditionMenuExpanded by remember { mutableStateOf(false) }
    var soundMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = AlertLineColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$symbol Alerts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isCreating) {
                    // Create New Alert Button
                    Button(
                        onClick = {
                            targetPriceInput = if (currentPrice > 0) currentPrice.toString() else ""
                            isCreating = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TVBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Alert for $symbol", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // List of alerts
                    val symbolAlerts = alerts.filter { it.symbol.equals(symbol, ignoreCase = true) }
                    if (symbolAlerts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active alerts set for $symbol",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(symbolAlerts, key = { it.id }) { alert ->
                                AlertRowItem(
                                    alert = alert,
                                    onToggle = { onToggleAlert(alert.id, it) },
                                    onDelete = { onDeleteAlert(alert.id) }
                                )
                            }
                        }
                    }
                } else {
                    // Alert Creation Form
                    Text(
                        text = "TARGET PRICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = targetPriceInput,
                        onValueChange = { targetPriceInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = TVBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Condition Dropdown
                    Text(
                        text = "TRIGGER CONDITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = conditionMenuExpanded,
                        onExpandedChange = { conditionMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCondition.title,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionMenuExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = TVBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = conditionMenuExpanded,
                            onDismissRequest = { conditionMenuExpanded = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            AlertCondition.values().forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond.title, color = TextPrimary) },
                                    onClick = {
                                        selectedCondition = cond
                                        conditionMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sound Dropdown + Test Sound Button
                    Text(
                        text = "ALERT SOUND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = soundMenuExpanded,
                            onExpandedChange = { soundMenuExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedSound.displayName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundMenuExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = TVBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkBackground,
                                    unfocusedContainerColor = DarkBackground
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = soundMenuExpanded,
                                onDismissRequest = { soundMenuExpanded = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                AlertSoundType.values().forEach { sound ->
                                    DropdownMenuItem(
                                        text = { Text(sound.displayName, color = TextPrimary) },
                                        onClick = {
                                            selectedSound = sound
                                            soundMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Test Sound Button
                        Button(
                            onClick = { AlertSoundPlayer.play(context, selectedSound) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Test",
                                tint = BinanceGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test", color = TextPrimary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { isCreating = false },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                val price = targetPriceInput.toDoubleOrNull()
                                if (price != null && price > 0.0) {
                                    val alert = AlertItem(
                                        symbol = symbol,
                                        targetPrice = price,
                                        condition = selectedCondition,
                                        soundType = selectedSound,
                                        note = noteInput
                                    )
                                    onAddAlert(alert)
                                    isCreating = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TVBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Alert", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRowItem(
    alert: AlertItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${alert.condition.title} @ ${alert.targetPrice}",
                color = if (alert.isActive) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Sound: ${alert.soundType.displayName}" +
                        if (alert.triggeredAt != null) " (Triggered)" else "",
                color = if (alert.triggeredAt != null) AlertLineColor else TextSecondary,
                fontSize = 11.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = alert.isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BullGreen,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = BearRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
