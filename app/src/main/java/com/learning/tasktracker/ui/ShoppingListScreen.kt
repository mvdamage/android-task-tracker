package com.learning.tasktracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.tasktracker.R
import com.learning.tasktracker.data.ShoppingCategoryEntity
import com.learning.tasktracker.data.ShoppingItemEntity
import com.learning.tasktracker.ui.components.AnyDoDivider
import com.learning.tasktracker.ui.components.CircularTaskCheckbox
import com.learning.tasktracker.ui.components.HorizontalSuggestionPills
import com.learning.tasktracker.ui.components.QuickAddBar
import com.learning.tasktracker.ui.components.ShoppingProgressBar
import com.learning.tasktracker.ui.theme.extendedColors
import com.learning.tasktracker.ui.voice.ShoppingVoiceConfirmSheet
import com.learning.tasktracker.ui.voice.VoiceCaptureDialogs
import com.learning.tasktracker.ui.voice.VoiceCaptureFabColumn
import com.learning.tasktracker.ui.voice.rememberVoiceCaptureSession
import com.learning.tasktracker.voice.ShoppingVoiceLine
import com.learning.tasktracker.voice.ShoppingVoiceParser
import kotlinx.coroutines.launch

private val ShoppingVoiceLinesSaver = Saver<List<ShoppingVoiceLine>?, List<Any?>>(
    save = { lines ->
        lines?.flatMap { listOf(it.title, it.categoryId, it.truncated) }
    },
    restore = { saved ->
        if (saved.isEmpty()) null
        else saved.chunked(3).map { chunk ->
            ShoppingVoiceLine(
                title = chunk[0] as String,
                categoryId = chunk[1] as Long?,
                truncated = chunk[2] as Boolean
            )
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(viewModel: ShoppingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val titleHistory by viewModel.titleHistory.collectAsStateWithLifecycle()
    val categoryIdByTitleLower by viewModel.categoryIdByTitleLower.collectAsStateWithLifecycle()
    val pendingDeleteCategory by viewModel.pendingDeleteCategory.collectAsStateWithLifecycle()
    var newItemTitle by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var showInput by remember { mutableStateOf(false) }
    var showCategoriesSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var confirmClearChecked by remember { mutableStateOf(false) }
    var deleteCategoryItemCount by remember { mutableIntStateOf(0) }
    var voiceConfirmLines by rememberSaveable(stateSaver = ShoppingVoiceLinesSaver) {
        mutableStateOf(null)
    }
    var voiceEmptyError by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val inputFocusRequester = remember { FocusRequester() }
    val voiceSession = rememberVoiceCaptureSession(
        onTextRecognized = { text ->
            val lines = ShoppingVoiceParser.parseShoppingVoice(
                raw = text,
                categories = state.categories,
                categoryIdByTitleLower = categoryIdByTitleLower
            )
            if (lines.isEmpty()) {
                voiceEmptyError = true
            } else {
                voiceConfirmLines = lines
            }
        }
    )

    LaunchedEffect(pendingDeleteCategory) {
        deleteCategoryItemCount = pendingDeleteCategory?.let {
            viewModel.itemsInCategoryCount(it.id)
        } ?: 0
    }

    LaunchedEffect(showInput) {
        if (showInput) {
            inputFocusRequester.requestFocus()
        }
    }

    val suggestions = remember(newItemTitle, titleHistory) {
        ShoppingViewModel.shoppingInputSuggestions(titleHistory, newItemTitle)
    }
    val quickSuggestions = remember(titleHistory) {
        ShoppingViewModel.shoppingInputSuggestions(titleHistory, "")
    }

    fun addCurrentItem() {
        if (newItemTitle.isNotBlank()) {
            val categoryId = selectedCategoryId.takeIf { it > 0L }
            viewModel.addItem(newItemTitle, categoryId)
            newItemTitle = ""
            selectedCategoryId = 0L
            showInput = false
        }
    }

    fun addFromSuggestion(title: String) {
        viewModel.addSuggestedItem(title)
        newItemTitle = ""
        selectedCategoryId = 0L
        showInput = false
    }

    val categoryById = remember(state.categories) {
        state.categories.associateBy { it.id }
    }
    val showSectionHeaders = remember(state.groups, state.categories) {
        when {
            state.groups.size > 1 -> true
            state.groups.size == 1 ->
                state.groups.first().category != null || state.categories.isNotEmpty()
            else -> false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        "Покупки",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showCategoriesSheet = true },
                        modifier = Modifier.testTag(TestTags.CATEGORIES_BUTTON)
                    ) {
                        Icon(
                            Icons.Outlined.Category,
                            contentDescription = "Категории",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { confirmClearChecked = true },
                        enabled = state.checkedCount > 0,
                        modifier = Modifier.testTag(TestTags.CLEAR_CHECKED_SHOPPING)
                    ) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = "Очистить купленное",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            VoiceCaptureFabColumn(
                onPrimaryClick = { showInput = true },
                primaryContentDescription = "Добавить товар",
                onMicClick = voiceSession.onMicClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showInput) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .imePadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newItemTitle,
                            onValueChange = { newItemTitle = it },
                            placeholder = { Text("Что купить?") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(inputFocusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addCurrentItem() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        IconButton(
                            onClick = ::addCurrentItem,
                            enabled = newItemTitle.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Добавить")
                        }
                    }
                    if (suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (newItemTitle.isBlank()) "Подсказки" else "Похожие",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        HorizontalSuggestionPills(
                            suggestions = suggestions,
                            onSelect = ::addFromSuggestion
                        )
                    }
                    if (state.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ShoppingCategoryPickerRow(
                            categories = state.categories,
                            selectedCategoryId = selectedCategoryId,
                            onSelect = { selectedCategoryId = it }
                        )
                    }
                }
            } else {
                QuickAddBar(
                    placeholder = "Добавить в список…",
                    onClick = { showInput = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    testTag = TestTags.QUICK_ADD_SHOPPING
                )
                if (quickSuggestions.isNotEmpty()) {
                    HorizontalSuggestionPills(
                        suggestions = quickSuggestions,
                        onSelect = viewModel::addSuggestedItem,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            if (state.items.isEmpty()) {
                ShoppingEmptyState(
                    onQuickAdd = viewModel::addSuggestedItem,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            } else {
                ShoppingProgressBar(
                    checked = state.checkedCount,
                    total = state.items.size,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                if (state.checkedCount > 0) {
                    TextButton(
                        onClick = { confirmClearChecked = true },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text("Очистить купленное (${state.checkedCount})")
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 8.dp
                    )
                ) {
                    state.groups.forEachIndexed { groupIndex, group ->
                        if (showSectionHeaders) {
                            item(key = "header_${group.category?.id ?: "uncategorized"}") {
                                ShoppingCategorySectionHeader(
                                    category = group.category,
                                    topPadding = if (groupIndex == 0) 0.dp else 12.dp
                                )
                            }
                        }
                        items(group.items, key = { it.id }) { item ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 4 },
                                exit = fadeOut()
                            ) {
                                ShoppingItemRow(
                                    item = item,
                                    category = item.categoryId?.let { categoryById[it] },
                                    showCategoryBadge = !showSectionHeaders,
                                    onToggle = { viewModel.toggleChecked(item) },
                                    onEdit = { editingItem = item }
                                )
                            }
                            AnyDoDivider()
                        }
                    }
                }
            }
        }
    }

    if (showCategoriesSheet) {
        ShoppingCategoriesSheet(
            categories = state.categories,
            pendingDeleteCategory = pendingDeleteCategory,
            itemsInPendingDeleteCategory = deleteCategoryItemCount,
            onAddCategory = viewModel::addCategory,
            onRequestDeleteCategory = viewModel::requestDeleteCategory,
            onConfirmDeleteCategory = viewModel::confirmDeleteCategory,
            onDismissDeleteCategory = viewModel::dismissDeleteCategory,
            onDismiss = { showCategoriesSheet = false }
        )
    }

    VoiceCaptureDialogs(session = voiceSession)

    if (voiceEmptyError) {
        AlertDialog(
            onDismissRequest = { voiceEmptyError = false },
            title = { Text(stringResource(R.string.voice_error_title)) },
            text = { Text(stringResource(R.string.voice_error_no_shopping_items)) },
            confirmButton = {
                TextButton(onClick = { voiceEmptyError = false }) {
                    Text(stringResource(R.string.voice_ok))
                }
            }
        )
    }

    voiceConfirmLines?.let { lines ->
        ShoppingVoiceConfirmSheet(
            lines = lines,
            categories = state.categories,
            onDismiss = { voiceConfirmLines = null },
            onConfirm = { confirmed ->
                confirmed.forEach { line ->
                    viewModel.addItem(line.title, line.categoryId)
                }
                val count = confirmed.size
                voiceConfirmLines = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.voice_snackbar_shopping_added, count)
                    )
                }
            }
        )
    }

    editingItem?.let { item ->
        ShoppingItemEditorSheet(
            item = item,
            categories = state.categories,
            titleHistory = titleHistory,
            onDismiss = { editingItem = null },
            onSave = { title, categoryId ->
                viewModel.updateItem(item, title, categoryId)
                editingItem = null
            },
            onDelete = {
                viewModel.delete(item)
                editingItem = null
            }
        )
    }

    if (confirmClearChecked) {
        AlertDialog(
            onDismissRequest = { confirmClearChecked = false },
            title = { Text("Очистить купленное?") },
            text = {
                Text(
                    "Будут удалены ${state.checkedCount} " +
                        pluralCheckedItems(state.checkedCount) + " из списка."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChecked()
                        confirmClearChecked = false
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearChecked = false }) { Text("Отмена") }
            }
        )
    }
}

private fun pluralCheckedItems(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "купленных товаров"
        mod10 == 1 -> "купленный товар"
        mod10 in 2..4 -> "купленных товара"
        else -> "купленных товаров"
    }
}

@Composable
private fun ShoppingCategorySectionHeader(
    category: ShoppingCategoryEntity?,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = topPadding, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (category != null) {
            ShoppingCategoryBadge(category = category)
            Text(
                text = category.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = ShoppingCategoryPresets.colorFromArgb(category.colorArgb),
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Text(
                text = "БЕЗ КАТЕГОРИИ",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.extendedColors.sectionHeader,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    category: ShoppingCategoryEntity?,
    showCategoryBadge: Boolean = true,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularTaskCheckbox(
            checked = item.isChecked,
            onCheckedChange = onToggle
        )
        if (showCategoryBadge && category != null) {
            ShoppingCategoryBadge(
                category = category,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.isChecked) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (showCategoryBadge && category != null) 10.dp else 14.dp)
                .clickable(onClick = onEdit),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShoppingEmptyState(
    onQuickAdd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ShoppingCart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Список пуст",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Нажмите + или строку выше",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ShoppingViewModel.defaultSuggestions.take(3).forEach { example ->
                Text(
                    text = example,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onQuickAdd(example) }
                )
            }
        }
    }
}
