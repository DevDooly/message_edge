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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.devdooly.notificationedge.R
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
        shape = RoundedCornerShape(20.dp),
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
                        text = stringResource(R.string.settings_filter_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isExpanded) stringResource(R.string.settings_filter_description) else stringResource(R.string.settings_filter_summary, discoveredAppList.size, blockedKeywords.size),
                        color = if (isExpanded) TextMuted else EdgeCyan,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(if (isExpanded) R.string.settings_collapse else R.string.settings_expand),
                    tint = TextMuted,
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
                            text = stringResource(R.string.settings_received_apps, discoveredAppList.size),
                            color = EdgeCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        if (discoveredAppList.isNotEmpty()) {
                            TextButton(
                                onClick = onClearDiscoveredPackages,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.heightIn(min = 28.dp)
                            ) {
                                Text(stringResource(R.string.settings_clear_history), color = TextMuted, fontSize = 11.sp)
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
                                text = stringResource(R.string.settings_received_apps_empty),
                                color = TextMuted,
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
                                                    color = if (isExcluded) TextMuted else Color.White,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = pkg,
                                                    color = if (isExcluded) Color(0xFF884444) else GlassBorder,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(if (isExcluded) R.string.settings_app_excluded else R.string.settings_app_receiving),
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
                                                    uncheckedThumbColor = TextMuted,
                                                    uncheckedTrackColor = GlassBorder
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
                        text = stringResource(R.string.settings_blocked_keywords, blockedKeywords.size),
                        color = EdgeCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_blocked_keywords_description),
                        color = TextMuted,
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
                            placeholder = { Text(stringResource(R.string.settings_keyword_placeholder), color = TextMuted, fontSize = 12.sp) },
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
                            Text(stringResource(R.string.settings_add), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 등록된 키워드 태그(Chip) 목록
                    if (blockedKeywords.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_blocked_keywords_empty),
                            color = GlassBorder,
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
                                    shape = RoundedCornerShape(20.dp),
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
                                                contentDescription = stringResource(R.string.settings_remove_keyword, keyword),
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
