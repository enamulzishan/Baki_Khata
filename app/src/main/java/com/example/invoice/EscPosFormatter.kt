package com.example.invoice

object EscPosFormatter {
    val ESC: Byte = 0x1B
    val GS: Byte = 0x1D
    
    val INIT = byteArrayOf(ESC, 0x40)
    
    val ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
    val ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
    val ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)
    
    val BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
    val BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)
    
    val TEXT_NORMAL = byteArrayOf(ESC, 0x21, 0x00)
    val TEXT_DOUBLE_HEIGHT = byteArrayOf(ESC, 0x21, 0x10)
    val TEXT_DOUBLE_WIDTH = byteArrayOf(ESC, 0x21, 0x20)
    val TEXT_DOUBLE_BOTH = byteArrayOf(ESC, 0x21, 0x30)
    
    val FEED_LINE = byteArrayOf(0x0A)
    
    val CUT = byteArrayOf(GS, 0x56, 0x41, 0x00)
    
    fun text(text: String): ByteArray {
        return text.toByteArray(Charsets.UTF_8)
    }
}
