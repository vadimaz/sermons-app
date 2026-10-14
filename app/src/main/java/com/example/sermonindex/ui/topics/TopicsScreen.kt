package com.example.sermonindex.ui.topics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sermonindex.SermonApp
import com.example.sermonindex.data.model.TopicDto
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SearchTextField

@Composable
fun TopicsScreen(
    onTopicSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = SermonApp.instance.repository
    var allTopics by remember { mutableStateOf<List<TopicDto>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        repository.getTopics()
            .onSuccess {
                allTopics = it
                isLoading = false
            }
            .onFailure {
                errorMessage = it.localizedMessage ?: "Failed to load topics catalog."
                isLoading = false
            }
    }

    val filteredTopics = remember(allTopics, searchQuery) {
        if (searchQuery.isBlank()) {
            allTopics
        } else {
            allTopics.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (isLoading) {
        LoadingView(modifier = modifier, text = "Loading thematic catalog...")
        return
    }

    if (errorMessage != null && allTopics.isEmpty()) {
        ErrorView(
            message = errorMessage ?: "Failed to load topics",
            onRetry = {
                // Retry
            },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 96.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            SearchTextField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search among ${allTopics.size} biblical themes..."
            )
        }

        if (filteredTopics.isEmpty()) {
            EmptyStateView(
                title = "No topics found",
                subtitle = "Try searching with another keyword."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTopics) { topic ->
                    TopicRow(topic = topic, onClick = { onTopicSelect(topic.slug) })
                }
            }
        }
    }
}

@Composable
private fun TopicRow(
    topic: TopicDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${topic.sermonCount} sermons indexed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
