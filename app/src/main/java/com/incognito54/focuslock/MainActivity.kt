package com.incognito54.focuslock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.delay


private val FocusBlue = Color(0xFF2563EB)
private val FocusPurple = Color(0xFF7C3AED)
private val FocusBackground = Color(0xFFF5F7FF)
private val FocusCard = Color.White
private val FocusDarkText = Color(0xFF172033)
private val FocusMutedText = Color(0xFF667085)


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = FocusBackground
            ) {
                FocusLockScreen(this)
            }
        }
    }
}


fun isAccessibilityEnabled(context: Context): Boolean {

    val enabledServices =
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

    return enabledServices?.contains(
        "${context.packageName}/com.incognito54.focuslock.FocusAccessibilityService"
    ) == true
}


data class InstalledApp(
    val name: String,
    val packageName: String
)


fun getInstalledApps(context: Context): List<InstalledApp> {

    val packageManager = context.packageManager

    val intent = Intent(
        Intent.ACTION_MAIN,
        null
    ).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    return packageManager
        .queryIntentActivities(intent, 0)
        .map {
            InstalledApp(
                name = it.loadLabel(packageManager).toString(),
                packageName = it.activityInfo.packageName
            )
        }
        .distinctBy {
            it.packageName
        }
        .sortedBy {
            it.name.lowercase()
        }
}


