package com.example.petmate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.petmate.ui.theme.*

@Composable
fun RatingDialog(
    initialScore: Double = 5.0,
    initialComment: String = "",
    onDismissRequest: () -> Unit,
    onSubmit: (Double, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var score by remember { mutableStateOf(initialScore.toInt()) }
    var comment by remember { mutableStateOf(initialComment) }

    val ratingLabels = mapOf(
        1 to "Rất tệ 😞",
        2 to "Không hài lòng 👎",
        3 to "Bình thường 😐",
        4 to "Hài lòng 👍",
        5 to "Rất tuyệt vời 🌟"
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title & Subtitle
                Text(
                    text = "Đánh giá người bán",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepBrown,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Hãy chia sẻ trải nghiệm giao dịch của bạn nhé",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Star Rating Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        val isSelected = i <= score
                        val starScale by animateFloatAsState(
                            targetValue = if (i == score) 1.2f else 1.0f,
                            label = "starScale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .scale(starScale)
                                .clip(CircleShape)
                                .clickable { score = i }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Star $i",
                                tint = if (isSelected) Color(0xFFFFB800) else Color(0xFFE0E0E0),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic Rating Text Label
                Text(
                    text = ratingLabels[score] ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentOrange,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Comment Input Field
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { 
                        Text(
                            "Nhận xét chi tiết (ví dụ: phản hồi nhanh, thú cưng khỏe mạnh...)", 
                            color = IconGray,
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = DeepBrown)
                )

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Xóa đánh giá",
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Xóa đánh giá này",
                            color = ErrorRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                    ) {
                        Text("Hủy", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { onSubmit(score.toDouble(), comment.trim()) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Gửi", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

