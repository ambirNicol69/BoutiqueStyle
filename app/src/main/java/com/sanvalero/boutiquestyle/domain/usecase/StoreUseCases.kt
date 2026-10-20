package com.sanvalero.boutiquestyle.domain.usecase

import com.sanvalero.boutiquestyle.domain.model.SaleRequest
import com.sanvalero.boutiquestyle.domain.model.CartLine
import com.sanvalero.boutiquestyle.domain.repository.StoreRepository
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: StoreRepository) {
    suspend operator fun invoke(username: String, password: String) = repository.login(username.trim(), password)
}

class CreateSaleUseCase @Inject constructor(private val repository: StoreRepository) {
    suspend operator fun invoke(request: SaleRequest): Long {
        require(request.items.isNotEmpty()) { "Agrega al menos una variante a la venta." }
        require(request.items.all { it.quantity > 0 }) { "La cantidad debe ser mayor que cero." }
        require(request.customerName.isNotBlank()) { "El nombre del cliente es obligatorio." }
        require(isValidDominicanId(request.customerId)) { "La cédula debe tener 11 dígitos o el RNC 9, con su dígito verificador correcto." }
        return repository.createSale(request)
    }
}

data class SaleTotals(val subtotal: Double, val itbis: Double, val total: Double)

class CalculateSaleTotalsUseCase @Inject constructor() {
    operator fun invoke(items: List<CartLine>): SaleTotals {
        require(items.isNotEmpty()) { "Agrega al menos una variante a la venta." }
        require(items.all { it.quantity > 0 && it.unitPrice > 0.0 }) { "La cantidad y el precio deben ser mayores que cero." }
        val subtotal = items.fold(BigDecimal.ZERO) { sum, line ->
            sum + BigDecimal.valueOf(line.unitPrice)
                .multiply(BigDecimal.valueOf(line.quantity.toLong()))
                .setScale(2, RoundingMode.HALF_UP)
        }.setScale(2, RoundingMode.HALF_UP)
        val tax = subtotal.multiply(BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP)
        return SaleTotals(subtotal.toDouble(), tax.toDouble(), subtotal.add(tax).toDouble())
    }
}

class ValidateSaleUseCase @Inject constructor() {
    fun isValidDominicanId(value: String): Boolean = com.sanvalero.boutiquestyle.domain.usecase.isValidDominicanId(value)
}

fun isValidDominicanId(value: String): Boolean {
    if (value.any { it !in '0'..'9' && it != '-' && !it.isWhitespace() }) return false
    val digits = value.filter { it in '0'..'9' }
    if ((digits.length != 9 && digits.length != 11) || digits.all { it == digits.first() }) return false

    return when (digits.length) {
        11 -> {
            val sum = digits.take(10).mapIndexed { index, digit ->
                val product = digit.digitToInt() * if (index % 2 == 0) 1 else 2
                if (product > 9) product - 9 else product
            }.sum()
            (10 - sum % 10) % 10 == digits.last().digitToInt()
        }
        9 -> {
            val weights = intArrayOf(7, 9, 8, 6, 5, 4, 3, 2)
            val remainder = digits.take(8).mapIndexed { index, digit -> digit.digitToInt() * weights[index] }.sum() % 11
            val expectedCheckDigit = when (val checkDigit = 11 - remainder) {
                10 -> 1
                11 -> 0
                else -> checkDigit
            }
            expectedCheckDigit == digits.last().digitToInt()
        }
        else -> false
    }
}
