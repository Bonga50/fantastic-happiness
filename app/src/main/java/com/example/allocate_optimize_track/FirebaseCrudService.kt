package com.example.allocate_optimize_track

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

class FirebaseCrudService<T : Any>(private val entityClass: Class<T>) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Helper to get the base path for the current user and entity type
    private fun getUserEntityRef(entityPath: String): DatabaseReference? {
        val userId = auth.currentUser?.uid ?: return null
        return FirebaseDatabase.getInstance().getReference(entityPath).child(userId)
    }

    suspend fun create(item: T, entityPath: String, itemIdField: (T) -> String, setItemId: (T, String) -> Unit): FirebaseResult<String> {
        return try {
            val userEntityRef = getUserEntityRef(entityPath)
                ?: return FirebaseResult.Failure(Exception("User not authenticated"))
            val newItemRef = userEntityRef.push() // Generate unique ID
            val newId = newItemRef.key ?: throw Exception("Failed to generate ID")
            setItemId(item, newId) // Set the ID on the item itself
            newItemRef.setValue(item).await()
            FirebaseResult.Success(newId)
        } catch (e: Exception) {
            FirebaseResult.Failure(e)
        }
    }

    fun getAll(entityPath: String): LiveData<List<T>> {
        val liveData = MutableLiveData<List<T>>()
        val userEntityRef = getUserEntityRef(entityPath)

        userEntityRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<T>()
                snapshot.children.forEach { childSnapshot ->
                    childSnapshot.getValue(entityClass)?.let { item ->
                        items.add(item)
                    }
                }
                liveData.postValue(items)
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle error, e.g., post empty list or error state
                liveData.postValue(emptyList())
            }
        }) ?: liveData.postValue(emptyList()) // Post empty if user not auth
        return liveData
    }


    suspend fun getById(id: String, entityPath: String): FirebaseResult<T?> {
        return try {
            val userEntityRef = getUserEntityRef(entityPath)
                ?: return FirebaseResult.Failure(Exception("User not authenticated"))

            val snapshot = userEntityRef.child(id).get().await()
            if (snapshot.exists()) {
                FirebaseResult.Success(snapshot.getValue(entityClass))
            } else {
                FirebaseResult.Success(null) // Not found
            }
        } catch (e: Exception) {
            FirebaseResult.Failure(e)
        }
    }

    suspend fun update(id: String, item: T, entityPath: String): FirebaseResult<Unit> {
        return try {
            val userEntityRef = getUserEntityRef(entityPath)
                ?: return FirebaseResult.Failure(Exception("User not authenticated"))
            userEntityRef.child(id).setValue(item).await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Failure(e)
        }
    }

    suspend fun delete(id: String, entityPath: String): FirebaseResult<Unit> {
        return try {
            val userEntityRef = getUserEntityRef(entityPath)
                ?: return FirebaseResult.Failure(Exception("User not authenticated"))
            userEntityRef.child(id).removeValue().await()
            FirebaseResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseResult.Failure(e)
        }
    }

    // For specific queries if needed, e.g., get expenses by categoryId
    fun queryByField(entityPath: String, fieldName: String, fieldValue: String): LiveData<List<T>> {
        val liveData = MutableLiveData<List<T>>()
        val userEntityRef = getUserEntityRef(entityPath)

        userEntityRef?.orderByChild(fieldName)?.equalTo(fieldValue)
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<T>()
                    snapshot.children.forEach { childSnapshot ->
                        childSnapshot.getValue(entityClass)?.let { items.add(it) }
                    }
                    liveData.postValue(items)
                }
                override fun onCancelled(error: DatabaseError) { liveData.postValue(emptyList()) }
            }) ?: liveData.postValue(emptyList())
        return liveData
    }

    suspend fun queryByFieldOnce(entityPath: String, fieldName: String, fieldValue: String): FirebaseResult<List<T>> {
        return try {
            val userEntityRef = getUserEntityRef(entityPath)
                ?: return FirebaseResult.Failure(Exception("User not authenticated for queryOnce"))

            val query = userEntityRef.orderByChild(fieldName).equalTo(fieldValue)
            val snapshot = query.get().await() // Use .get().await() for a one-time fetch

            val items = mutableListOf<T>()
            if (snapshot.exists()) {
                snapshot.children.forEach { childSnapshot ->
                    childSnapshot.getValue(entityClass)?.let { items.add(it) }
                }
            }
            FirebaseResult.Success(items)
        } catch (e: Exception) {
            FirebaseResult.Failure(e)
        }
    }
}