@Composable
fun FocusLockScreen(context: Context) {

    val preferences = remember {
        context.getSharedPreferences(
            "focus_lock",
            Context.MODE_PRIVATE
        )
    }

    var installedApps by remember {
        mutableStateOf(
            getInstalledApps(context)
        )
    }

    var selectedApps by remember {
        mutableStateOf(
            preferences
                .getStringSet(
                    "selected_apps",
                    emptySet()
                )
                ?.toSet()
                ?: emptySet()
        )
    }

    var endTime by remember {
        mutableLongStateOf(
            preferences.getLong(
                "end_time",
                0L
            )
        )
    }

    var focusStarted by remember {
        mutableStateOf(
            endTime > System.currentTimeMillis()
        )
    }

    var daysInput by remember {
        mutableStateOf("0")
    }

    var hoursInput by remember {
        mutableStateOf("0")
    }

    var minutesInput by remember {
        mutableStateOf("30")
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var secondsLeft by remember {
        mutableLongStateOf(
            if (focusStarted) {
                (endTime - System.currentTimeMillis()) / 1000
            } else {
                0
            }
        )
    }

    var settingsScreen by remember {
        mutableStateOf(false)
    }

    val days =
        daysInput.toLongOrNull() ?: 0L

    val hours =
        hoursInput.toLongOrNull() ?: 0L

    val minutes =
        minutesInput.toLongOrNull() ?: 0L

    val totalDurationMinutes =
        days * 24L * 60L +
                hours * 60L +
                minutes

    val maxDurationMinutes =
        30L * 24L * 60L

    val durationValid =
        totalDurationMinutes in
                1L..maxDurationMinutes &&
                hours in 0L..23L &&
                minutes in 0L..59L &&
                (
                        days < 30L ||
                                (
                                        days == 30L &&
                                                hours == 0L &&
                                                minutes == 0L
                                        )
                        )

    val durationSummary =
        when {
            !durationValid ->
                "Enter a duration from 1 minute to 30 days."

            days > 0L && hours > 0L && minutes > 0L ->
                "${days}d ${hours}h ${minutes}m"

            days > 0L && hours > 0L ->
                "${days}d ${hours}h"

            days > 0L && minutes > 0L ->
                "${days}d ${minutes}m"

            days > 0L ->
                "${days}d"

            hours > 0L && minutes > 0L ->
                "${hours}h ${minutes}m"

            hours > 0L ->
                "${hours}h"

            else ->
                "${minutes}m"
        }

    BackHandler(
        enabled = focusStarted
    ) {
        // Back disabled during Focus Lock.
    }

    /*
     * FOCUS SESSION TIMER
     *
     * No Emergency Access.
     *
     * The selected apps remain blocked
     * until the Focus Session ends.
     */
    LaunchedEffect(
        focusStarted
    ) {

        if (focusStarted) {

            while (true) {

                val remaining =
                    (
                            endTime -
                                    System.currentTimeMillis()
                            ) / 1000

                secondsLeft =
                    remaining.coerceAtLeast(0)

                if (remaining <= 0) {

                    focusStarted = false

                    preferences.edit()
                        .remove("end_time")
                        .apply()

                    context.stopService(
                        Intent(
                            context,
                            FocusService::class.java
                        )
                    )

                    break
                }

                delay(1000)
            }
        }
    }

    val filteredApps =
        installedApps.filter { app ->

            searchQuery.isBlank() ||
                    app.name.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||
                    app.packageName.contains(
                        searchQuery,
                        ignoreCase = true
                    )
        }

    val selectedInstalledApps =
        filteredApps
            .filter {
                selectedApps.contains(
                    it.packageName
                )
            }
            .sortedBy {
                it.name.lowercase()
            }

    val unselectedInstalledApps =
        filteredApps
            .filter {
                !selectedApps.contains(
                    it.packageName
                )
            }
            .sortedBy {
                it.name.lowercase()
            }

    if (!focusStarted) {

        if (settingsScreen) {

            SettingsScreen(
                context = context,
                onBack = {
                    settingsScreen = false
                }
            )

            return
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
        ) {

            /*
             * HEADER
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Focus Lock",
                        style =
                            MaterialTheme.typography.headlineMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            FocusDarkText
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            "Protect your focus. Control your distractions.",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            FocusMutedText
                    )
                }

                IconButton(
                    onClick = {
                        settingsScreen = true
                    },
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(
                                FocusBlue.copy(
                                    alpha = 0.10f
                                )
                            )
                ) {

                    Text(
                        text = "⚙",
                        style =
                            MaterialTheme.typography.titleLarge,
                        color = FocusBlue
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * DURATION CARD
             */
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            FocusCard
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Focus duration",
                        style =
                            MaterialTheme.typography.titleLarge,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            FocusDarkText
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Choose any duration from 1 minute up to 30 days.",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            FocusMutedText
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        DurationField(
                            value = daysInput,
                            label = "Days",
                            onValueChange = {
                                if (
                                    it.all { char ->
                                        char.isDigit()
                                    } &&
                                    it.length <= 2
                                ) {
                                    daysInput = it
                                }
                            },
                            modifier =
                                Modifier.weight(1f)
                        )

                        DurationField(
                            value = hoursInput,
                            label = "Hours",
                            onValueChange = {
                                if (
                                    it.all { char ->
                                        char.isDigit()
                                    } &&
                                    it.length <= 2
                                ) {
                                    hoursInput = it
                                }
                            },
                            modifier =
                                Modifier.weight(1f)
                        )

                        DurationField(
                            value = minutesInput,
                            label = "Minutes",
                            onValueChange = {
                                if (
                                    it.all { char ->
                                        char.isDigit()
                                    } &&
                                    it.length <= 2
                                ) {
                                    minutesInput = it
                                }
                            },
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "Selected: $durationSummary",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            FocusBlue
                    )

                    if (!durationValid) {

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Duration must be between 1 minute and 30 days.",
                            color =
                                MaterialTheme.colorScheme.error,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * APP SECTION
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Apps to block",
                        style =
                            MaterialTheme.typography.titleLarge,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            FocusDarkText
                    )

                    Text(
                        text =
                            "${selectedApps.size} app(s) selected",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            FocusMutedText
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            /*
             * SEARCH
             */
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                label = {
                    Text("Search installed apps")
                },
                singleLine = true,
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        "${filteredApps.size} apps",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        FocusMutedText
                )

                TextButton(
                    onClick = {
                        installedApps =
                            getInstalledApps(context)
                    }
                ) {
                    Text(
                        text = "Refresh",
                        color = FocusBlue
                    )
                }
            }

            /*
             * APP LIST
             */
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
            ) {

                if (
                    selectedInstalledApps.isNotEmpty()
                ) {

                    item {

                        Text(
                            text = "Selected",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                FocusPurple,
                            modifier =
                                Modifier.padding(
                                    vertical = 8.dp
                                )
                        )
                    }

                    items(
                        items =
                            selectedInstalledApps,
                        key = {
                            "selected_${it.packageName}"
                        }
                    ) { app ->

                        AppSelectionRow(
                            app = app,
                            selected = true,
                            onCheckedChange = {

                                val newSelection =
                                    selectedApps
                                        .toMutableSet()

                                if (it) {
                                    newSelection.add(
                                        app.packageName
                                    )
                                } else {
                                    newSelection.remove(
                                        app.packageName
                                    )
                                }

                                selectedApps =
                                    newSelection

                                preferences.edit()
                                    .putStringSet(
                                        "selected_apps",
                                        newSelection
                                    )
                                    .apply()
                            }
                        )
                    }

                    item {

                        HorizontalDivider(
                            modifier =
                                Modifier.padding(
                                    vertical = 8.dp
                                )
                        )
                    }
                }

                item {

                    Text(
                        text = "All apps",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            FocusDarkText,
                        modifier =
                            Modifier.padding(
                                vertical = 8.dp
                            )
                    )
                }

                items(
                    items =
                        unselectedInstalledApps,
                    key = {
                        "all_${it.packageName}"
                    }
                ) { app ->

                    AppSelectionRow(
                        app = app,
                        selected = false,
                        onCheckedChange = {

                            val newSelection =
                                selectedApps
                                    .toMutableSet()

                            if (it) {
                                newSelection.add(
                                    app.packageName
                                )
                            } else {
                                newSelection.remove(
                                    app.packageName
                                )
                            }

                            selectedApps =
                                newSelection

                            preferences.edit()
                                .putStringSet(
                                    "selected_apps",
                                    newSelection
                                )
                                .apply()
                        }
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            /*
             * START FOCUS
             */
            Button(
                onClick = {

                    if (!durationValid) {
                        return@Button
                    }

                    val newEndTime =
                        System.currentTimeMillis() +
                                (
                                        totalDurationMinutes *
                                                60_000L
                                        )

                    preferences.edit()
                        .putLong(
                            "end_time",
                            newEndTime
                        )
                        .apply()

                    endTime =
                        newEndTime

                    secondsLeft =
                        totalDurationMinutes * 60L

                    focusStarted =
                        true

                    context.startForegroundService(
                        Intent(
                            context,
                            FocusService::class.java
                        )
                    )
                },
                enabled =
                    durationValid,
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            FocusBlue
                    ),
                shape =
                    RoundedCornerShape(16.dp)
            ) {

                Text(
                    text = "Start Focus",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

    } else {

        /*
         * ACTIVE FOCUS SESSION
         *
         * There is no Emergency Access.
         *
         * The user remains in the Focus Session
         * until the countdown reaches zero.
         */
        val focusMinutes =
            secondsLeft / 60

        val focusSeconds =
            secondsLeft % 60

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        FocusBackground
                    )
                    .padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "FOCUS LOCKED 🔒",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.Bold,
                color =
                    FocusBlue
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text = String.format(
                    "%02d:%02d",
                    focusMinutes,
                    focusSeconds
                ),
                style =
                    MaterialTheme.typography.displayLarge,
                fontWeight =
                    FontWeight.Bold,
                color =
                    FocusDarkText
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text =
                    "Your selected apps are locked until the session ends.",
                style =
                    MaterialTheme.typography.bodyLarge,
                color =
                    FocusMutedText
            )
        }
    }
}


/*
 * SETTINGS SCREEN
 */
@Composable
fun SettingsScreen(
    context: Context,
    onBack: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    FocusBackground
                )
                .padding(20.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Text(
                    text = "←",
                    style =
                        MaterialTheme.typography.titleLarge,
                    color = FocusBlue
                )
            }

            Spacer(
                modifier =
                    Modifier.width(4.dp)
            )

            Text(
                text = "Settings",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.Bold,
                color =
                    FocusDarkText
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text(
            text = "Focus Lock Setup",
            style =
                MaterialTheme.typography.titleLarge,
            fontWeight =
                FontWeight.Bold,
            color =
                FocusDarkText
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(
            text =
                "Enable the permissions Focus Lock needs to protect your sessions.",
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                FocusMutedText
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        /*
         * USAGE ACCESS
         */
        SettingsPermissionCard(
            title = "Usage Access",
            description =
                "Allows Focus Lock to monitor app usage.",
            buttonText = "Enable",
            onClick = {

                context.startActivity(
                    Intent(
                        Settings.ACTION_USAGE_ACCESS_SETTINGS
                    )
                )
            }
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        /*
         * ACCESSIBILITY
         */
        SettingsPermissionCard(
            title = "Accessibility",
            description =
                "Allows Focus Lock to detect and block protected apps.",
            buttonText =
                if (isAccessibilityEnabled(context)) {
                    "Enabled"
                } else {
                    "Enable"
                },
            onClick = {

                context.startActivity(
                    Intent(
                        Settings.ACTION_ACCESSIBILITY_SETTINGS
                    )
                )
            }
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        /*
         * DEVICE ADMIN
         */
        SettingsPermissionCard(
            title = "Device Admin",
            description =
                "Helps protect Focus Lock from being disabled during an active session.",
            buttonText = "Enable",
            onClick = {

                val deviceAdminComponent =
                    ComponentName(
                        context,
                        FocusDeviceAdminReceiver::class.java
                    )

                val intent =
                    Intent(
                        DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
                    )

                intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    deviceAdminComponent
                )

                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Focus Lock uses device administrator access to make it harder to disable the app during an active focus session."
                )

                context.startActivity(intent)
            }
        )
    }
}


/*
 * SETTINGS CARD
 */
@Composable
fun SettingsPermissionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    FocusCard
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.Bold,
                color =
                    FocusDarkText
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text = description,
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    FocusMutedText
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            OutlinedButton(
                onClick = onClick,
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = buttonText,
                    color =
                        FocusBlue,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}


/*
 * DURATION FIELD
 */
@Composable
fun DurationField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType.Number
            ),
        modifier = modifier
    )
}


/*
 * APP ROW
 */
@Composable
fun AppSelectionRow(
    app: InstalledApp,
    selected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                ),
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        FocusBlue.copy(
                            alpha = 0.08f
                        )
                    } else {
                        FocusCard
                    }
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Checkbox(
                checked = selected,
                onCheckedChange =
                    onCheckedChange
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = app.name,
                    style =
                        MaterialTheme.typography.bodyLarge,
                    fontWeight =
                        if (selected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                    color =
                        FocusDarkText
                )

                Text(
                    text = app.packageName,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        FocusMutedText
                )
            }
        }
    }
}