package com.devdooly.notificationedge.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.ui.theme.*
import com.devdooly.notificationedge.util.CustomFontManager
@Composable
internal fun FontSettingsCard(
    selectedFontId: String,
    onFontSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var customFonts by remember { mutableStateOf(CustomFontManager.getCustomFonts(context)) }

    // 파일 선택 런처 (.ttf, .otf, .ttc)
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = CustomFontManager.saveCustomFont(context, uri)
            result.onSuccess { fontInfo ->
                customFonts = CustomFontManager.getCustomFonts(context)
                onFontSelected(fontInfo.id)
                Toast.makeText(context, "폰트가 추가되었습니다: ${fontInfo.displayName}", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "폰트 등록 실패: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val currentDisplayName = if (selectedFontId.startsWith("custom:")) {
        val fileName = selectedFontId.removePrefix("custom:")
        customFonts.find { it.fileName == fileName }?.displayName ?: fileName
    } else {
        AppFont.fromId(selectedFontId).displayName
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "글꼴 및 폰트 설정 (Font)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "현재: $currentDisplayName",
                        color = EdgeCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "접기" else "더보기",
                        tint = Color.LightGray
                    )
                }
            }

            // 한영 혼용 정렬 보정 안내 문구
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = EdgeCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "한글/영문 혼용 시 수직 기준선(Baseline)과 패딩을 보정하여 고르게 표시합니다.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))

                // 폰트 파일 업로드 버튼
                Button(
                    onClick = {
                        fontPickerLauncher.launch(
                            arrayOf(
                                "font/*",
                                "application/x-font-ttf",
                                "application/x-font-opentype",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = EdgeCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "내 기기에서 폰트 파일(.ttf, .otf) 불러오기",
                        color = EdgeCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // 내가 추가한 폰트 목록
                if (customFonts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "내가 추가한 커스텀 폰트 (${customFonts.size})",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        customFonts.forEach { customFont ->
                            val isSelected = customFont.id == selectedFontId
                            val customFamily = CustomFontManager.loadFontFamily(context, customFont.id) ?: androidx.compose.ui.text.font.FontFamily.Default

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) EdgeCyan.copy(alpha = 0.12f) else DarkBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) EdgeCyan else Color(0xFF333B4A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFontSelected(customFont.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customFont.displayName,
                                            color = if (isSelected) EdgeCyan else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = customFont.fileName,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Notification 알림 123 (Aa 가나다)",
                                            color = if (isSelected) CloudDancer else Color.LightGray,
                                            fontSize = 12.sp,
                                            fontFamily = customFamily
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                CustomFontManager.deleteCustomFont(context, customFont.fileName)
                                                customFonts = CustomFontManager.getCustomFonts(context)
                                                if (selectedFontId == customFont.id) {
                                                    onFontSelected("default")
                                                }
                                                Toast.makeText(context, "폰트가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "삭제",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onFontSelected(customFont.id) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = EdgeCyan,
                                                unselectedColor = Color.Gray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 기본 제공 폰트 프리셋 목록
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "기본 제공 프리셋 폰트",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFont.entries.forEach { fontOption ->
                        val isSelected = fontOption.id == selectedFontId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EdgeCyan.copy(alpha = 0.12f) else DarkBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) EdgeCyan else Color(0xFF333B4A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFontSelected(fontOption.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fontOption.displayName,
                                        color = if (isSelected) EdgeCyan else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = fontOption.description,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // 폰트 실시간 적용 미리보기 샘플
                                    Text(
                                        text = "Notification 알림 123 (Aa 한글 폰트)",
                                        color = if (isSelected) CloudDancer else Color.LightGray,
                                        fontSize = 12.sp,
                                        fontFamily = fontOption.toFontFamily()
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onFontSelected(fontOption.id) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = EdgeCyan,
                                        unselectedColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
