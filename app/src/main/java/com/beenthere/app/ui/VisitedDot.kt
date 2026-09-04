package com.beenthere.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beenthere.app.R
import com.beenthere.app.ui.theme.BorderGray
import com.beenthere.app.ui.theme.Land
import com.beenthere.app.ui.theme.Visited

/**
 * Il pallino che segna/desegna un paese dalla UI nativa. E' la via di selezione
 * per gli stati troppo piccoli da colpire con il dito sul globo (Malta,
 * Singapore, Maldive): senza, la ricerca resterebbe un vicolo cieco.
 */
@Composable
fun VisitedDot(
    isVisited: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = if (isVisited) Visited else Land,
        label = "dotColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isVisited) Visited else BorderGray,
        label = "dotBorder"
    )
    val description = appString(if (isVisited) R.string.unmark_visited else R.string.mark_visited)
    val shape = RoundedCornerShape(10.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onToggle)
            .semantics { contentDescription = description }
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}
