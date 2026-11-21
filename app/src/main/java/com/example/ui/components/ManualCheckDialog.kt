package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.plate.PlateNormalizer
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SlateMuted

@Composable
fun ManualCheckDialog(
    onDismiss: () -> Unit,
    onCheckPlate: (String) -> Unit
) {
    var plateInput by remember { mutableStateOf("") }
    val normalizedPreview = remember(plateInput) { PlateNormalizer.normalizePlate(plateInput) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = RadarCyan
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_manual_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = plateInput,
                    onValueChange = { plateInput = it },
                    label = { Text(stringResource(R.string.field_plate)) },
                    placeholder = { Text(stringResource(R.string.field_plate_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (normalizedPreview.length >= 2) {
                                onCheckPlate(plateInput)
                                onDismiss()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_plate_input")
                )
                if (normalizedPreview.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "المعرّف الموحّد:",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                        Text(
                            text = normalizedPreview,
                            style = MaterialTheme.typography.labelMedium,
                            color = RadarCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCheckPlate(plateInput)
                    onDismiss()
                },
                enabled = normalizedPreview.length >= 2,
                modifier = Modifier.testTag("confirm_manual_check_button")
            ) {
                Text(stringResource(R.string.action_check_now))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_manual_check_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
