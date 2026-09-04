package com.devdooly.notificationedge.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.devdooly.notificationedge.ui.theme.*

/**
 * 알림 필터링 & 제외 관리 카드 (수신된 앱 목록별 제외 및 특정 키워드 차단)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationFilterSettingsCard(
    discoveredPackages: Set<String>,
    excludedPackages: Set<String>,
    blockedKeywords: Set<String>,
    onToggleExcludedPackage: (String, Boolean) -> Unit,
    onClearDiscoveredPackages: () -> Unit,
    onAddBlockedKeyword: (String) -> Unit,
    onRemoveBlockedKeyword: (String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var newKeywordText by remember { mutableStateOf("") }

    // 패키지 매니저를 통해 발견된 앱 정보 로드
    val pm = remember { context.packageManager }
    val discoveredAppList = remember(discoveredPackages) {
        discoveredPackages.map { pkg ->
            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg
            }
            val appIcon = try {
                pm.getApplicationIcon(pkg)
            } catch (e: Exception) {
                null
            }
            Triple(pkg, appName, appIcon)
        }.sortedBy { it.second.lowercase() }
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 헤더 (클릭 시 접기/펼치기)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = EdgeCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "알림 필터링 & 제외 관리",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isExpanded) "알림 수신된 앱별 제외 및 특정 키워드 차단" else "수신 앱 ${discoveredAppList.size}개 · 차단 키워드 ${blockedKeywords.size}개",
                        color = if (isExpanded) Color.Gray else EdgeCyan,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // 1. 수신된 앱별 알림 제외 관리 섹션
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "수신 기록된 앱 (${discoveredAppList.size}개)",
                            color = EdgeCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        if (discoveredAppList.isNotEmpty()) {
                            TextButton(
                                onClick = onClearDiscoveredPackages,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("기록 비우기", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (discoveredAppList.isEmpty()) {
                        Surface(
                            color = Color(0x33000000),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "아직 수신된 알림이 없습니다. 새로운 알림이 도착하면 여기에 해당 앱이 자동으로 등록되어 간편하게 알림을 끄거나 켤 수 있습니다.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            discoveredAppList.forEach { (pkg, appName, appIcon) ->
                                val isExcluded = excludedPackages.contains(pkg)
                                val iconBitmap = remember(appIcon) {
                                    try {
                                        appIcon?.toBitmap(72, 72)?.asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                Surface(
                                    color = if (isExcluded) Color(0x22111111) else Color(0x33282828),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isExcluded) Color(0x33FF5252) else Color(0x22FFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (iconBitmap != null) {
                                                Image(
                                                    bitmap = iconBitmap,
                                                    contentDescription = appName,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = appName,
                                                    color = if (isExcluded) Color.Gray else Color.White,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = pkg,
                                                    color = if (isExcluded) Color(0xFF884444) else Color.DarkGray,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isExcluded) "알림 제외됨" else "알림 수신",
                                                color = if (isExcluded) Color(0xFFFF6B6B) else EdgeCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Switch(
                                                checked = !isExcluded,
                                                onCheckedChange = { isEnabled ->
                                                    onToggleExcludedPackage(pkg, !isEnabled)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = EdgeCyan,
                                                    checkedTrackColor = EdgeCyan.copy(alpha = 0.3f),
                                                    uncheckedThumbColor = Color.Gray,
                                                    uncheckedTrackColor = Color.DarkGray
                                                ),
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // 2. 특정 키워드 차단 관리 섹션
                    // ==========================================
                    Text(
                        text = "차단 키워드 (${blockedKeywords.size}개)",
                        color = EdgeCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "제목, 본문, 메시지에 포함 시 알림을 표시하지 않습니다.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 키워드 입력 필드 + 추가 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newKeywordText,
                            onValueChange = { newKeywordText = it },
                            placeholder = { Text("차단할 키워드 (예: 광고, 특가, 스팸)", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EdgeCyan,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                cursorColor = EdgeCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (newKeywordText.isNotBlank()) {
                                        onAddBlockedKeyword(newKeywordText)
                                        newKeywordText = ""
                                        keyboardController?.hide()
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (newKeywordText.isNotBlank()) {
                                    onAddBlockedKeyword(newKeywordText)
                                    newKeywordText = ""
                                    keyboardController?.hide()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text("추가", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 등록된 키워드 태그(Chip) 목록
                    if (blockedKeywords.isEmpty()) {
                        Text(
                            text = "등록된 차단 키워드가 없습니다.",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            blockedKeywords.forEach { keyword ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = EdgeCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, EdgeCyan.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = keyword,
                                            color = EdgeCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onRemoveBlockedKeyword(keyword) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "삭제",
                                                tint = EdgeCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
