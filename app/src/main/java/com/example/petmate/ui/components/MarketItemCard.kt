package com.example.petmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.petmate.model.Pet
import com.example.petmate.ui.theme.*
import com.example.petmate.util.LocationHelper
import com.example.petmate.util.TimeHelper

@Composable
fun MarketItemCard(
    item: Pet,
    onClick: (Pet) -> Unit,
    userLatitude: Double? = null,
    userLongitude: Double? = null
) {
    val isOrg = item.organization != null
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image with Badge Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                if (!item.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = "Không có ảnh",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // Badge overlay at top-left
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = (if (isOrg) SuccessGreen else AccentOrange).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(bottomEnd = 16.dp)
                        )
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOrg) Icons.Default.HomeWork else Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                // Name and Likes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name ?: "Chưa có tên",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepBrown,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (item.likeCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null, 
                            tint = if (item.likeCount > 0) HeartRed else IconGray, 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.likeCount.toString(), 
                            style = MaterialTheme.typography.labelMedium, 
                            color = TextGray, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Price
                val formattedPrice = remember(item.price) {
                    val p = item.price
                    if (p.isNullOrEmpty() || p == "0" || p == "0.0" || p.lowercase().contains("miễn phí")) {
                        "Miễn phí"
                    } else {
                        try {
                            val amount = p.replace(Regex("[^0-9]"), "").toLong()
                            val formatter = java.text.DecimalFormat("#,###")
                            formatter.format(amount).replace(",", ".") + " đ"
                        } catch (_: Exception) {
                            p + " đ"
                        }
                    }
                }
                val isFree = formattedPrice == "Miễn phí"
                
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isFree) SuccessGreen else Color(0xFFE53935)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Location and Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = IconGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    val distanceText = remember(item.latitude, item.longitude, userLatitude, userLongitude) {
                        LocationHelper.getDistanceText(
                            userLatitude, userLongitude,
                            item.latitude,
                            item.longitude
                        )?.let { "Cách $it" } ?: "Chưa rõ"
                    }
                    val timeText = remember(item.createdAt) {
                        TimeHelper.getRelativeTime(item.createdAt)
                    }

                    Text(
                        text = "$distanceText • $timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = IconGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
