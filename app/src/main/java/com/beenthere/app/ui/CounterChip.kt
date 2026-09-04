package com.beenthere.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beenthere.app.R
import com.beenthere.app.ui.theme.BorderGray
import com.beenthere.app.ui.theme.OnPanel
import com.beenthere.app.ui.theme.Panel
import com.beenthere.app.ui.theme.Visited

/**
 * "47 / 242 paesi". E' anche il pulsante che apre la lista dei visitati:
 * niente barre o icone in piu' sopra il globo.
 */
@Composable
fun CounterChip(
    visitedCount: Int,
    total: Int,
    isReady: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    val text = if (isReady) {
        appString(R.string.counter, visitedCount, total)
    } else {
        appString(R.string.counter_loading)
    }
    // Il numero dei visitati va in arancione ovunque cada nella frase tradotta.
    val label = buildAnnotatedString {
        append(text)
        if (isReady) {
            val needle = visitedCount.toString()
            val start = text.indexOf(needle)
            if (start >= 0) {
                addStyle(
                    SpanStyle(color = Visited, fontWeight = FontWeight.Bold),
                    start,
                    start + needle.length
                )
            }
        }
    }

    Text(
        text = label,
        color = OnPanel,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(shape)
            .background(Panel, shape)
            .border(1.dp, BorderGray, shape)
            .clickable(enabled = isReady, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
