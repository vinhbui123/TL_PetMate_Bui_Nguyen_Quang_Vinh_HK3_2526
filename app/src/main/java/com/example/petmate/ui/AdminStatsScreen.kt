package com.example.petmate.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.model.ChartPointDto
import com.example.petmate.model.PieChartPointDto
import com.example.petmate.model.SystemStatsDto
import com.example.petmate.network.NetworkClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(onBack: () -> Unit) {
    var stats by remember { mutableStateOf<SystemStatsDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isLoading = true
            try {
                stats = NetworkClient.apiService.getSystemStats()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bảng điều khiển hệ thống", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3F51B5))
            }
        } else if (stats != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Stats Grid
                Text("Tổng quan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactStatItem(modifier = Modifier.weight(1f), "Người dùng", stats!!.totalUsers.toString(), Icons.Default.People, Color(0xFF2196F3))
                    CompactStatItem(modifier = Modifier.weight(1f), "Tổ chức", stats!!.totalOrganizations.toString(), Icons.Default.Business, Color(0xFF4CAF50))
                    CompactStatItem(modifier = Modifier.weight(1f), "Thú cưng", stats!!.totalPets.toString(), Icons.Default.Pets, Color(0xFFFF9800))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactStatItem(modifier = Modifier.weight(1f), "Tổng đơn", stats!!.totalAdoptions.toString(), Icons.Default.Assignment, Color(0xFF9C27B0))
                    CompactStatItem(modifier = Modifier.weight(1f), "Chờ duyệt", stats!!.pendingAdoptions.toString(), Icons.Default.HourglassEmpty, Color(0xFFF44336))
                    CompactStatItem(modifier = Modifier.weight(1f), "Báo cáo VP", stats!!.totalReports.toString(), Icons.Default.ReportProblem, Color(0xFFE91E63))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Line Chart Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Biểu đồ đường nhận nuôi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Text("Xu hướng đơn nhận nuôi (6 tháng qua)", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        if (!stats!!.adoptionTrend.isNullOrEmpty()) {
                            LineChart(data = stats!!.adoptionTrend!!, modifier = Modifier.fillMaxWidth().height(200.dp))
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("Chưa có dữ liệu", color = Color.Gray)
                            }
                        }
                    }
                }

                // Pie Chart Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phân bổ nội dung", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Text("Tỷ lệ các loại thú cưng đang có trên hệ thống", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        if (!stats!!.contentMix.isNullOrEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PieChart(data = stats!!.contentMix!!, modifier = Modifier.size(150.dp))
                                Spacer(modifier = Modifier.width(24.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    stats!!.contentMix!!.forEach { item ->
                                        PieLegendItem(item)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                Text("Chưa có dữ liệu", color = Color.Gray)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CompactStatItem(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier.aspectRatio(1.2f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun LineChart(data: List<ChartPointDto>, modifier: Modifier = Modifier) {
    val lineChartColor = Color(0xFF6200EA)
    Canvas(modifier = modifier) {
        val maxVal = data.maxOfOrNull { it.value }?.toFloat()?.coerceAtLeast(1f) ?: 1f
        val pointSpacing = size.width / (data.size - 1).coerceAtLeast(1)
        
        val points = data.mapIndexed { index, point ->
            val x = index * pointSpacing
            val y = size.height - (point.value.toFloat() / maxVal * size.height * 0.8f) // leave top 20% margin
            Offset(x, y)
        }

        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    // Bezier curve
                    val controlPoint1 = Offset(p1.x + pointSpacing / 2, p1.y)
                    val controlPoint2 = Offset(p1.x + pointSpacing / 2, p2.y)
                    cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        p2.x, p2.y
                    )
                }
            }
        }

        drawPath(
            path = path,
            color = lineChartColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw points and labels
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val valuePaint = android.graphics.Paint().apply {
            color = lineChartColor.toArgb()
            textSize = 12.sp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }

        points.forEachIndexed { index, offset ->
            drawCircle(color = lineChartColor, radius = 4.dp.toPx(), center = offset)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = offset)
            
            // X-axis label
            drawContext.canvas.nativeCanvas.drawText(
                data[index].label,
                offset.x,
                size.height + 16.dp.toPx(), // Push label below chart
                textPaint
            )
            // Value label
            drawContext.canvas.nativeCanvas.drawText(
                data[index].value.toString(),
                offset.x,
                offset.y - 8.dp.toPx(),
                valuePaint
            )
        }
    }
}

@Composable
fun PieChart(data: List<PieChartPointDto>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val total = data.sumOf { it.value }.toFloat().coerceAtLeast(1f)
        var startAngle = -90f
        
        data.forEach { point ->
            val sweepAngle = (point.value.toFloat() / total) * 360f
            val color = try { Color(android.graphics.Color.parseColor(point.colorHex)) } catch (e: Exception) { Color.Gray }
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = size.width / 4f) // Donut chart style
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun PieLegendItem(item: PieChartPointDto) {
    val color = try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch (e: Exception) { Color.Gray }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = item.label, fontSize = 14.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "(${item.value})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}
