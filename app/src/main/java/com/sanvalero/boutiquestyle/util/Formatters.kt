package com.sanvalero.boutiquestyle.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double): String =
    "RD$ " + NumberFormat.getNumberInstance(Locale("es", "DO")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)

fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "DO")).format(Date(epochMillis))
