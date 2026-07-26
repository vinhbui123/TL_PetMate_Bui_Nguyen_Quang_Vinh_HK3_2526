package com.example.petmate.ui.org

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.ui.theme.PrimaryPeach

@Composable
fun OrgRegistrationStepperComponent(currentStep: Int, totalSteps: Int = 6) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..totalSteps) {
            StepCircle(step = i, isCurrent = i == currentStep, isCompleted = i < currentStep)
            
            if (i < totalSteps) {
                val lineColor by animateColorAsState(
                    targetValue = if (i < currentStep) PrimaryPeach else Color(0xFFEEEEEE),
                    label = "lineColor"
                )
                
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    color = lineColor,
                    thickness = 2.dp
                )
            }
        }
    }
}

@Composable
private fun StepCircle(step: Int, isCurrent: Boolean, isCompleted: Boolean) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isCurrent -> PrimaryPeach
            isCompleted -> PrimaryPeach.copy(alpha = 0.4f)
            else -> Color(0xFFEEEEEE)
        },
        label = "bgColor"
    )
    
    val size by animateDpAsState(
        targetValue = if (isCurrent) 36.dp else 28.dp,
        label = "size"
    )
    
    val textColor = when {
        isCurrent || isCompleted -> Color.White
        else -> Color(0xFFAAAAAA)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = step.toString(),
            color = textColor,
            fontSize = if (isCurrent) 16.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
