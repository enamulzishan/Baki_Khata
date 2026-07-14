package com.example.reports

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.Customer
import com.example.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {
    fun generatePdf(
        context: Context,
        customers: List<Customer>,
        transactions: List<Transaction>,
        dateRangeStr: String,
        totalSales: Long,
        totalCollections: Long
    ): File? {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("Shop Report", 50f, 50f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Date Range: $dateRangeStr", 50f, 80f, paint)
            canvas.drawText("Total Sales: Tk $totalSales", 50f, 110f, paint)
            canvas.drawText("Total Collections: Tk $totalCollections", 50f, 130f, paint)

            paint.isFakeBoldText = true
            canvas.drawText("Recent Transactions:", 50f, 170f, paint)
            paint.isFakeBoldText = false

            var y = 200f
            val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            for (tx in transactions.take(15)) {
                val type = if (tx.type == "DUE") "Sale" else "Payment"
                canvas.drawText("${format.format(tx.timestamp.toDate())} - ${tx.customerName} - $type - Tk ${tx.amount}", 50f, y, paint)
                y += 20f
            }

            document.finishPage(page)

            val dir = File(context.cacheDir, "reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "report_${System.currentTimeMillis()}.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generateCsv(
        context: Context,
        transactions: List<Transaction>
    ): File? {
        try {
            val dir = File(context.cacheDir, "reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "report_${System.currentTimeMillis()}.csv")
            val writer = file.bufferedWriter()
            writer.write("Date,Customer,Type,Amount,Method\n")
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (tx in transactions) {
                val date = format.format(tx.timestamp.toDate())
                val type = if (tx.type == "DUE") "Sale" else "Payment"
                writer.write("$date,${tx.customerName},$type,${tx.amount},${tx.paymentMethod}\n")
            }
            writer.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
