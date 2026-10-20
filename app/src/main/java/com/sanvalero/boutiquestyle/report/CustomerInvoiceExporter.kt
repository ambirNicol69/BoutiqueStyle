package com.sanvalero.boutiquestyle.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.RectF
import android.graphics.Typeface
import com.sanvalero.boutiquestyle.util.formatCurrency
import com.sanvalero.boutiquestyle.util.formatDate
import java.io.OutputStream

data class CustomerInvoiceItem(
    val name: String,
    val variant: String,
    val quantity: Int,
    val unitPrice: Double
)

data class CustomerInvoice(
    val date: Long,
    val customer: String,
    val identification: String,
    val payment: String,
    val items: List<CustomerInvoiceItem>
) {
    val subtotal: Double get() = items.sumOf { it.quantity * it.unitPrice }
    val tax: Double get() = kotlin.math.round(subtotal * 0.18 * 100.0) / 100.0
}

object CustomerInvoiceExporter {
    private const val WIDTH = 1080
    private const val LEFT = 72f
    private const val ESPRESSO = 0xFF3E2723.toInt()
    private const val PETAL = 0xFFF4C9D6.toInt()
    private const val IVORY = 0xFFF5F0E6.toInt()
    private const val MUTED = 0xFF6E625E.toInt()

    fun writeImage(invoice: CustomerInvoice, output: OutputStream) {
        renderBitmap(invoice).useBitmap { bitmap ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "No se pudo generar la imagen de la factura." }
        }
    }

    fun writePdf(invoice: CustomerInvoice, output: OutputStream) {
        val document = PdfDocument()
        try {
            val designHeight = heightFor(invoice)
            val scale = 595f / WIDTH
            val pageHeight = (designHeight * scale).toInt()
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, pageHeight, 1).create())
            page.canvas.scale(scale, scale)
            drawInvoice(page.canvas, invoice, designHeight)
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun renderBitmap(invoice: CustomerInvoice): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, heightFor(invoice), Bitmap.Config.ARGB_8888)
        drawInvoice(Canvas(bitmap), invoice, bitmap.height)
        return bitmap
    }

    private fun heightFor(invoice: CustomerInvoice) = 1240 + invoice.items.size * 126

    private fun drawInvoice(canvas: Canvas, invoice: CustomerInvoice, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(IVORY)
        paint.color = ESPRESSO
        canvas.drawRoundRect(RectF(36f, 36f, WIDTH - 36f, 296f), 30f, 30f, paint)

        paint.color = PETAL
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 44f
        canvas.drawText("BOUTIQUESTYLE", LEFT, 110f, paint)
        paint.color = -1
        paint.textSize = 58f
        canvas.drawText("Factura de compra", LEFT, 194f, paint)
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(formatDate(invoice.date), LEFT, 248f, paint)
        canvas.drawText("Pago: ${invoice.payment}", 630f, 248f, paint)

        var y = 374f
        paint.color = ESPRESSO
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 34f
        canvas.drawText("Datos del cliente", LEFT, y, paint)
        y += 58f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 30f
        canvas.drawText("Nombre: ${invoice.customer.take(44)}", LEFT, y, paint)
        y += 48f
        canvas.drawText("Cédula / RNC: ${invoice.identification.take(32)}", LEFT, y, paint)
        y += 82f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 34f
        canvas.drawText("Detalle de compra", LEFT, y, paint)
        y += 26f
        paint.color = PETAL
        canvas.drawRoundRect(RectF(LEFT, y, WIDTH - LEFT, y + 64f), 12f, 12f, paint)
        paint.color = ESPRESSO
        paint.textSize = 24f
        canvas.drawText("PRENDA", LEFT + 18f, y + 42f, paint)
        canvas.drawText("CANT.", 770f, y + 42f, paint)
        canvas.drawText("IMPORTE", 892f, y + 42f, paint)
        y += 92f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 27f
        invoice.items.forEach { item ->
            paint.color = ESPRESSO
            canvas.drawText(item.name.take(32), LEFT + 8f, y, paint)
            paint.color = MUTED
            paint.textSize = 23f
            canvas.drawText(item.variant.take(42), LEFT + 8f, y + 38f, paint)
            paint.color = ESPRESSO
            paint.textSize = 27f
            canvas.drawText(item.quantity.toString(), 790f, y, paint)
            canvas.drawText(formatCurrency(item.quantity * item.unitPrice), 892f, y, paint)
            y += 126f
            paint.color = 0xFFD9CFCA.toInt()
            paint.strokeWidth = 2f
            canvas.drawLine(LEFT, y - 20f, WIDTH - LEFT, y - 20f, paint)
        }

        y += 18f
        val totalTop = height - 310f
        if (y > totalTop) y = totalTop
        paint.color = ESPRESSO
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawAmount(canvas, paint, "Subtotal", invoice.subtotal, y)
        drawAmount(canvas, paint, "ITBIS (18 %)", invoice.tax, y + 58f)
        paint.color = ESPRESSO
        canvas.drawRoundRect(RectF(LEFT, y + 92f, WIDTH - LEFT, y + 174f), 16f, 16f, paint)
        paint.color = -1
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 36f
        canvas.drawText("TOTAL", LEFT + 20f, y + 146f, paint)
        canvas.drawText(formatCurrency(invoice.subtotal + invoice.tax), 750f, y + 146f, paint)
        paint.color = MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 23f
        canvas.drawText("Gracias por comprar en BoutiqueStyle", LEFT, height - 68f, paint)
    }

    private fun drawAmount(canvas: Canvas, paint: Paint, label: String, amount: Double, y: Float) {
        paint.color = ESPRESSO
        canvas.drawText(label, LEFT + 8f, y, paint)
        canvas.drawText(formatCurrency(amount), 750f, y, paint)
    }

    private inline fun Bitmap.useBitmap(block: (Bitmap) -> Unit) {
        try { block(this) } finally { recycle() }
    }
}
