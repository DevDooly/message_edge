package com.devdooly.notificationedge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.ui.theme.*
import kotlinx.coroutines.launch
@Composable
internal fun AppUpdateCard(currentVersionName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var updateStatus by remember { mutableStateOf<UpdateUIState>(UpdateUIState.Idle) }
    var releaseInfo by remember { mutableStateOf<com.devdooly.notificationedge.data.updater.ReleaseInfo?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    val installDownloadedApk: (java.io.File) -> Unit = { apkFile ->
        com.devdooly.notificationedge.data.updater.AppUpdateManager.installApk(context, apkFile)
            .onFailure { error ->
                updateStatus = UpdateUIState.Error(error.message ?: "APK 검증 또는 설치 실패")
            }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (updateStatus is UpdateUIState.UpdateAvailable) EdgeCyan else Color(0xFF333333)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = EdgeCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "앱 업데이트",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "현재 v$currentVersionName",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (val state = updateStatus) {
                is UpdateUIState.Idle -> {
                    Text(
                        text = "GitHub Release의 최신 APK 및 릴리즈 노트를 확인하고 터치 한 번으로 바로 업데이트할 수 있습니다.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            updateStatus = UpdateUIState.Checking
                            scope.launch {
                                val result = com.devdooly.notificationedge.data.updater.AppUpdateManager.checkForUpdate(currentVersionName)
                                result.onSuccess { info ->
                                    releaseInfo = info
                                    if (info.hasUpdate) {
                                        updateStatus = UpdateUIState.UpdateAvailable(info)
                                    } else {
                                        updateStatus = UpdateUIState.UpToDate(info.tagName)
                                    }
                                }.onFailure { error ->
                                    updateStatus = UpdateUIState.Error(error.message ?: "업데이트 확인 실패")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = EdgeCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("최신 업데이트 확인", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                is UpdateUIState.Checking -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = EdgeCyan,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("GitHub Releases 최신 버전 확인 중...", color = Color.LightGray, fontSize = 13.sp)
                    }
                }

                is UpdateUIState.UpToDate -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E332A))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EdgeGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "현재 최신 버전(${state.latestVersion})을 사용하고 있습니다!",
                            color = Color(0xFFB9F6CA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val info = releaseInfo
                                if (info != null) {
                                    updateStatus = UpdateUIState.Downloading
                                    scope.launch {
                                        com.devdooly.notificationedge.data.updater.AppUpdateManager.downloadApk(
                                            context = context,
                                            downloadUrl = info.downloadUrl,
                                            expectedSha256 = info.sha256,
                                            onProgress = { progress ->
                                                downloadProgress = progress
                                            }
                                        ).onSuccess { apkFile ->
                                            updateStatus = UpdateUIState.Downloaded(apkFile)
                                            installDownloadedApk(apkFile)
                                        }.onFailure { error ->
                                            updateStatus = UpdateUIState.Error(error.message ?: "다운로드 실패")
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("최신 APK 직접 재설치", color = Color.Gray, fontSize = 11.sp)
                        }

                        TextButton(
                            onClick = {
                                updateStatus = UpdateUIState.Checking
                                scope.launch {
                                    val result = com.devdooly.notificationedge.data.updater.AppUpdateManager.checkForUpdate(currentVersionName)
                                    result.onSuccess { info ->
                                        releaseInfo = info
                                        updateStatus = if (info.hasUpdate) UpdateUIState.UpdateAvailable(info) else UpdateUIState.UpToDate(info.tagName)
                                    }.onFailure {
                                        updateStatus = UpdateUIState.Error(it.message ?: "확인 실패")
                                    }
                                }
                            }
                        ) {
                            Text("다시 확인", color = EdgeCyan, fontSize = 12.sp)
                        }
                    }
                }

                is UpdateUIState.UpdateAvailable -> {
                    val info = state.info
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EdgeCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, EdgeCyan)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EdgeCyan
                                ) {
                                    Text(
                                        text = "NEW ${info.tagName}",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = info.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (info.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = info.releaseNotes,
                                    color = Color(0xFFDDDDDD),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 6
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            updateStatus = UpdateUIState.Downloading
                            downloadProgress = 0f
                            scope.launch {
                                val result = com.devdooly.notificationedge.data.updater.AppUpdateManager.downloadApk(
                                    context = context,
                                    downloadUrl = info.downloadUrl,
                                    expectedSha256 = info.sha256,
                                    onProgress = { downloadProgress = it }
                                )
                                result.onSuccess { apkFile ->
                                    updateStatus = UpdateUIState.Downloaded(apkFile)
                                    installDownloadedApk(apkFile)
                                }.onFailure { error ->
                                    updateStatus = UpdateUIState.Error("다운로드 실패: ${error.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("지금 다운로드 및 바로 업데이트", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                is UpdateUIState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GitHub에서 최신 APK 다운로드 중...", color = Color.LightGray, fontSize = 12.sp)
                            Text("${(downloadProgress * 100).toInt()}%", color = EdgeCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            color = EdgeCyan,
                            trackColor = Color(0xFF333333),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                is UpdateUIState.Downloaded -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EdgeGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("다운로드 완료! 설치 창을 열었습니다.", color = Color.White, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                installDownloadedApk(state.apkFile)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EdgeGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("설치 화면 다시 열기", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is UpdateUIState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "오류: ${state.message}",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                updateStatus = UpdateUIState.Idle
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("다시 시도", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private sealed interface UpdateUIState {
    data object Idle : UpdateUIState
    data object Checking : UpdateUIState
    data class UpToDate(val latestVersion: String) : UpdateUIState
    data class UpdateAvailable(val info: com.devdooly.notificationedge.data.updater.ReleaseInfo) : UpdateUIState
    data object Downloading : UpdateUIState
    data class Downloaded(val apkFile: java.io.File) : UpdateUIState
    data class Error(val message: String) : UpdateUIState
}
