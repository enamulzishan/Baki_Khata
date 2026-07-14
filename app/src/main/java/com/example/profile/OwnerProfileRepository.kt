package com.example.profile

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.Timestamp

class OwnerProfileRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getProfile(): OwnerProfile? {
        val user = auth.currentUser ?: return null
        val snapshot = db.collection("users").document(user.uid).get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(OwnerProfile::class.java)
        } else {
            // Return a default profile based on Firebase Auth if no doc exists
            val defaultProfile = OwnerProfile(
                uid = user.uid,
                fullName = user.displayName ?: "",
                email = user.email ?: "",
                phone = user.phoneNumber ?: "",
            )
            return defaultProfile
        }
    }

    suspend fun saveProfile(profile: OwnerProfile, imageUri: Uri?): OwnerProfile {
        val user = auth.currentUser ?: throw Exception("Not logged in")
        var finalProfile = profile.copy(
            uid = user.uid,
            email = user.email ?: profile.email,
            updatedAt = Timestamp.now()
        )

        if (imageUri != null) {
            val ref = storage.reference.child("profile_photos/${user.uid}/${UUID.randomUUID()}")
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            finalProfile = finalProfile.copy(photoUrl = url)
        }

        db.collection("users").document(user.uid).set(finalProfile).await()
        return finalProfile
    }
}
