package de.yanneckreiss.mlkittutorial.gas.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.yanneckreiss.mlkittutorial.entry.data.TaskViewModel.EntryViewModel
import de.yanneckreiss.mlkittutorial.gas.model.CylinderSizeInfoList
import de.yanneckreiss.mlkittutorial.gas.model.GasCylinder
import de.yanneckreiss.mlkittutorial.gas.viewmodel.GasCylinderViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GasCylinderTrackerScreen(
    onNavigateBack: () -> Unit,
    gasViewModel: GasCylinderViewModel = hiltViewModel(),
    entryViewModel: EntryViewModel = hiltViewModel()
) {
    val editingCylinder by gasViewModel.editingCylinder.collectAsStateWithLifecycle()
    val activeCylinder by gasViewModel.activeCylinder.collectAsStateWithLifecycle()
    val cylinderHistory by gasViewModel.cylinderHistory.collectAsStateWithLifecycle()
    val allSavedCylinders by gasViewModel.allSavedCylinders.collectAsStateWithLifecycle()

    // Date picker dialog states
    var showBoughtDatePicker by remember { mutableStateOf(false) }
    var showUsageDatePicker by remember { mutableStateOf(false) }

    // State for collapsable Service Dates & Details card
    var showServiceDetails by remember { mutableStateOf(false) }

    // Snackbar host state for feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isEditingSaved = allSavedCylinders.any { it.id == editingCylinder.id }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gas Cylinder Tracker",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isEditingSaved) "Editing Saved Cylinder" else "Log New Gas Cylinder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                },
                actions = {
                    // + New Cylinder Action Button
                    IconButton(onClick = { gasViewModel.resetToNewCylinder() }) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Log New Cylinder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { gasViewModel.onCycleColor() }) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = "Cycle Cylinder Color",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ========================================================
            // 1. SWIPEABLE CAROUSEL FOR SAVED CYLINDERS (WITH DOTS INDICATOR & + OPTION)
            // ========================================================
            item {
                SavedCylindersSwipeableCarousel(
                    savedCylinders = allSavedCylinders,
                    focusedCylinderId = editingCylinder.id,
                    onSelectCylinder = { gasViewModel.loadCylinderForEditing(it) },
                    onAddNewCylinder = { gasViewModel.resetToNewCylinder() }
                )
            }

            // ========================================================
            // 2. HERO PROPANE GAS CYLINDER VISUAL PREVIEW & CAROUSEL
            // ========================================================
            item {
                CylinderHeroVisualCard(
                    savedCylinders = allSavedCylinders,
                    focusedCylinder = editingCylinder,
                    isSavedCylinder = isEditingSaved,
                    onSelectCylinder = { gasViewModel.loadCylinderForEditing(it) },
                    onAddNewCylinder = { gasViewModel.resetToNewCylinder() },
                    onTapCycleColor = { gasViewModel.onCycleColor() }
                )
            }

            // ========================================================
            // 3. CYLINDER CONFIGURATION (STATUS, CAPACITY, COLOR & SAVE BUTTON)
            // ========================================================
            item {
                DynamicColorPaletteSection(
                    cylinder = editingCylinder,
                    isSavedCylinder = isEditingSaved,
                    colors = gasViewModel.availableColors,
                    onSizeSelected = { size -> gasViewModel.onSizeSelected(size) },
                    onStatusSelected = { isActive -> gasViewModel.onIsActiveChanged(isActive) },
                    onColorSelected = { color -> gasViewModel.onColorSelected(color) },
                    onTapCycleColor = { gasViewModel.onCycleColor() },
                    onSaveCylinder = {
                        gasViewModel.saveActiveCylinder()
                        coroutineScope.launch {
                            val actionMsg = if (isEditingSaved) "Updated ${editingCylinder.sizeKg} kg cylinder details!" else "Logged new ${editingCylinder.sizeKg} kg cylinder!"
                            snackbarHostState.showSnackbar(message = actionMsg)
                        }
                    }
                )
            }

            // ========================================================
            // 4. COLLAPSABLE SERVICE DATES & DETAILS FORM (ANIMATED)
            // ========================================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Collapsable Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showServiceDetails = !showServiceDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Service Dates & Details (Optional)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (showServiceDetails) "Tap to hide dates, supplier & notes" else "Tap to edit bought date, usage start & notes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { showServiceDetails = !showServiceDetails }
                            ) {
                                Icon(
                                    imageVector = if (showServiceDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Details",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Animated Visibility for opening/closing Service Dates & Details
                        AnimatedVisibility(
                            visible = showServiceDetails,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Date Pickers: Bought Date & Usage Start Date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    DatePickerDisplayCard(
                                        modifier = Modifier.weight(1f),
                                        label = "Bought Date",
                                        emoji = "🛒",
                                        date = editingCylinder.boughtDate,
                                        onClick = { showBoughtDatePicker = true }
                                    )

                                    DatePickerDisplayCard(
                                        modifier = Modifier.weight(1f),
                                        label = "Usage Start",
                                        emoji = "🔥",
                                        date = editingCylinder.usageStartDate,
                                        onClick = { showUsageDatePicker = true }
                                    )
                                }

                                // Usage Insights Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Duration in Service",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${editingCylinder.daysInUse} days active",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Stored Prior",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${editingCylinder.daysInStorageBeforeUse} days storage",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Supplier / Depot text field
                                OutlinedTextField(
                                    value = editingCylinder.supplier,
                                    onValueChange = { gasViewModel.onSupplierChanged(it) },
                                    label = { Text("Supplier / Brand (Optional)") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Storefront, contentDescription = null)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                // Notes text field
                                OutlinedTextField(
                                    value = editingCylinder.notes,
                                    onValueChange = { gasViewModel.onNotesChanged(it) },
                                    label = { Text("Notes / Location (e.g. Kitchen Stove)") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.EditNote, contentDescription = null)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 5. YEARLY CYLINDER LOG HISTORY
            // ========================================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yearly Gas Cylinder History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${cylinderHistory.size} logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (cylinderHistory.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No cylinder exchanges logged yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = cylinderHistory,
                    key = { index, cylinder -> "${cylinder.id}_$index" }
                ) { _, cylinder ->
                    CylinderHistoryItemCard(
                        cylinder = cylinder,
                        isCurrentActive = cylinder.id == activeCylinder.id,
                        isFocusedForEditing = cylinder.id == editingCylinder.id,
                        onSelectForEditing = { gasViewModel.loadCylinderForEditing(it) },
                        onDelete = { gasViewModel.deleteCylinder(cylinder.id) }
                    )
                }
            }
        }
    }

    // Modal Date Pickers
    if (showBoughtDatePicker) {
        SimpleDatePickerDialog(
            initialDate = editingCylinder.boughtDate,
            onDateSelected = { date ->
                gasViewModel.onBoughtDateSelected(date)
                showBoughtDatePicker = false
            },
            onDismiss = { showBoughtDatePicker = false }
        )
    }

    if (showUsageDatePicker) {
        SimpleDatePickerDialog(
            initialDate = editingCylinder.usageStartDate,
            onDateSelected = { date ->
                gasViewModel.onUsageStartDateSelected(date)
                showUsageDatePicker = false
            },
            onDismiss = { showUsageDatePicker = false }
        )
    }
}

