package com.ankit.attendwise

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class AttendWiseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Optimize Firestore for offline usage
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100 * 1024 * 1024) // 100 MB cache
                    .build()
            )
            .build()
        db.firestoreSettings = settings
    }
}
