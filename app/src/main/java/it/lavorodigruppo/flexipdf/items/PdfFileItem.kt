package it.lavorodigruppo.flexipdf.items

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.net.toUri


data class PdfFileItem(
    val uriString: String,
    val displayName: String
) {
    fun getUri(): Uri = uriString.toUri()
}

fun List<PdfFileItem>.toJson(): String {
    return Gson().toJson(this)
}

fun String.toPdfFileList(): List<PdfFileItem> {
    return try {
        Gson().fromJson(this, object : TypeToken<List<PdfFileItem>>() {}.type)
    } catch (e: Exception) {
        emptyList()
    }
}