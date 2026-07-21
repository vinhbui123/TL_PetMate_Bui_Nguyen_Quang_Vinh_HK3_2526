package com.example.petmate.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.petmate.model.ReportRequest
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.*
import kotlinx.coroutines.launch
import android.widget.Toast

@Composable
fun ReportDialog(
    reportedPetId: Long? = null,
    reportedUserId: Long? = null,
    reportedMessageId: Long? = null,
    onDismissRequest: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val reasons = listOf(
        "Spam, lừa đảo",
        "Hình ảnh phản cảm, bạo lực",
        "Nội dung không phù hợp",
        "Thông tin sai sự thật",
        "Lý do khác"
    )
    
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = { if (!isSubmitting) onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Báo cáo vi phạm",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 20.dp),
                    textAlign = TextAlign.Center
                )
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Vui lòng chọn lý do báo cáo:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (reason == selectedReason),
                                    onClick = { selectedReason = reason }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (reason == selectedReason),
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentOrange)
                            )
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Mô tả chi tiết (Tùy chọn)", color = IconGray) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = InputBorder,
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextGray)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
                        enabled = !isSubmitting
                    ) {
                        Text("Hủy", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isSubmitting = true
                            coroutineScope.launch {
                                try {
                                    val request = ReportRequest(
                                        reportedPetId = reportedPetId,
                                        reportedUserId = reportedUserId,
                                        reportedMessageId = reportedMessageId,
                                        reason = selectedReason,
                                        description = description.takeIf { it.isNotBlank() }
                                    )
                                    NetworkClient.apiService.submitReport(request)
                                    onSuccess()
                                    onDismissRequest()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi khi gửi báo cáo: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isSubmitting = false
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Báo cáo", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
