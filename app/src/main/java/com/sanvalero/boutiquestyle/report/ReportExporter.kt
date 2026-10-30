package com.sanvalero.boutiquestyle.report

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
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
            appendLine("Fecha,Cliente,Identificacion,Articulos,Subtotal,ITBIS,Total,Metodo de pago")
            sales.forEach { sale ->
                val details = state.saleItems.filter { it.saleId == sale.id }.joinToString("; ") { item ->
                    val variant = state.variants.firstOrNull { it.id == item.variantId }
                    val garment = state.garments.firstOrNull { it.id == variant?.garmentId }
                    "${garment?.name ?: "Prenda"} ${variant?.size.orEmpty()} ${variant?.color.orEmpty()} x${item.quantity} @ ${item.unitPrice}"
                }
                appendLine(listOf(dateField(sale.saleDate), csv(sale.customerName), csv(sale.customerCedulaOrRnc), csv(details), sale.subtotal, sale.itbis, sale.total, csv(sale.paymentMethod)).joinToString(","))
            }
            appendLine(listOf("", "TOTALES", "", "", sales.sumOf { it.subtotal }, sales.sumOf { it.itbis }, sales.sumOf { it.total }, "").joinToString(","))
        }
        saveBytes("ventas_${fileDate()}.csv", "text/csv", csv.toByteArray(Charsets.UTF_8))
        val rows = sales.flatMap { sale ->
            val lines = state.saleItems.filter { it.saleId == sale.id }.map { item ->
                val variant = state.variants.firstOrNull { it.id == item.variantId }
                val garment = state.garments.firstOrNull { it.id == variant?.garmentId }
                "  ${garment?.name ?: "Prenda"} ${variant?.size.orEmpty()} ${variant?.color.orEmpty()} · ${item.quantity} x ${formatCurrency(item.unitPrice)} = ${formatCurrency(item.lineTotal)}"
            }
            listOf("${formatDate(sale.saleDate)} | ${sale.customerName} | ID ${sale.customerCedulaOrRnc} | ${sale.paymentMethod}") +
                lines.ifEmpty { listOf("  Sin detalle de artículos") } +
                "Subtotal ${formatCurrency(sale.subtotal)} | ITBIS ${formatCurrency(sale.itbis)} | Total ${formatCurrency(sale.total)}"
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
        val pageRows = rows.chunked(24).ifEmpty { listOf(emptyList()) }
        val logo = context.resources.openRawResource(R.drawable.logo_boutiquestyle).use { BitmapFactory.decodeStream(it) }
        pageRows.forEachIndexed { index, lines ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
            val canvas = page.canvas
            val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 30f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(35, 27, 31); textSize = 9f }
            val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(215, 211, 213); strokeWidth = 0.7f; style = Paint.Style.STROKE }
            val titleBand = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(217, 54, 130); style = Paint.Style.FILL }
            val columnBand = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(255, 201, 223); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 595f, 112f, titleBand)
            canvas.drawBitmap(logo, null, RectF(507f, 15f, 567f, 75f), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.drawText(title, 28f, 69f, heading)
            val emitted = Paint(body).apply { color = android.graphics.Color.WHITE; textSize = 9f }
            canvas.drawText("BoutiqueStyle  ·  Emitido: ${formatDate(System.currentTimeMillis())}", 30f, 96f, emitted)
            val headers = when {
                title.contains("Inventario", true) -> listOf("Prenda", "Talla", "Color", "SKU", "Existencias")
                title.contains("Ventas", true) -> listOf("Fecha", "Cliente", "Identificación", "Detalle y total")
                else -> listOf("Detalle del reporte")
            }
            val left = 28f
            val right = 567f
            val tableWidth = right - left
            val widths = when (headers.size) {
                5 -> listOf(145f, 54f, 90f, 135f, 115f)
                4 -> listOf(74f, 126f, 105f, 234f)
                else -> listOf(tableWidth)
            }
            var y = 130f
            fun drawRow(cells: List<String>, header: Boolean = false) {
                val rowHeight = if (header) 30f else 24f
                canvas.drawRect(left, y, right, y + rowHeight, if (header) columnBand else Paint().apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL })
                canvas.drawRect(left, y, right, y + rowHeight, grid)
                var x = left
                widths.forEachIndexed { column, width ->
                    if (column > 0) canvas.drawLine(x, y, x, y + rowHeight, grid)
                    val cellText = cells.getOrElse(column) { "" }.replace("\n", " ")
                    val cellPaint = if (header) Paint(body).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) } else body
                    canvas.save()
                    canvas.clipRect(x + 4f, y, x + width - 3f, y + rowHeight)
                    canvas.drawText(cellText, x + 5f, y + if (header) 19f else 16f, cellPaint)
                    canvas.restore()
                    x += width
                }
                y += rowHeight
            }
            drawRow(headers, header = true)
            lines.forEach { line ->
                val cells = when (headers.size) {
                    5 -> line.split(" | ", limit = 5)
                    4 -> line.split(" | ", limit = 4).let { if (it.size == 1) listOf("", "", "", line) else it }
                    else -> listOf(line)
                }
                drawRow(cells)
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
