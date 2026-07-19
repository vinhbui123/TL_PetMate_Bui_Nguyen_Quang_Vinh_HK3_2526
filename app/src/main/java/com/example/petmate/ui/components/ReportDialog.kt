package com.example.petmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.model.ReportRequest
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDialog(
    reportedPetId: Long? = null,
    reportedUserId: Long? = null,
    onDismissRequest: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val reasons = listOf(
        "Lừa đảo",
        "Thông tin sai sự thật",
        "Bán thú cưng bị bệnh/cấm",
        "Tài khoản giả mạo",
        "Lý do khác"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFE53935), // Red Warning Color
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (reportedPetId != null) "Báo cáo tin đăng" else "Báo cáo người dùng",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Vui lòng chọn lý do để chúng tôi xem xét:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Reason List
            LazyColumn(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reasons) { reason ->
                    val isSelected = selectedReason == reason
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) com.example.petmate.ui.theme.PrimaryPeach.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) com.example.petmate.ui.theme.PrimaryPeach else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedReason = reason }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = reason,
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) com.example.petmate.ui.theme.PrimaryPeach else Color.Black
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = com.example.petmate.ui.theme.PrimaryPeach,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description Box
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Mô tả chi tiết hơn (không bắt buộc)", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = com.example.petmate.ui.theme.PrimaryPeach,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (selectedReason != null) {
                        isSubmitting = true
                        coroutineScope.launch {
                            try {
                                val request = ReportRequest(
                                    reportedPetId = reportedPetId,
                                    reportedUserId = reportedUserId,
                                    reason = selectedReason!!,
                                    description = description
                                )
                                val response = NetworkClient.apiService.submitReport(request)
                                if (response.isSuccessful) {
                                    onSuccess()
                                    onDismissRequest()
                                } else {
                                    isSubmitting = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isSubmitting = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedReason != null && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.petmate.ui.theme.PrimaryPeach,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Gửi Báo Cáo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
