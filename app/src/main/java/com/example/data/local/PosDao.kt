package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {
    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettings)

    // Products
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getActiveProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isDeleted = 0 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteProduct(id: Int)

    @Query("UPDATE products SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreProduct(id: Int)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Int)

    @Query("DELETE FROM products WHERE barcode IN (:barcodes)")
    suspend fun deleteProductsByBarcodes(barcodes: List<String>)

    // Utang Records
    @Query("SELECT * FROM utang_records ORDER BY timestamp DESC")
    fun getAllUtangFlow(): Flow<List<UtangRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtang(record: UtangRecord)

    @Query("DELETE FROM utang_records WHERE id = :id")
    suspend fun deleteUtang(id: Int)

    // Receipt Logs
    @Query("SELECT * FROM receipt_logs ORDER BY timestamp DESC")
    fun getAllReceiptLogsFlow(): Flow<List<ReceiptLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptLog(receipt: ReceiptLog)

    @Query("SELECT * FROM receipt_logs WHERE id = :id")
    suspend fun getReceiptLogById(id: String): ReceiptLog?
}