/**
 * Swipeable Carousel for Saved Cylinders.
 * Allows horizontal swiping between past cylinders and a + New Cylinder option.
 * Displays page dots representing visited and upcoming cylinders with peek preview.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedCylindersSwipeableCarousel(
    savedCylinders: List<GasCylinder>,
    focusedCylinderId: String,
    onSelectCylinder: (GasCylinder) -> Unit,
    onAddNewCylinder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val totalPages = savedCylinders.size + 1
    val pagerState = rememberPagerState(initialPage = 0) { totalPages }

    // Synchronize carousel page when focusedCylinderId changes externally
    LaunchedEffect(focusedCylinderId, savedCylinders) {
        val targetPage = if (savedCylinders.none { it.id == focusedCylinderId }) {
            0
        } else {
            savedCylinders.indexOfFirst { it.id == focusedCylinderId } + 1
        }
        if (targetPage in 0 until totalPages && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Synchronize focused cylinder when user swipes the carousel
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) {
            if (savedCylinders.any { it.id == focusedCylinderId }) {
                onAddNewCylinder()
            }
        } else {
            val cylinderIndex = pagerState.currentPage - 1
            if (cylinderIndex in savedCylinders.indices) {
                val selected = savedCylinders[cylinderIndex]
                if (selected.id != focusedCylinderId) {
                    onSelectCylinder(selected)
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with title and swipe indicator status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saved Cylinders Carousel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Swipe left/right to browse cylinders",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Cylinder",
                            tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < totalPages - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < totalPages - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Cylinder",
                            tint = if (pagerState.currentPage < totalPages - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            // Swipeable HorizontalPager with peek preview of upcoming/previous cylinders
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) { pageIndex ->
                if (pageIndex == 0) {
                    // Page 0: + New Cylinder Option Card
                    Surface(
                        onClick = onAddNewCylinder,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add New Cylinder",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "+ Log New Cylinder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Configure details below to log a new propane cylinder exchange",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Page 1..N: Saved Cylinder Card
                    val cylinder = savedCylinders[pageIndex - 1]
                    val isFocused = cylinder.id == focusedCylinderId

                    Surface(
                        onClick = { onSelectCylinder(cylinder) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Color circle preview badge
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(cylinder.color)
                                    .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${cylinder.sizeKg}k",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${cylinder.sizeKg} kg Cylinder",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (cylinder.isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "FINISHED",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Started: ${cylinder.usageStartDate} • ${cylinder.daysInUse}d in service",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = cylinder.supplier.ifBlank { "Gas Cylinder" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Pager Dots Indicator Bar (Visited vs Active vs Upcoming)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalPages) {
                        val isSelected = i == pagerState.currentPage
                        val isVisited = i < pagerState.currentPage

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 22.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isVisited -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    }
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(i)
                                    }
                                }
                        )
                    }
                }

                // Subtitle explaining visited & total cylinders count
                Text(
                    text = if (pagerState.currentPage == 0) {
                        "New Cylinder • ${savedCylinders.size} saved cylinder(s) available"
                    } else {
                        "Visited ${pagerState.currentPage} of $totalPages • Swipe left/right"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Hero Visual Preview Card displaying a swipeable 3D-styled Cylinder carousel with page peeking,
 * pager indicator dots for visited/upcoming cylinders, and + option for logging new cylinders.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CylinderHeroVisualCard(
    savedCylinders: List<GasCylinder>,
    focusedCylinder: GasCylinder,
    isSavedCylinder: Boolean,
    onSelectCylinder: (GasCylinder) -> Unit,
    onAddNewCylinder: () -> Unit,
    onTapCycleColor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val totalPages = savedCylinders.size + 1
    val pagerState = rememberPagerState(initialPage = 0) { totalPages }

    // Synchronize carousel page when focusedCylinder changes externally
    LaunchedEffect(focusedCylinder.id, savedCylinders) {
        val targetPage = if (savedCylinders.none { it.id == focusedCylinder.id }) {
            0
        } else {
            savedCylinders.indexOfFirst { it.id == focusedCylinder.id } + 1
        }
        if (targetPage in 0 until totalPages && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Synchronize focused cylinder when user swipes the hero carousel
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) {
            if (savedCylinders.any { it.id == focusedCylinder.id }) {
                onAddNewCylinder()
            }
        } else {
            val cylinderIndex = pagerState.currentPage - 1
            if (cylinderIndex in savedCylinders.indices) {
                val selected = savedCylinders[cylinderIndex]
                if (selected.id != focusedCylinder.id) {
                    onSelectCylinder(selected)
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (focusedCylinder.isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline)
                    )
                    Text(
                        text = if (focusedCylinder.isActive) "ACTIVE IN SERVICE" else "USED UP / FINISHED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (focusedCylinder.isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSavedCylinder) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isSavedCylinder) "Editing Focused Cylinder" else "+ New Cylinder",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSavedCylinder) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Swipeable 3D Visual HorizontalPager with Page Peeking
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateBefore,
                        contentDescription = "Previous Cylinder",
                        tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(210.dp)
                ) { pageIndex ->
                    val displayCylinder = if (pageIndex == 0) focusedCylinder else savedCylinders[pageIndex - 1]
                    val isPlaceholder = pageIndex == 0

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = if (pageIndex == pagerState.currentPage) 2.dp else 1.dp,
                            color = if (pageIndex == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PropaneGasCylinderComposable(
                                cylinder = displayCylinder,
                                isPlaceholderNew = isPlaceholder,
                                onClick = onTapCycleColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (pagerState.currentPage < totalPages - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < totalPages - 1,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateNext,
                        contentDescription = "Next Cylinder",
                        tint = if (pagerState.currentPage < totalPages - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // Quick Spec Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickSpecBadge(label = "Capacity", value = "${focusedCylinder.sizeKg} kg")
                QuickSpecBadge(label = "Days Active", value = "${focusedCylinder.daysInUse}d")
                QuickSpecBadge(label = "Est. Left", value = "${focusedCylinder.estimatedRemainingDays}d")
            }

            // Pager Dots Indicator Bar (Visited vs Active vs Upcoming)
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalPages) {
                        val isSelected = i == pagerState.currentPage
                        val isVisited = i < pagerState.currentPage

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 22.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isVisited -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    }
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(i)
                                    }
                                }
                        )
                    }
                }

                Text(
                    text = if (pagerState.currentPage == 0) {
                        "New Cylinder Preview • Swipe 3D view to browse saved"
                    } else {
                        "Visited ${pagerState.currentPage} of $totalPages cylinders • Swipe 3D view"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickSpecBadge(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Cylinder Configuration Section.
 * Configures focused cylinder status, size capacity (4kg, 9kg, 14kg, 48kg), color palette,
 * and includes the primary Save / Update button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicColorPaletteSection(
    cylinder: GasCylinder,
    isSavedCylinder: Boolean,
    colors: List<Color>,
    onSizeSelected: (Int) -> Unit,
    onStatusSelected: (Boolean) -> Unit,
    onColorSelected: (Color) -> Unit,
    onTapCycleColor: () -> Unit,
    onSaveCylinder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSavedCylinder) "Edit Cylinder Configuration" else "New Cylinder Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Size capacity, status & color theme",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    onClick = onTapCycleColor,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Cycle Color",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Cycle Color",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 1. CYLINDER STATUS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cylinder Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = cylinder.isActive,
                        onClick = { onStatusSelected(true) },
                        leadingIcon = {
                            if (cylinder.isActive) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        label = { Text("Active (In Use)") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = !cylinder.isActive,
                        onClick = { onStatusSelected(false) },
                        leadingIcon = {
                            if (!cylinder.isActive) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        label = { Text("Used Up (Finished)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 2. CYLINDER SIZE CAPACITY SELECTION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cylinder Size (Capacity)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CylinderSizeInfoList.forEach { info ->
                        val isSelected = cylinder.sizeKg == info.sizeKg
                        Surface(
                            onClick = { onSizeSelected(info.sizeKg) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = info.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 3. DYNAMIC COLOR SWATCHES PALETTE
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Color Palette",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap swatch below to recolor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(colors) { color ->
                        val isSelected = color == cylinder.color
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelected(color) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = Color.Black.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected Color",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 4. PRIMARY SAVE / UPDATE BUTTON
            Button(
                onClick = onSaveCylinder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isSavedCylinder) Icons.Default.Save else Icons.Default.AddCircle,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSavedCylinder) "Update Focused ${cylinder.sizeKg} kg Cylinder" else "Save & Log New ${cylinder.sizeKg} kg Cylinder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tappable card displaying a picked date and triggering the date picker modal.
 */
