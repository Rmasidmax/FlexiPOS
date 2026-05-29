package com.example.data.repository

import android.content.Context
import android.content.Intent
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.data.local.PosDatabase
import com.example.data.local.PosDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class PosRepository(private val context: Context) {
    private val database = PosDatabase.getDatabase(context)
    private val dao = database.posDao()

    val settingsFlow: Flow<AppSettings?> = dao.getSettingsFlow()
    val allProductsFlow: Flow<List<Product>> = dao.getAllProductsFlow()
    val activeProductsFlow: Flow<List<Product>> = dao.getActiveProductsFlow()
    val allUtangFlow: Flow<List<UtangRecord>> = dao.getAllUtangFlow()
    val allReceiptLogsFlow: Flow<List<ReceiptLog>> = dao.getAllReceiptLogsFlow()

    suspend fun getSettings(): AppSettings {
        return dao.getSettings() ?: AppSettings().also {
            dao.insertOrUpdateSettings(it)
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        dao.insertOrUpdateSettings(settings)
    }

    suspend fun insertProduct(product: Product) {
        dao.insertProduct(product)
    }

    suspend fun softDeleteProduct(id: Int) {
        dao.softDeleteProduct(id)
    }

    suspend fun restoreProduct(id: Int) {
        dao.restoreProduct(id)
    }

    suspend fun deleteProduct(id: Int) {
        dao.deleteProduct(id)
    }

    suspend fun deleteProductsByBarcodes(barcodes: List<String>) {
        dao.deleteProductsByBarcodes(barcodes)
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return dao.getProductByBarcode(barcode)
    }

    suspend fun getProductById(id: Int): Product? {
        return dao.getProductById(id)
    }

    suspend fun insertUtang(record: UtangRecord) {
        dao.insertUtang(record)
    }

    suspend fun deleteUtang(id: Int) {
        dao.deleteUtang(id)
    }

    suspend fun insertReceiptLog(receipt: ReceiptLog) {
        dao.insertReceiptLog(receipt)
    }

    suspend fun getReceiptLogById(id: String): ReceiptLog? {
        return dao.getReceiptLogById(id)
    }

    suspend fun checkpointDb() = withContext(Dispatchers.IO) {
        try {
            database.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { cursor ->
                if (cursor.moveToFirst()) {
                    // Checkpoint run successfully
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun exportDatabase(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        checkpointDb()
        val dbFile = context.getDatabasePath(PosDatabase.DATABASE_NAME)
        if (!dbFile.exists()) return@withContext false
        try {
            FileInputStream(dbFile).use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importAndRestoreDatabase(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_restore.db")
        try {
            FileOutputStream(tempFile).use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }

            var isValid = false
            try {
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    tempFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                ).use { db ->
                    db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
                        if (cursor != null) {
                            isValid = true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isValid = false
            }

            if (!isValid) {
                tempFile.delete()
                return@withContext false
            }

            // Close current instance
            database.close()

            val dbFile = context.getDatabasePath(PosDatabase.DATABASE_NAME)
            val shmFile = File(dbFile.absolutePath + "-shm")
            val walFile = File(dbFile.absolutePath + "-wal")

            tempFile.copyTo(dbFile, overwrite = true)

            if (shmFile.exists()) shmFile.delete()
            if (walFile.exists()) walFile.delete()

            tempFile.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
