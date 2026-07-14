package com.example

import com.google.firebase.Timestamp
import java.util.Date

data class Reminder(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val method: String = "SMS",
    val messageText: String = "",
    val timestamp: Timestamp = Timestamp(Date()),
    val status: String = "SENT"
)
