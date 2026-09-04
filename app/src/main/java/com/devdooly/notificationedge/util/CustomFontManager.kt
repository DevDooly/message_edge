package com.devdooly.notificationedge.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

data class CustomFontInfo(
    val id: String, // "custom:filename.ttf"
    val fileName: String,
    val displayName: String,
    val file: File
)

object CustomFontManager {

    private const val FONTS_DIR_NAME = "custom_fonts"
    private const val MAX_FONT_FILE_BYTES = 20L * 1024L * 1024L
    private val fontCache = ConcurrentHashMap<String, Pair<Long, FontFamily>>()

    private fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, FONTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getCustomFonts(context: Context): List<CustomFontInfo> {
        val dir = getFontsDir(context)
        val files = dir.listFiles { _, name ->
            val lower = name.lowercase()
            lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc")
        } ?: return emptyList()

        return files.map { file ->
            val nameWithoutExt = file.nameWithoutExtension
            CustomFontInfo(
                id = "custom:${file.name}",
                fileName = file.name,
                displayName = nameWithoutExt.replace("_", " ").replace("-", " "),
                file = file
            )
        }.sortedBy { it.displayName }
    }

    fun getFontFile(context: Context, fileName: String): File? {
        val file = resolveSafeFontFile(context, fileName) ?: return null
        return if (file.exists() && file.canRead()) file else null
    }

    fun saveCustomFont(context: Context, uri: Uri): Result<CustomFontInfo> {
        return runCatching {
            var fileName = getFileNameFromUri(context, uri) ?: "custom_font_${System.currentTimeMillis()}.ttf"
            if (listOf(".ttf", ".otf", ".ttc").none { fileName.lowercase().endsWith(it) }) {
                fileName = "$fileName.ttf"
            }

            // 파일명 정제
            val safeFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = requireNotNull(resolveSafeFontFile(context, safeFileName)) {
                "안전하지 않은 폰트 파일명입니다."
            }

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalBytes = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            if (totalBytes > MAX_FONT_FILE_BYTES) {
                                throw IllegalArgumentException("폰트 파일은 20MB 이하여야 합니다.")
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                } ?: throw IllegalStateException("폰트 파일을 읽을 수 없습니다.")
            } catch (error: Exception) {
                targetFile.delete()
                throw error
            }

            // 폰트 유효성 검사 (실제 파싱 가능한지 Typeface로 확인)
            try {
                val typeface = Typeface.createFromFile(targetFile)
                if (typeface == null) {
                    targetFile.delete()
                    throw IllegalArgumentException("유효하지 않은 폰트 파일 형식입니다.")
                }
            } catch (e: Exception) {
                targetFile.delete()
                throw IllegalArgumentException("폰트 파일 검증 실패: ${e.message}")
            }

            val nameWithoutExt = targetFile.nameWithoutExtension
            fontCache.remove(targetFile.absolutePath)
            CustomFontInfo(
                id = "custom:${targetFile.name}",
                fileName = targetFile.name,
                displayName = nameWithoutExt.replace("_", " ").replace("-", " "),
                file = targetFile
            )
        }
    }

    fun deleteCustomFont(context: Context, fileName: String): Boolean {
        val file = resolveSafeFontFile(context, fileName) ?: return false
        fontCache.remove(file.absolutePath)
        return if (file.exists()) file.delete() else false
    }

    fun loadFontFamily(context: Context, fontId: String): FontFamily? {
        if (!fontId.startsWith("custom:")) return null
        val fileName = fontId.removePrefix("custom:")
        val file = getFontFile(context, fileName) ?: return null
        fontCache[file.absolutePath]?.let { (lastModified, family) ->
            if (lastModified == file.lastModified()) return family
        }
        return try {
            FontFamily(Font(file)).also { family ->
                fontCache[file.absolutePath] = file.lastModified() to family
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resolveSafeFontFile(context: Context, fileName: String): File? {
        if (fileName.isBlank() || fileName != File(fileName).name) return null
        val directory = getFontsDir(context).canonicalFile
        val candidate = File(directory, fileName).canonicalFile
        return candidate.takeIf { it.parentFile == directory }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }
}
