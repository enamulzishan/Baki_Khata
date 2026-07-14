package com.example.profile

import com.google.firebase.Timestamp

data class OwnerProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val photoUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
