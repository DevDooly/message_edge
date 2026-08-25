package com.devdooly.notificationedge.data.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val hasUpdate: Boolean,
    val publishedAt: String
)

object AppUpdateManager {

    private const val GITHUB_REPO = "DevDooly/message_edge"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    /**
     * GitHub Releases API를 조회하여 최신 릴리즈 정보 확인
     */
    suspend fun checkForUpdate(currentVersionName: String): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "NotificationEdge-Android")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("GitHub API 응답 오류: ${connection.responseCode}"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", tagName)
            val body = json.optString("body", "새로운 변경 사항이 포함되어 있습니다.")
            val publishedAt = json.optString("published_at", "")

            var downloadUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (downloadUrl.isBlank()) {
                downloadUrl = "https://github.com/$GITHUB_REPO/releases/download/$tagName/NotificationEdge-$tagName.apk"
            }

            val hasUpdate = isNewerVersion(currentVersionName, tagName)

            Result.success(
                ReleaseInfo(
                    tagName = tagName,
                    title = title,
                    releaseNotes = body,
                    downloadUrl = downloadUrl,
                    hasUpdate = hasUpdate,
                    publishedAt = publishedAt
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 버전 문자열 비교 (예: "1.1.0" vs "v1.1.1" -> true)
     */
    internal fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").trim()
        val cleanLatest = latest.removePrefix("v").trim()

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    /**
     * 최신 APK 다운로드 및 진행률 콜백
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NotificationEdge-Android")
                connectTimeout = 15000
                readTimeout = 30000
            }

            // 리다이렉트 대응 (GitHub Release download는 aws s3로 리다이렉트됨)
            var currentConnection = connection
            var redirect = false
            val status = currentConnection.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                status == HttpURLConnection.HTTP_MOVED_PERM || 
                status == HttpURLConnection.HTTP_SEE_OTHER) {
                redirect = true
            }

            if (redirect) {
                val newUrl = currentConnection.getHeaderField("Location")
                currentConnection = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "NotificationEdge-Android")
                }
            }

            val totalBytes = currentConnection.contentLengthLong
            val apkFile = File(context.cacheDir, "NotificationEdge_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            currentConnection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        if (totalBytes > 0) {
                            val progress = totalDownloaded.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress.coerceIn(0f, 1f))
                            }
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(1.0f)
            }
            Result.success(apkFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 다운로드된 APK 설치 실행 (FileProvider 인텐트)
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) return

            // Android 8.0+ 알 수 없는 앱 설치 권한 체크
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        if (context !is Activity) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
