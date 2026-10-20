package com.sanvalero.boutiquestyle.ui.screens

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.sanvalero.boutiquestyle.domain.usecase.isValidDominicanId

@Composable
fun IdentificationField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Cédula / RNC"
) {
    val hasValue = value.isNotBlank()
    val isValid = hasValue && isValidDominicanId(value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = hasValue && !isValid,
        supportingText = if (hasValue) {
            { Text(if (isValid) "Estructura y dígito verificador correctos." else "Usa 11 dígitos para cédula o 9 para RNC, con verificador correcto.") }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}