@Composable
private fun DatePickerDisplayCard(
    label: String,
    emoji: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$emoji $label",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Card representing an individual logged cylinder in the history log list.
 */
@Composable
private fun CylinderHistoryItemCard(
    cylinder: GasCylinder,
    isCurrentActive: Boolean,
    isFocusedForEditing: Boolean,
    onSelectForEditing: (GasCylinder) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = { onSelectForEditing(cylinder) },
        shape = RoundedCornerShape(16.dp),
        color = if (isFocusedForEditing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        border = if (isFocusedForEditing) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Color Circle Preview
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(cylinder.color)
                    .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${cylinder.sizeKg}k",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Cylinder Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${cylinder.sizeKg} kg Cylinder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (isCurrentActive) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "FINISHED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Started: ${cylinder.usageStartDate} • ${cylinder.daysInUse}d in service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (cylinder.notes.isNotBlank()) {
                    Text(
                        text = cylinder.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onSelectForEditing(cylinder) }) {
                    Icon(
                        imageVector = if (isFocusedForEditing) Icons.Default.Edit else Icons.Outlined.Edit,
                        contentDescription = "Edit Cylinder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Cylinder Log",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Simple Material 3 DatePickerDialog wrapper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val instant = Instant.fromEpochMilliseconds(selectedMillis)
                        val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                        onDateSelected(localDate)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Select Date", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
