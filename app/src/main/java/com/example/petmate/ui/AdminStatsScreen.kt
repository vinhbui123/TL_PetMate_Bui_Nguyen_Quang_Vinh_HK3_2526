package com.example.petmate.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmate.model.ChartPointDto
import com.example.petmate.model.PieChartPointDto
import com.example.petmate.model.SystemStatsDto
import com.example.petmate.network.NetworkClient
import com.example.petmate.ui.theme.PetMateTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

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

    AdminStatsContent(
        stats = stats,
        isLoading = isLoading,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsContent(
    stats: SystemStatsDto?,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bảng điều khiển hệ thống",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Thống kê & phân tích dữ liệu ứng dụng",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Trở về",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF6366F1))
            }
        } else if (stats != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chỉ số tổng quan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Cập nhật realtime",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Metric Grid (2 columns for spacious, clear title & icon layout)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatMetricCard(
                            title = "Người dùng",
                            value = stats.totalUsers,
                            icon = Icons.Default.Groups,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Tổ chức cứu hộ",
                            value = stats.totalOrganizations,
                            icon = Icons.Default.Domain,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatMetricCard(
                            title = "Thú cưng đăng tin",
                            value = stats.totalPets,
                            icon = Icons.Default.Pets,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Tổng nhận nuôi",
                            value = stats.totalAdoptions,
                            icon = Icons.AutoMirrored.Filled.FactCheck,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatMetricCard(
                            title = "Đơn chờ duyệt",
                            value = stats.pendingAdoptions,
                            icon = Icons.Default.HourglassTop,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Báo cáo vi phạm",
                            value = stats.totalReports,
                            icon = Icons.Default.Report,
                            color = Color(0xFFEC4899),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Line Chart Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Xu hướng nhận nuôi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Số lượng đơn nhận nuôi theo tháng (6 tháng gần nhất)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFEEF2FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (!stats.adoptionTrend.isNullOrEmpty()) {
                            LineChart(
                                data = stats.adoptionTrend,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có dữ liệu thống kê", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Pie Chart Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Phân bổ thú cưng theo loài",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Tỷ lệ danh mục thú cưng đang đăng tải",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF0FDF4), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (!stats.contentMix.isNullOrEmpty()) {
                            val totalPetsCount = stats.contentMix.sumOf { it.value }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Donut Chart with Center Total Text
                                Box(
                                    modifier = Modifier.size(170.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PieChart(
                                        data = stats.contentMix,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = NumberFormat.getNumberInstance(Locale.US).format(totalPetsCount),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Tổng thú cưng",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // 2-Column Grid Legends
                                PieLegendGrid(
                                    data = stats.contentMix,
                                    totalCount = totalPetsCount,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có dữ liệu phân bổ", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: Long,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.semantics { contentDescription = title },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = NumberFormat.getNumberInstance(Locale.US).format(value),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LineChart(data: List<ChartPointDto>, modifier: Modifier = Modifier) {
    val lineChartColor = Color(0xFF6366F1) // Modern Indigo Accent
    val gridColor = Color(0xFFE2E8F0)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val topPadding = 32.dp.toPx()
        val bottomPadding = 28.dp.toPx()
        val sidePadding = 16.dp.toPx()

        val chartWidth = width - (sidePadding * 2)
        val chartHeight = height - topPadding - bottomPadding

        val maxVal = data.maxOfOrNull { it.value }?.toFloat()?.coerceAtLeast(1f) ?: 1f
        val pointSpacing = chartWidth / (data.size - 1).coerceAtLeast(1)

        // Draw 3 horizontal gridlines
        val gridLinesCount = 3
        for (i in 0..gridLinesCount) {
            val y = topPadding + (chartHeight / gridLinesCount) * i
            drawLine(
                color = gridColor,
                start = Offset(sidePadding, y),
                end = Offset(width - sidePadding, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        val points = data.mapIndexed { index, point ->
            val x = sidePadding + (index * pointSpacing)
            val y = topPadding + chartHeight - (point.value.toFloat() / maxVal * chartHeight)
            Offset(x, y)
        }

        // Line Path
        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
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

        // Gradient Fill under curve
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, topPadding + chartHeight)
                lineTo(points.first().x, topPadding + chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineChartColor.copy(alpha = 0.25f),
                        lineChartColor.copy(alpha = 0.0f)
                    ),
                    startY = topPadding,
                    endY = topPadding + chartHeight
                )
            )
        }

        // Draw Line Curve
        drawPath(
            path = path,
            color = lineChartColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Native Paints for text
        val labelTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val valueTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#4338CA")
            textSize = 11.sp.toPx()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // Draw Data Points & Text
        points.forEachIndexed { index, offset ->
            // Outer Ring
            drawCircle(color = lineChartColor, radius = 5.5.dp.toPx(), center = offset)
            // Inner White Dot
            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = offset)

            // X-Axis Month Label
            drawContext.canvas.nativeCanvas.drawText(
                data[index].label,
                offset.x,
                height - 6.dp.toPx(),
                labelTextPaint
            )

            // Value Label above point
            drawContext.canvas.nativeCanvas.drawText(
                data[index].value.toString(),
                offset.x,
                offset.y - 8.dp.toPx(),
                valueTextPaint
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
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun PieLegendGrid(
    data: List<PieChartPointDto>,
    totalCount: Long,
    modifier: Modifier = Modifier
) {
    val total = totalCount.toFloat().coerceAtLeast(1f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    val percentage = (item.value.toFloat() / total * 100).let {
                        String.format(Locale.US, "%.1f%%", it)
                    }
                    PieLegendItem(
                        item = item,
                        percentage = percentage,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PieLegendItem(
    item: PieChartPointDto,
    percentage: String,
    modifier: Modifier = Modifier
) {
    val color = try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch (e: Exception) { Color.Gray }
    
    Surface(
        modifier = modifier,
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.US).format(item.value),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "• $percentage",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminStatsScreenPreview() {
    val sampleStats = SystemStatsDto(
        totalUsers = 1250,
        totalOrganizations = 45,
        totalPets = 320,
        totalAdoptions = 850,
        pendingAdoptions = 12,
        approvedAdoptions = 780,
        totalReports = 5,
        adoptionTrend = listOf(
            ChartPointDto("Jan", 10),
            ChartPointDto("Feb", 15),
            ChartPointDto("Mar", 12),
            ChartPointDto("Apr", 25),
            ChartPointDto("May", 20),
            ChartPointDto("Jun", 35)
        ),
        contentMix = listOf(
            PieChartPointDto("Chó", 150, "#FF9800"),
            PieChartPointDto("Mèo", 120, "#2196F3"),
            PieChartPointDto("Chim cảnh", 28, "#E91E63"),
            PieChartPointDto("Cá cảnh", 5, "#00BCD4"),
            PieChartPointDto("Thỏ", 10, "#9C27B0"),
            PieChartPointDto("Gia cầm", 12, "#795548"),
            PieChartPointDto("Khác", 5, "#4CAF50")
        )
    )

    PetMateTheme {
        AdminStatsContent(
            stats = sampleStats,
            isLoading = false,
            onBack = {}
        )
    }
}
