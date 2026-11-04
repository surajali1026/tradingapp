package com.example.ui.markets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketCategory
import com.example.data.model.Ticker24h
import com.example.ui.theme.BearRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BullGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TVBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketListSheet(
    isOpen: Boolean,
    currentCategory: MarketCategory,
    tickers: List<Ticker24h>,
    watchlistSymbols: List<String>,
    isLoading: Boolean,
    onSelectCategory: (MarketCategory) -> Unit,
    onSelectSymbol: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onMoveWatchlistUp: (String) -> Unit,
    onMoveWatchlistDown: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Binance USD-M Futures",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search contract (e.g. BTC, ETH, SOL)", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = TVBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Market Category Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MarketCategory.values().forEach { category ->
                    val isSelected = category == currentCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TVBlue else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) TVBlue else DarkBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectCategory(category) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = category.title,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter tickers based on category and search query
            val filteredTickers = remember(tickers, currentCategory, watchlistSymbols, searchQuery) {
                var list = when (currentCategory) {
                    MarketCategory.ALL_FUTURES -> tickers
                    MarketCategory.TOP_100_VOLUME -> tickers.sortedByDescending { it.quoteVolume }.take(100)
                    MarketCategory.HIGHEST_VOLUME -> tickers.sortedByDescending { it.quoteVolume }
                    MarketCategory.TOP_GAINERS -> tickers.sortedByDescending { it.priceChangePercent }
                    MarketCategory.TOP_LOSERS -> tickers.sortedBy { it.priceChangePercent }
                    MarketCategory.WATCHLIST -> {
                        // Order according to watchlistSymbols order
                        val map = tickers.associateBy { it.symbol.uppercase() }
                        watchlistSymbols.mapNotNull { map[it.uppercase()] }
                    }
                }

                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.trim().uppercase()
                    list = list.filter { it.symbol.uppercase().contains(q) }
                }
                list
            }

            if (isLoading && filteredTickers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TVBlue, modifier = Modifier.size(36.dp))
                }
            } else if (filteredTickers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentCategory == MarketCategory.WATCHLIST)
                            "Your watchlist is empty.\nTap the star icon next to any coin to add."
                        else "No trading pairs found",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(filteredTickers, key = { _, item -> item.symbol }) { index, ticker ->
                        val isWatchlisted = watchlistSymbols.any { it.equals(ticker.symbol, ignoreCase = true) }

                        MarketTickerRow(
                            ticker = ticker,
                            isWatchlisted = isWatchlisted,
                            showReorderButtons = currentCategory == MarketCategory.WATCHLIST && searchQuery.isBlank(),
                            isFirst = index == 0,
                            isLast = index == filteredTickers.lastIndex,
                            onClick = {
                                onSelectSymbol(ticker.symbol)
                                onDismiss()
                            },
                            onToggleWatchlist = { onToggleWatchlist(ticker.symbol) },
                            onMoveUp = { onMoveWatchlistUp(ticker.symbol) },
                            onMoveDown = { onMoveWatchlistDown(ticker.symbol) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketTickerRow(
    ticker: Ticker24h,
    isWatchlisted: Boolean,
    showReorderButtons: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Star + Symbol Name + Volume
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleWatchlist,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Watchlist",
                    tint = if (isWatchlisted) BinanceGold else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = ticker.symbol,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Vol: ${formatVolume(ticker.quoteVolume)} USDT",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Right: Price + 24h Change Pill + Optional Reorder
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(ticker.lastPrice),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val isPositive = ticker.priceChangePercent >= 0
                val changeText = (if (isPositive) "+" else "") + String.format(Locale.US, "%.2f%%", ticker.priceChangePercent)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPositive) BullGreen else BearRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = changeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showReorderButtons) {
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    if (!isFirst) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (!isLast) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatVolume(vol: Double): String {
    return when {
        vol >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", vol / 1_000_000_000)
        vol >= 1_000_000 -> String.format(Locale.US, "%.2fM", vol / 1_000_000)
        vol >= 1_000 -> String.format(Locale.US, "%.2fK", vol / 1_000)
        else -> String.format(Locale.US, "%.2f", vol)
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.4f", price)
        price > 0.0001 -> String.format(Locale.US, "%.6f", price)
        else -> String.format(Locale.US, "%.8f", price)
    }
}
