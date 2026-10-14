package com.sanvalero.boutiquestyle.report

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.sanvalero.boutiquestyle.R
import com.sanvalero.boutiquestyle.data.local.entity.SaleEntity
import com.sanvalero.boutiquestyle.domain.model.StoreUiState
import com.sanvalero.boutiquestyle.util.formatCurrency
import com.sanvalero.boutiquestyle.util.formatDate
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportExporter(private val context: Context) {
    fun stockReport(state: StoreUiState) {
        val rows = state.garments.flatMap { garment ->
            state.variants.filter { it.garmentId == garment.id }.map { variant ->
                "${garment.name} | ${variant.size} | ${variant.color} | ${variant.sku} | ${variant.stock} unidades"
            }
        }
        sharePdf("Inventario general", rows.ifEmpty { listOf("No hay variantes registradas.") })
    }

    fun salesReport(state: StoreUiState) {
        val date = SimpleDateFormat("dd/MM/yyyy", Locale("es", "DO")).format(Date())
        val sales = state.sales
        val csv = buildString {
            appendLine("Fecha,Cliente,Identificacion,Subtotal,ITBIS,Total,Metodo de pago")
            sales.forEach { sale ->
                appendLine(listOf(dateField(sale.saleDate), csv(sale.customerName), csv(sale.customerCedulaOrRnc), sale.subtotal, sale.itbis, sale.total, csv(sale.paymentMethod)).joinToString(","))
            }
            appendLine(listOf("", "TOTALES", "", sales.sumOf { it.subtotal }, sales.sumOf { it.itbis }, sales.sumOf { it.total }, "").joinToString(","))
        }
        saveBytes("ventas_${fileDate()}.csv", "text/csv", csv.toByteArray(Charsets.UTF_8))
        val rows = sales.map {
            "${formatDate(it.saleDate)} | ${it.customerName} | Subtotal ${formatCurrency(it.subtotal)} | ITBIS ${formatCurrency(it.itbis)} | Total ${formatCurrency(it.total)}"
        }.toMutableList()
        rows += "Totales: subtotal ${formatCurrency(sales.sumOf { it.subtotal })} · ITBIS ${formatCurrency(sales.sumOf { it.itbis })} · total ${formatCurrency(sales.sumOf { it.total })}"
        sharePdf("Ventas por período", rows.ifEmpty { listOf("No hay ventas registradas para el período.") })
    }

    fun managementReport(state: StoreUiState) {
        val soldByVariant = state.saleItems.groupBy { it.variantId }.mapValues { (_, items) -> items.sumOf { it.quantity } }
        val topProducts = soldByVariant.entries.mapNotNull { (variantId, quantity) ->
            val variant = state.variants.firstOrNull { it.id == variantId } ?: return@mapNotNull null
            val garment = state.garments.firstOrNull { it.id == variant.garmentId } ?: return@mapNotNull null
            Triple(garment.name, variant.size, quantity)
        }.groupBy { it.first }
            .mapValues { (name, products) -> name to products.sumOf { it.third } }
            .values
            .sortedByDescending { it.second }
            .take(5)
        val chartData = topProducts.map { it.first.take(15) to it.second }
        val rows = listOf(
            "Ventas en el período: ${state.sales.size}",
            "Apartados activos: ${state.dashboard.activeLayaways}",
            "Variantes con stock bajo: ${state.dashboard.lowStock}",
            "Ingresos en el período: ${formatCurrency(state.sales.sumOf { it.total })}",
            "",
            "Top 5 prendas vendidas"
        ) + topProducts.map { (name, quantity) -> "$name: $quantity unidades" }.ifEmpty { listOf("Aún no hay ventas con detalle.") }
        sharePdf("Resumen gerencial", rows, chartData)
    }

    private fun sharePdf(title: String, rows: List<String>, chartData: List<Pair<String, Int>>? = null) {
        val uri = savePdf(title, rows, chartData)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "BoutiqueStyle · $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
    }

    private fun savePdf(title: String, rows: List<String>, chartData: List<Pair<String, Int>>?): Uri {
        val document = PdfDocument()
        val pageRows = rows.chunked(34).ifEmpty { listOf(emptyList()) }
        val logo = context.resources.openRawResource(R.drawable.logo_boutiquestyle).use { BitmapFactory.decodeStream(it) }
        pageRows.forEachIndexed { index, lines ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
            val canvas = page.canvas
            val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(62, 39, 35); textSize = 19f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(55, 45, 42); textSize = 10.5f }
            if (logo != null) canvas.drawBitmap(logo, null, android.graphics.Rect(32, 22, 104, 94), Paint(Paint.ANTI_ALIAS_FLAG))
            canvas.drawText("BoutiqueStyle", 120f, 48f, heading)
            canvas.drawText(title, 120f, 68f, body)
            canvas.drawText("Emitido: ${formatDate(System.currentTimeMillis())}", 120f, 84f, body)
            canvas.drawLine(32f, 110f, 563f, 110f, body)
            var y = 137f
            lines.forEach { line ->
                wrap(line, 92).forEach { piece ->
                    canvas.drawText(piece, 38f, y, body)
                    y += 18f
                }
            }
            if (index == 0 && !chartData.isNullOrEmpty()) drawTopProductsChart(canvas, chartData, heading, body)
            body.textAlign = Paint.Align.RIGHT
            canvas.drawText("Página ${index + 1} de ${pageRows.size}", 560f, 814f, body)
            document.finishPage(page)
        }
        return try {
            writeOutput("boutiquestyle_${fileDate()}.pdf", "application/pdf", document::writeTo)
        } finally {
            document.close()
        }
    }

    private fun saveBytes(name: String, mime: String, data: ByteArray) {
        writeOutput(name, mime) { it.write(data) }
    }

    private fun writeOutput(name: String, mime: String, writer: (OutputStream) -> Unit): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/BoutiqueStyle")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("No se pudo crear el archivo en Descargas.")
            try {
                context.contentResolver.openOutputStream(uri, "w")?.use(writer)
                    ?: error("No se pudo guardar el archivo en Descargas.")
            } catch (exception: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw exception
            }
            return uri
        }
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BoutiqueStyle")
        if (!directory.exists() && !directory.mkdirs()) error("No se pudo crear la carpeta de reportes.")
        val file = File(directory, name)
        file.outputStream().use(writer)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun wrap(text: String, maxChars: Int): List<String> =
        text.chunked(maxChars).ifEmpty { listOf("") }

    private fun drawTopProductsChart(
        canvas: android.graphics.Canvas,
        products: List<Pair<String, Int>>,
        heading: Paint,
        body: Paint
    ) {
        canvas.drawText("Unidades por prenda en el período", 38f, 360f, heading.apply { textSize = 13f })
        val entries = products.take(5)
        val maxValue = (entries.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        val baseline = 665f
        val barWidth = 54f
        val slotWidth = 102f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(244, 201, 214) }
        entries.forEachIndexed { index, entry ->
            val x = 46f + index * slotWidth
            val height = 205f * entry.second / maxValue.toFloat()
            canvas.drawRoundRect(x, baseline - height, x + barWidth, baseline, 8f, 8f, fill)
            body.textAlign = Paint.Align.CENTER
            canvas.drawText(entry.second.toString(), x + barWidth / 2, baseline - height - 8f, body)
            canvas.drawText(entry.first.take(14), x + barWidth / 2, baseline + 19f, body)
        }
        body.textAlign = Paint.Align.LEFT
    }

    private fun csv(value: String) = "\"${value.replace("\"", "\"\"")}\""
    private fun dateField(time: Long) = formatDate(time)
    private fun fileDate() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
