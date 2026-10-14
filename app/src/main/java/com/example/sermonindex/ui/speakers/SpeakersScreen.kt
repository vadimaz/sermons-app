package com.example.sermonindex.ui.speakers

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.sermonindex.SermonApp
import com.example.sermonindex.data.model.SpeakerDto
import com.example.sermonindex.ui.components.EmptyStateView
import com.example.sermonindex.ui.components.ErrorView
import com.example.sermonindex.ui.components.LoadingView
import com.example.sermonindex.ui.components.SearchTextField

@Composable
fun SpeakersScreen(
    onSpeakerSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = SermonApp.instance.repository
    var allSpeakers by remember { mutableStateOf<List<SpeakerDto>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadSpeakers() {
        isLoading = true
        errorMessage = null
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).run {
            // Use repository call
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        repository.getSpeakers()
            .onSuccess {
                allSpeakers = it
                isLoading = false
            }
            .onFailure {
                errorMessage = it.localizedMessage ?: "Failed to load speakers list."
                isLoading = false
            }
    }

    val filteredSpeakers = remember(allSpeakers, searchQuery) {
        if (searchQuery.isBlank()) {
            allSpeakers
        } else {
            allSpeakers.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (isLoading) {
        LoadingView(modifier = modifier, text = "Loading preachers directory...")
        return
    }

    if (errorMessage != null && allSpeakers.isEmpty()) {
        ErrorView(
            message = errorMessage ?: "Failed to load speakers",
            onRetry = {
                isLoading = true
                errorMessage = null
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
        // Search bar
        Box(modifier = Modifier.padding(16.dp)) {
            SearchTextField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search among ${allSpeakers.size} preachers..."
            )
        }

        if (filteredSpeakers.isEmpty()) {
            EmptyStateView(
                title = "No preachers found",
                subtitle = "Try adjusting your search criteria."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSpeakers) { speaker ->
                    SpeakerRow(speaker = speaker, onClick = { onSpeakerSelect(speaker.slug) })
                }
            }
        }
    }
}

@Composable
private fun SpeakerRow(
    speaker: SpeakerDto,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (speaker.image.isNotBlank()) {
                AsyncImage(
                    model = speaker.image,
                    contentDescription = speaker.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = speaker.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${speaker.sermonCount} sermons • ${speaker.audioCount} audio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
