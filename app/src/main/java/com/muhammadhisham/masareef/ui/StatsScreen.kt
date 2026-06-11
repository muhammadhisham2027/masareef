package com.muhammadhisham.masareef.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhammadhisham.masareef.data.Expense
import com.muhammadhisham.masareef.data.formatAmount
import com.muhammadhisham.masareef.data.toLocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val shortMonthFmt = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

@Composable
fun StatsScreen(expenses: List<Expense>) {
    var month by remember { mutableStateOf(YearMonth.now()) }

    val thisMonthExp = expenses.filter { YearMonth.from(it.toLocalDate()) == month }
    val lastMonthExp = expenses.filter { YearMonth.from(it.toLocalDate()) == month.minusMonths(1) }
    val total     = thisMonthExp.sumOf { it.amount }
    val lastTotal = lastMonthExp.sumOf { it.amount }
    val diff      = total - lastTotal
    val diffPct   = if (lastTotal > 0) (diff / lastTotal * 100) else 0.0

    val byCategory = thisMonthExp
        .groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // 6-month bar chart data
    val barData = (5 downTo 0).map { offset ->
        val m = YearMonth.now().minusMonths(offset.toLong())
        val s = expenses.filter { YearMonth.from(it.toLocalDate()) == m }.sumOf { it.amount }
        Pair(m.format(shortMonthFmt), s)
    }
    val barMax = barData.maxOfOrNull { it.second } ?: 1.0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Month nav ────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev month")
            }
            Text(month.format(monthFmt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            IconButton(onClick = { month = month.plusMonths(1) },
                enabled = month < YearMonth.now()) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
            }
        }

        // ── Summary card ─────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary,
                               MaterialTheme.colorScheme.secondary)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text("Total spent", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                Text(formatAmount(total),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.height(8.dp))
                if (lastTotal > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (diff <= 0) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                            null,
                            tint = if (diff <= 0) Green200 else Amber500,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${if (diff <= 0) "▼" else "▲"} ${formatAmount(kotlin.math.abs(diff))} " +
                            "(${String.format(Locale.US, "%.1f", kotlin.math.abs(diffPct))}%) vs last month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${thisMonthExp.size} transaction(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
            }
        }

        // ── 6-month bar chart ────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("6-month trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    barData.forEach { (label, value) ->
                        val isCurrentMonth = label == YearMonth.now().format(shortMonthFmt)
                        BarColumn(
                            label   = label,
                            value   = value,
                            maxVal  = barMax,
                            highlight = isCurrentMonth
                        )
                    }
                }
            }
        }

        // ── Category breakdown ───────────────────────────────────────────────
        if (byCategory.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("By category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    byCategory.forEach { (cat, sum) ->
                        CategoryBar(category = cat, amount = sum, total = total)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        // ── Top merchant ────────────────────────────────────────────────────
        val topMerchant = thisMonthExp
            .groupBy { it.merchant }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .maxByOrNull { it.value }

        topMerchant?.let { (name, amount) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏆", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Top merchant this month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(formatAmount(amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // ── Daily average ────────────────────────────────────────────────────
        if (thisMonthExp.isNotEmpty()) {
            val uniqueDays = thisMonthExp.map { it.toLocalDate() }.distinct().size
            val dailyAvg = if (uniqueDays > 0) total / uniqueDays else 0.0

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Daily average",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatAmount(dailyAvg),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Active days",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$uniqueDays days",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BarColumn(label: String, value: Double, maxVal: Double, highlight: Boolean) {
    val fraction = if (maxVal > 0) (value / maxVal).toFloat() else 0f
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "bar_$label"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(36.dp)
    ) {
        Box(
            Modifier
                .width(20.dp)
                .height(100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .width(20.dp)
                    .height((100 * animated).dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        if (highlight) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun CategoryBar(category: String, amount: Double, total: Double) {
    val fraction = if (total > 0) (amount / total).toFloat() else 0f
    val color = categoryColor(category)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    formatAmount(amount) + "  " +
                    String.format(Locale.US, "(%.0f%%)", fraction * 100),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            AnimatedProgressBar(
                fraction = fraction,
                color = color,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}
