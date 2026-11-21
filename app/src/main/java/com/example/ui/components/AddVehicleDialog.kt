package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.plate.PlateNormalizer
import com.example.ui.theme.ObsidianBase
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SlateMuted

@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onSave: (
        plateRaw: String,
        makeModel: String,
        color: String,
        year: String,
        caseNumber: String,
        notes: String
    ) -> Unit
) {
    var plate by remember { mutableStateOf("") }
    var makeModel by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var caseNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val normalizedPlate = remember(plate) { PlateNormalizer.normalizePlate(plate) }
    val isValid = normalizedPlate.length >= 2 && makeModel.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = RadarCyan
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_add_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text(stringResource(R.string.field_plate)) },
                    placeholder = { Text(stringResource(R.string.field_plate_placeholder)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_plate_input")
                )

                if (normalizedPlate.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianBase
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مفتاح المطابقة الآلي:",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted
                            )
                            Text(
                                text = normalizedPlate,
                                style = MaterialTheme.typography.labelLarge,
                                color = RadarCyan
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = makeModel,
                    onValueChange = { makeModel = it },
                    label = { Text(stringResource(R.string.field_make_model)) },
                    placeholder = { Text(stringResource(R.string.field_make_model_placeholder)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_make_model_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = color,
                        onValueChange = { color = it },
                        label = { Text(stringResource(R.string.field_color)) },
                        placeholder = { Text(stringResource(R.string.field_color_placeholder)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("add_color_input")
                    )

                    OutlinedTextField(
                        value = year,
                        onValueChange = { if (it.length <= 4) year = it },
                        label = { Text(stringResource(R.string.field_year)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("add_year_input")
                    )
                }

                OutlinedTextField(
                    value = caseNumber,
                    onValueChange = { caseNumber = it },
                    label = { Text(stringResource(R.string.field_case_number)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_case_number_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.field_notes)) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_notes_input")
                )

                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(plate, makeModel, color, year, caseNumber, notes)
                    onDismiss()
                },
                enabled = isValid,
                modifier = Modifier.testTag("save_stolen_vehicle_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_vehicle_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
