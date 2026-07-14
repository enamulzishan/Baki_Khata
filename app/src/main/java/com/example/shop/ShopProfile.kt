package com.example.shop

import com.google.firebase.Timestamp

data class ShopProfile(
    val shopId: String = "",
    val ownerUserId: String = "",
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val website: String = "",
    val businessCategory: String = "",
    val tradeLicense: String = "",
    val taxId: String = "",
    val currency: String = "BDT",
    val facebook: String = "",
    val whatsapp: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val district: String = "",
    val upazila: String = "",
    val language: String = "বাংলা",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
