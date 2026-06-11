package com.muhammadhisham.masareef.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class OnboardPage(val emoji: String, val title: String, val body: String)

private val pages = listOf(
    OnboardPage("💸", "Track every piaster",
        "Log expenses manually in seconds — amount, merchant, category, note. All stored on your phone."),
    OnboardPage("📩", "Bank SMS, auto-detected",
        "Masareef reads your bank's debit SMS and records the transaction automatically — no typing needed."),
    OnboardPage("📊", "Understand your spending",
        "Monthly trends, category breakdowns, top merchants, and day-of-week patterns at a glance."),
    OnboardPage("🔒", "Completely private",
        "Zero cloud. Zero account. Everything lives on your device and never leaves it.")
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(page) {
        visible = false
        delay(80)
        visible = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary,
                           MaterialTheme.colorScheme.secondary)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pages[page].emoji, fontSize = 72.sp)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        pages[page].title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        pages[page].body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    val size by animateFloatAsState(
                        if (i == page) 24f else 8f, tween(300), label = "dot_$i")
                    Box(
                        Modifier
                            .height(8.dp)
                            .size(size.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == page) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            if (page < pages.lastIndex) {
                Button(
                    onClick = { page++ },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Next", style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Skip", color = MaterialTheme.colorScheme.onPrimary) }
            } else {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Get started", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}
