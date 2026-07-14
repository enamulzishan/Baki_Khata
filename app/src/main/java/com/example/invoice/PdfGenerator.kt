package com.example.invoice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.Customer
import com.example.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    fun generateInvoicePdf(
        context: Context,
        transaction: Transaction,
        customer: Customer,
        shopProfile: com.example.shop.ShopProfile? = null
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
        }

        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val tf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateStr = df.format(transaction.timestamp.toDate())
        val timeStr = tf.format(transaction.timestamp.toDate())

        var y = 50f
        val centerX = pageInfo.pageWidth / 2f

        // Header
        canvas.drawText(if (shopProfile?.shopName.isNullOrBlank()) "SHOP NAME" else shopProfile!!.shopName, centerX, y, titlePaint)
        y += 30
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (shopProfile?.address.isNullOrBlank()) "123 Main Street, City" else shopProfile!!.address, centerX, y, paint)
        y += 20
        canvas.drawText("Phone: ${if (shopProfile?.phone.isNullOrBlank()) "+880 1234 567890" else shopProfile!!.phone}", centerX, y, paint)
        y += 40

        // Separator
        canvas.drawLine(50f, y, pageInfo.pageWidth - 50f, y, paint)
        y += 30

        // Invoice Info
        paint.textAlign = Paint.Align.LEFT
        val rightAlignPaint = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
        
        canvas.drawText("Invoice Number: ${transaction.invoiceNumber ?: "N/A"}", 50f, y, paint)
        canvas.drawText("Date: $dateStr", pageInfo.pageWidth - 50f, y, rightAlignPaint)
        y += 20
        canvas.drawText("Transaction ID: ${transaction.id}", 50f, y, paint)
        canvas.drawText("Time: $timeStr", pageInfo.pageWidth - 50f, y, rightAlignPaint)
        y += 30

        // Customer Info
        canvas.drawText("Bill To:", 50f, y, boldPaint)
        y += 20
        canvas.drawText("Name: ${customer.name}", 50f, y, paint)
        y += 20
        canvas.drawText("Code: ${customer.customerCode}", 50f, y, paint)
        y += 20
        canvas.drawText("Phone: ${customer.phone}", 50f, y, paint)
        y += 40

        // Separator
        canvas.drawLine(50f, y, pageInfo.pageWidth - 50f, y, paint)
        y += 30

        // Items Header
        canvas.drawText("Description", 50f, y, boldPaint)
        canvas.drawText("Qty", 250f, y, boldPaint)
        canvas.drawText("Unit Price", 350f, y, boldPaint)
        canvas.drawText("Subtotal", 450f, y, boldPaint)
        y += 20
        canvas.drawLine(50f, y, pageInfo.pageWidth - 50f, y, paint)
        y += 20

        // Item
        val description = if (transaction.type == "DUE") "Credit Sale" else "Payment"
        val amountStr = transaction.amount.toString()
        canvas.drawText(description, 50f, y, paint)
        canvas.drawText("1", 250f, y, paint)
        canvas.drawText(amountStr, 350f, y, paint)
        canvas.drawText(amountStr, 450f, y, paint)
        y += 40

        // Separator
        canvas.drawLine(50f, y, pageInfo.pageWidth - 50f, y, paint)
        y += 30

        // Summary
        val summaryX1 = 350f
        val summaryX2 = 500f
        rightAlignPaint.isFakeBoldText = true
        
        canvas.drawText("Subtotal:", summaryX1, y, paint)
        canvas.drawText("৳$amountStr", summaryX2, y, rightAlignPaint)
        y += 20
        canvas.drawText("Discount:", summaryX1, y, paint)
        canvas.drawText("৳0", summaryX2, y, rightAlignPaint)
        y += 20
        
        // previous due = current remaining due + paid - sale (wait, this depends on type)
        // just use transaction values directly
        val previousDue = if (transaction.type == "DUE") {
            transaction.remainingDue + transaction.paidAmount - transaction.saleAmount
        } else {
            transaction.remainingDue + transaction.paidAmount
        }
        
        canvas.drawText("Previous Due:", summaryX1, y, paint)
        canvas.drawText("৳$previousDue", summaryX2, y, rightAlignPaint)
        y += 20
        canvas.drawText("Payment Rcvd:", summaryX1, y, paint)
        canvas.drawText("৳${transaction.paidAmount}", summaryX2, y, rightAlignPaint)
        y += 20
        canvas.drawText("Remaining Due:", summaryX1, y, paint)
        canvas.drawText("৳${transaction.remainingDue}", summaryX2, y, rightAlignPaint)
        y += 20
        canvas.drawText("Grand Total:", summaryX1, y, boldPaint)
        canvas.drawText("৳$amountStr", summaryX2, y, rightAlignPaint)
        y += 60

        // QR Code
        val qrData = "INV:${transaction.invoiceNumber}, CUST:${customer.customerCode}, TX:${transaction.id}"
        val qrBitmap = QRCodeGenerator.generateQRCode(qrData, 150, 150)
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, 50f, y, paint)
        }

        // Footer
        paint.textAlign = Paint.Align.CENTER
        y += 180
        canvas.drawText("Thank you for your business!", centerX, y, paint)
        y += 20
        canvas.drawText("Custom shop note or terms go here.", centerX, y, paint)

        pdfDocument.finishPage(page)

        // Save
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${transaction.invoiceNumber ?: transaction.id}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
