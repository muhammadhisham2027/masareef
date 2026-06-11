package com.muhammadhisham.masareef.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun categoryIcon(category: String): ImageVector = when (category) {
    "Food"          -> Icons.Filled.Fastfood
    "Groceries"     -> Icons.Filled.ShoppingCart
    "Transport"     -> Icons.Filled.DirectionsCar
    "Shopping"      -> Icons.Filled.ShoppingBag
    "Bills"         -> Icons.Filled.Receipt
    "Health"        -> Icons.Filled.LocalHospital
    "Entertainment" -> Icons.Filled.Movie
    else            -> Icons.Filled.Sell
}

fun categoryColor(category: String): Color = when (category) {
    "Food"          -> Color(0xFFFF6B6B)
    "Groceries"     -> Color(0xFF51CF66)
    "Transport"     -> Color(0xFF339AF0)
    "Shopping"      -> Color(0xFFCC5DE8)
    "Bills"         -> Color(0xFFFF922B)
    "Health"        -> Color(0xFF20C997)
    "Entertainment" -> Color(0xFFFFD43B)
    else            -> Color(0xFF868E96)
}

@Composable
fun CategoryBadge(category: String, size: Dp = 44.dp) {
    val color = categoryColor(category)
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                categoryIcon(category),
                contentDescription = category,
                tint = color,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0)),
                    start  = Offset(translateAnim - 500f, 0f),
                    end    = Offset(translateAnim, 0f)
                )
            )
    )
}

@Composable
fun PulsingDot(color: Color, size: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue  = 1.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * A horizontal progress bar drawn with two nested Boxes.
 * No custom layout modifier — avoids any measure/layout API changes between versions.
 */
@Composable
fun AnimatedProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFFE0E0E0)
) {
    val animatedFraction by animateFloatAsState(
        targetValue   = fraction.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "progress"
    )
    Box(modifier.clip(CircleShape).background(trackColor)) {
        Box(
            Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}
