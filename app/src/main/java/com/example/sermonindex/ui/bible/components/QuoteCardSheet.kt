package com.example.sermonindex.ui.bible.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.example.sermonindex.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class QuoteCardTheme(
    val displayNameRes: Int,
    val previewBgColor: Color,
    val previewBorderColor: Color
) {
    MINIMAL(
        displayNameRes = R.string.quote_card_style_minimal,
        previewBgColor = Color(0xFFF3F4F6),
        previewBorderColor = Color(0xFFD1D5DB)
    ),
    SAGE_TEAL(
        displayNameRes = R.string.quote_card_style_teal,
        previewBgColor = Color(0xFF0F766E),
        previewBorderColor = Color(0x33000000)
    ),
    PAPER(
        displayNameRes = R.string.quote_card_style_paper,
        previewBgColor = Color(0xFFF7F2E7),
        previewBorderColor = Color(0xFFE8DFC8)
    ),
    CHARCOAL(
        displayNameRes = R.string.quote_card_style_charcoal,
        previewBgColor = Color(0xFF181A20),
        previewBorderColor = Color(0xFF2B2D37)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteCardSheet(
    verseText: String,
    reference: String,
    translationName: String,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTheme by remember { mutableStateOf(QuoteCardTheme.MINIMAL) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.quote_card_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Quote Card Preview (Always 1:1 square)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    QuoteCardView(
                        verseText = verseText,
                        reference = reference,
                        translationName = translationName,
                        theme = selectedTheme
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme selector chips with horizontal scroll
            Text(
                text = stringResource(R.string.reading_settings),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                QuoteCardTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = selectedTheme == theme,
                        onClick = { selectedTheme = theme },
                        label = { Text(stringResource(theme.displayNameRes)) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(theme.previewBgColor)
                                    .border(1.dp, theme.previewBorderColor, CircleShape)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Share Text
                OutlinedButton(
                    onClick = {
                        shareQuoteCardAsText(context, verseText, reference, translationName)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share_text))
                }

                // Share Image
                Button(
                    onClick = {
                        coroutineScope.launch {
                            shareQuoteCardAsImage(
                                context = context,
                                verseText = verseText,
                                reference = reference,
                                translationName = translationName,
                                theme = selectedTheme
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share_image))
                }
            }
        }
    }
}

@Composable
private fun QuoteCardView(
    verseText: String,
    reference: String,
    translationName: String,
    theme: QuoteCardTheme
) {
    val backgroundColor: Color
    val textColor: Color
    val accentColor: Color
    val borderColor: Color?

    when (theme) {
        QuoteCardTheme.MINIMAL -> {
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
            accentColor = MaterialTheme.colorScheme.primary
            borderColor = MaterialTheme.colorScheme.outlineVariant
        }
        QuoteCardTheme.SAGE_TEAL -> {
            backgroundColor = Color(0xFF0F766E)
            textColor = Color.White
            accentColor = Color(0xFFCCFBF1)
            borderColor = null
        }
        QuoteCardTheme.PAPER -> {
            backgroundColor = Color(0xFFF7F2E7)
            textColor = Color(0xFF2C241E)
            accentColor = Color(0xFF8C7355)
            borderColor = Color(0xFFE8DFC8)
        }
        QuoteCardTheme.CHARCOAL -> {
            backgroundColor = Color(0xFF181A20)
            textColor = Color(0xFFF3F4F6)
            accentColor = Color(0xFFE5C07B)
            borderColor = Color(0xFF2B2D37)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                } else Modifier
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.85f),
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "«$verseText»",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                ),
                color = textColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$reference • $translationName",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                ),
                color = accentColor
            )
        }
    }
}

private suspend fun shareQuoteCardAsImage(
    context: Context,
    verseText: String,
    reference: String,
    translationName: String,
    theme: QuoteCardTheme
) = withContext(Dispatchers.IO) {
    try {
        val bitmap = generateQuoteCardBitmap(verseText, reference, translationName, theme)
        val cacheDir = File(context.cacheDir, "quote_cards").apply { mkdirs() }
        val file = File(cacheDir, "quote_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, context.getString(R.string.action_share_image))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        android.util.Log.e("QuoteCardSheet", "Error sharing quote card: ${e.message}", e)
    }
}

private fun generateQuoteCardBitmap(
    verseText: String,
    reference: String,
    translationName: String,
    theme: QuoteCardTheme
): Bitmap {
    val width = 1080
    val height = 1080
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val (bgArgb, textArgb, accentArgb) = when (theme) {
        QuoteCardTheme.MINIMAL -> Triple(0xFFF3F4F6.toInt(), 0xFF1F2937.toInt(), 0xFF0D9488.toInt())
        QuoteCardTheme.SAGE_TEAL -> Triple(0xFF0F766E.toInt(), 0xFFFFFFFF.toInt(), 0xFFCCFBF1.toInt())
        QuoteCardTheme.PAPER -> Triple(0xFFF7F2E7.toInt(), 0xFF2C241E.toInt(), 0xFF8C7355.toInt())
        QuoteCardTheme.CHARCOAL -> Triple(0xFF181A20.toInt(), 0xFFF3F4F6.toInt(), 0xFFE5C07B.toInt())
    }

    // 1. Draw Background
    val bgPaint = Paint().apply {
        color = bgArgb
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // 2. Draw border for Minimal/Paper
    if (theme == QuoteCardTheme.MINIMAL || theme == QuoteCardTheme.PAPER) {
        val borderPaint = Paint().apply {
            color = if (theme == QuoteCardTheme.MINIMAL) 0xFFE5E7EB.toInt() else 0xFFE8DFC8.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat(), borderPaint)
    }

    // 3. Draw decorative quotation mark
    val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentArgb
        textSize = 96f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // 4. Text Paint for scripture
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textArgb
        textSize = 50f
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    }

    val contentWidth = width - 180
    val formattedText = "«$verseText»"
    val textLayout = StaticLayout.Builder.obtain(
        formattedText,
        0,
        formattedText.length,
        textPaint,
        contentWidth
    )
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(16f, 1.25f)
        .build()

    // 5. Reference Text Paint
    val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentArgb
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    val totalContentHeight = 96f + 32f + textLayout.height + 60f + 40f
    var currentY = (height - totalContentHeight) / 2f

    // Draw Quote Mark
    canvas.drawText("“", width / 2f, currentY + 70f, quotePaint)
    currentY += 120f

    // Draw Scripture Body
    canvas.save()
    canvas.translate((width - contentWidth) / 2f, currentY)
    textLayout.draw(canvas)
    canvas.restore()
    currentY += textLayout.height + 60f

    // Draw Reference
    val refText = "$reference • $translationName".uppercase()
    canvas.drawText(refText, width / 2f, currentY, refPaint)

    return bitmap
}

private fun shareQuoteCardAsText(
    context: Context,
    verseText: String,
    reference: String,
    translationName: String
) {
    val shareContent = "«$verseText»\n— $reference ($translationName)"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareContent)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.action_share_text))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
