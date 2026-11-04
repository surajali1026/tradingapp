package com.example.ui.chart

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChartScaleSettings
import com.example.data.model.ScalePosition
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TVBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PriceScaleSettingsDialog(
    settings: ChartScaleSettings,
    onUpdateSettings: (ChartScaleSettings) -> Unit,
    onResetScale: () -> Unit,
    onDismiss: () -> Unit
) {
    var minPriceInput by remember {
        mutableStateOf(settings.manualMinPrice?.toString() ?: "")
    }
    var maxPriceInput by remember {
        mutableStateOf(settings.manualMaxPrice?.toString() ?: "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
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
                    Text(
                        text = "Price Scale Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Rows
                ScaleToggleRow(
                    title = "Auto Scale",
                    description = "Fit price scale automatically to visible candles",
                    checked = settings.autoScale,
                    onCheckedChange = { onUpdateSettings(settings.copy(autoScale = it)) }
                )

                ScaleToggleRow(
                    title = "Logarithmic Scale (log)",
                    description = "Equal vertical distance for equal percentage moves",
                    checked = settings.isLogarithmic,
                    onCheckedChange = { onUpdateSettings(settings.copy(isLogarithmic = it, isPercentage = false)) }
                )

                ScaleToggleRow(
                    title = "Percentage Scale (%)",
                    description = "Show relative price change percentage",
                    checked = settings.isPercentage,
                    onCheckedChange = { onUpdateSettings(settings.copy(isPercentage = it, isLogarithmic = false)) }
                )

                ScaleToggleRow(
                    title = "Invert Scale",
                    description = "Flip price scale upside down",
                    checked = settings.isInverted,
                    onCheckedChange = { onUpdateSettings(settings.copy(isInverted = it)) }
                )

                ScaleToggleRow(
                    title = "Scale Lock",
                    description = "Prevent price scale vertical dragging",
                    checked = settings.isScaleLocked,
                    onCheckedChange = { onUpdateSettings(settings.copy(isScaleLocked = it)) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Price Scale Position (Left vs Right)
                Text(
                    text = "SCALE PLACEMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ScalePosition.RIGHT to "Right Side", ScalePosition.LEFT to "Left Side").forEach { (pos, label) ->
                        val isSelected = settings.scalePosition == pos
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TVBlue else DarkSurfaceVariant)
                                .border(1.dp, if (isSelected) TVBlue else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { onUpdateSettings(settings.copy(scalePosition = pos)) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual Price Range (When Auto Scale is OFF)
                if (!settings.autoScale) {
                    Text(
                        text = "MANUAL PRICE RANGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minPriceInput,
                            onValueChange = { minPriceInput = it },
                            label = { Text("Min Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = TVBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = maxPriceInput,
                            onValueChange = { maxPriceInput = it },
                            label = { Text("Max Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = TVBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val minVal = minPriceInput.toDoubleOrNull()
                            val maxVal = maxPriceInput.toDoubleOrNull()
                            if (minVal != null && maxVal != null && maxVal > minVal) {
                                onUpdateSettings(
                                    settings.copy(
                                        manualMinPrice = minVal,
                                        manualMaxPrice = maxVal
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TVBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Manual Range", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Reset Price Scale Button
                Button(
                    onClick = {
                        onResetScale()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Price Scale", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun ScaleToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TVBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkBackground
            )
        )
    }
}
