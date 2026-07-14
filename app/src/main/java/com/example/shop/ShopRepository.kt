package com.example.shop

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ShopRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun getShopProfile(): ShopProfile? {
        val userId = auth.currentUser?.uid ?: return null
        val snapshot = db.collection("shops").whereEqualTo("ownerUserId", userId).get().await()
        return if (!snapshot.isEmpty) {
            snapshot.documents.first().toObject(ShopProfile::class.java)
        } else {
            null
        }
    }
    
    suspend fun saveShopProfile(profile: ShopProfile, imageUri: Uri?): ShopProfile {
        val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        var finalProfile = profile.copy(ownerUserId = userId)
        
        if (finalProfile.shopId.isEmpty()) {
            finalProfile = finalProfile.copy(shopId = UUID.randomUUID().toString())
        }
        
        if (imageUri != null) {
            val ref = storage.reference.child("shop_logos/${finalProfile.shopId}/${UUID.randomUUID()}")
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            finalProfile = finalProfile.copy(logoUrl = url)
        }
        
        db.collection("shops").document(finalProfile.shopId).set(finalProfile).await()
        return finalProfile
    }
}
