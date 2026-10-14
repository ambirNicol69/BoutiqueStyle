package com.sanvalero.boutiquestyle.domain.usecase

import com.sanvalero.boutiquestyle.domain.model.CartLine
import com.sanvalero.boutiquestyle.data.local.entity.VariantEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleRulesTest {
    private val totals = CalculateSaleTotalsUseCase()

    @Test
    fun calculatesItbisAtEighteenPercentWithCurrencyRounding() {
        val result = totals(listOf(CartLine(VariantEntity(garmentId = 1, size = "M", color = "Rosa", stock = 3, sku = "SKU-1"), 3, 10.50)))

        assertEquals(31.50, result.subtotal, 0.001)
        assertEquals(5.67, result.itbis, 0.001)
        assertEquals(37.17, result.total, 0.001)
    }

    @Test
    fun totalsAllLinesInTheSaleCart() {
        val result = totals(
            listOf(
                CartLine(VariantEntity(garmentId = 1, size = "S", color = "Rosa", stock = 2, sku = "SKU-2"), 2, 1.11),
                CartLine(VariantEntity(garmentId = 2, size = "M", color = "Marfil", stock = 1, sku = "SKU-3"), 1, 2.50)
            )
        )

        assertEquals(4.72, result.subtotal, 0.001)
        assertEquals(0.85, result.itbis, 0.001)
        assertEquals(5.57, result.total, 0.001)
    }

    @Test
    fun validatesDominicanCedulaAndRncChecksums() {
        assertTrue(isValidDominicanId("001-1234567-3"))
        assertTrue(isValidDominicanId("402123455"))
        assertFalse(isValidDominicanId("00112345674"))
        assertFalse(isValidDominicanId("ID00112345673"))
        assertFalse(isValidDominicanId("12345"))
    }
}
