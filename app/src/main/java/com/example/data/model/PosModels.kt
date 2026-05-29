package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class AppTheme {
    CLASSIC_NAVY, WARM_AMBER, EMERALD_GREEN, MINIMALIST_DARK
}

enum class StoreType {
    SARI_SARI, CAFE, TECH_REPAIR, CAR_WASH, LAUNDROMAT, WATER_STATION, BAKERY
}

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "My Business POS",
    val storeLogoUri: String = "",
    val activeTheme: AppTheme = AppTheme.CLASSIC_NAVY,
    val isBarcodeScannerEnabled: Boolean = false,
    val scanBarcodeOnlyOnce: Boolean = false,
    val storeType: StoreType = StoreType.SARI_SARI
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val costPrice: Double,
    val retailPrice: Double,
    val barcode: String? = null,
    val imageUri: String? = null,
    val isDeleted: Boolean = false
)

@Entity(tableName = "utang_records")
data class UtangRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val contactNo: String,
    val amount: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "receipt_logs")
data class ReceiptLog(
    @PrimaryKey val id: String, // Unique transaction/tracking ID
    val storeName: String,
    val storeType: String,
    val itemsJson: String, // Stringified list of CartItem
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val amountGiven: Double,
    val changeDue: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val metadataJson: String? = null // Type-specific payload (e.g. plate numbers, preorders, serial refs etc)
)

data class CartItem(
    val product: Product,
    val quantity: Double, // Weight, portions, or count
    val customPrice: Double? = null, // Clearance discounts, custom portioning price
    val customNotes: String? = null, // e.g. serial numbers or extra notes
    val selectedOption: String? = null // e.g. Tingi option, Cafe table, Water refill type, Car wash size
) {
    val displayPrice: Double
        get() = customPrice ?: product.retailPrice

    val total: Double
        get() = displayPrice * quantity
}

object JsonUtils {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun <T> toJson(data: T, clazz: Class<T>): String {
        return moshi.adapter(clazz).toJson(data)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        return try {
            moshi.adapter(clazz).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun listToJson(list: List<CartItem>): String {
        val type = Types.newParameterizedType(List::class.java, CartItem::class.java)
        return moshi.adapter<List<CartItem>>(type).toJson(list)
    }

    fun listFromJson(json: String): List<CartItem> {
        return try {
            val type = Types.newParameterizedType(List::class.java, CartItem::class.java)
            moshi.adapter<List<CartItem>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
