package com.example.nexora

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/* ============================================================
   COLORS
   ============================================================ */

private val Hazel = Color(0xFF795548)
private val MediumHazel = Color(0xFF9A765F)
private val Caramel = Color(0xFFC49A78)

private val Cream = Color(0xFFFFF9F3)
private val SoftCream = Color(0xFFF7EDE3)
private val WarmWhite = Color(0xFFFFFCF9)
private val TextBrown = Color(0xFF3E3028)

private val DarkBackground = Color(0xFF211A16)
private val DarkCard = Color(0xFF30251F)
private val DarkSidebar = Color(0xFF3A2B24)


/* ============================================================
   NOTE MODEL
   ============================================================ */

data class Note(
    val id: Long,
    var title: String,
    var content: String,
    var subject: String,
    var tags: String,
    var favorite: Boolean,
    var pinned: Boolean,
    var updated: String,

    // Multiple files
    var attachments: List<String> = emptyList()
)


/* ============================================================
   NOTE STORE
   ============================================================ */

class NoteStore(
    private val context: Context
) {

    private val prefs =
        context.getSharedPreferences(
            "nexora_notes",
            Context.MODE_PRIVATE
        )

    var isDark by mutableStateOf(
        prefs.getBoolean(
            "dark_mode",
            false
        )
    )
        private set

    var notes by mutableStateOf(
        loadNotes()
    )
        private set


    /* ========================================================
       DARK MODE
       ======================================================== */

    fun setDarkMode(value: Boolean) {

        isDark = value

        prefs.edit()
            .putBoolean(
                "dark_mode",
                value
            )
            .apply()
    }


    /* ========================================================
       LOAD NOTES
       ======================================================== */

    private fun loadNotes(): List<Note> {

        val raw =
            prefs.getString(
                "notes",
                null
            ) ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (i in 0 until array.length()) {

                    val obj =
                        array.getJSONObject(i)

                    val loadedAttachments =
                        mutableListOf<String>()

                    /*
                     * New multiple-attachment format
                     */
                    val attachmentArray =
                        obj.optJSONArray(
                            "attachments"
                        )

                    if (attachmentArray != null) {

                        for (
                        j in 0 until
                                attachmentArray.length()
                        ) {

                            val path =
                                attachmentArray
                                    .optString(j)

                            if (path.isNotBlank()) {

                                loadedAttachments
                                    .add(path)
                            }
                        }
                    }

                    /*
                     * Compatibility with old notes
                     * that had only one attachment.
                     */
                    val oldAttachment =
                        obj.optString(
                            "attachment",
                            ""
                        )

                    if (
                        oldAttachment.isNotBlank() &&
                        !loadedAttachments.contains(
                            oldAttachment
                        )
                    ) {

                        loadedAttachments.add(
                            oldAttachment
                        )
                    }

                    add(

                        Note(

                            id =
                                obj.optLong(
                                    "id"
                                ),

                            title =
                                obj.optString(
                                    "title"
                                ),

                            content =
                                obj.optString(
                                    "content"
                                ),

                            subject =
                                obj.optString(
                                    "subject"
                                ),

                            tags =
                                obj.optString(
                                    "tags"
                                ),

                            favorite =
                                obj.optBoolean(
                                    "favorite"
                                ),

                            pinned =
                                obj.optBoolean(
                                    "pinned"
                                ),

                            updated =
                                obj.optString(
                                    "updated"
                                ),

                            attachments =
                                loadedAttachments
                        )
                    )
                }
            }

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

            emptyList()
        }
    }


    /* ========================================================
       SAVE NOTES
       ======================================================== */

    private fun save() {

        val array =
            JSONArray()

        notes.forEach { note ->

            val obj =
                JSONObject()

            obj.put(
                "id",
                note.id
            )

            obj.put(
                "title",
                note.title
            )

            obj.put(
                "content",
                note.content
            )

            obj.put(
                "subject",
                note.subject
            )

            obj.put(
                "tags",
                note.tags
            )

            obj.put(
                "favorite",
                note.favorite
            )

            obj.put(
                "pinned",
                note.pinned
            )

            obj.put(
                "updated",
                note.updated
            )

            /*
             * Save multiple attachments
             */
            val attachmentArray =
                JSONArray()

            note.attachments.forEach { path ->

                attachmentArray.put(
                    path
                )
            }

            obj.put(
                "attachments",
                attachmentArray
            )

            array.put(
                obj
            )
        }

        prefs.edit()
            .putString(
                "notes",
                array.toString()
            )
            .apply()
    }


    /* ========================================================
       ADD NOTE
       ======================================================== */

    fun addNote(note: Note) {

        notes =
            listOf(note) + notes

        save()
    }


    /* ========================================================
       UPDATE NOTE
       ======================================================== */

    fun updateNote(note: Note) {

        notes =
            notes.map {

                if (it.id == note.id) {

                    note

                } else {

                    it
                }
            }

        save()
    }


    /* ========================================================
       DELETE NOTE
       ======================================================== */

    fun deleteNote(id: Long) {

        val note =
            notes.find {
                it.id == id
            }

        /*
         * Delete attached files from
         * internal storage too.
         */
        note?.attachments?.forEach { path ->

            try {

                val file =
                    File(path)

                if (file.exists()) {
                    file.delete()
                }

            } catch (
                ignored: Exception
            ) {
            }
        }

        notes =
            notes.filter {
                it.id != id
            }

        save()
    }


    /* ========================================================
       FAVORITE
       ======================================================== */

    fun toggleFavorite(id: Long) {

        notes =
            notes.map {

                if (it.id == id) {

                    it.copy(
                        favorite =
                            !it.favorite
                    )

                } else {

                    it
                }
            }

        save()
    }


    /* ========================================================
       PIN
       ======================================================== */

    fun togglePinned(id: Long) {

        notes =
            notes.map {

                if (it.id == id) {

                    it.copy(
                        pinned =
                            !it.pinned
                    )

                } else {

                    it
                }
            }

        save()
    }


    /* ========================================================
       COPY FILE TO APP STORAGE
       ======================================================== */

    fun copyFileToApp(
        uri: Uri
    ): String? {

        return try {

            val directory =
                File(
                    context.filesDir,
                    "attachments"
                )

            if (!directory.exists()) {

                directory.mkdirs()
            }


            /*
             * Get original filename.
             */

            var originalName =
                "attachment"


            val cursor: Cursor? =
                context.contentResolver.query(

                    uri,

                    arrayOf(
                        OpenableColumns
                            .DISPLAY_NAME
                    ),

                    null,
                    null,
                    null
                )


            cursor?.use {

                val nameIndex =
                    it.getColumnIndex(
                        OpenableColumns
                            .DISPLAY_NAME
                    )

                if (
                    it.moveToFirst() &&
                    nameIndex >= 0
                ) {

                    val foundName =
                        it.getString(
                            nameIndex
                        )

                    if (
                        !foundName.isNullOrBlank()
                    ) {

                        originalName =
                            foundName
                    }
                }
            }


            /*
             * Make filename safe.
             */

            originalName =
                originalName
                    .replace(
                        "/",
                        "_"
                    )
                    .replace(
                        "\\",
                        "_"
                    )
                    .replace(
                        ":",
                        "_"
                    )
                    .replace(
                        "*",
                        "_"
                    )
                    .replace(
                        "?",
                        "_"
                    )
                    .replace(
                        "\"",
                        "_"
                    )
                    .replace(
                        "<",
                        "_"
                    )
                    .replace(
                        ">",
                        "_"
                    )
                    .replace(
                        "|",
                        "_"
                    )
                    .trim()


            if (
                originalName.isBlank()
            ) {

                originalName =
                    "attachment"
            }


            /*
             * Preserve extension.
             */

            val extension =
                originalName
                    .substringAfterLast(
                        ".",
                        ""
                    )


            val baseName =

                if (
                    extension.isNotBlank()
                ) {

                    originalName.dropLast(
                        extension.length + 1
                    )

                } else {

                    originalName
                }


            /*
             * Unique filename.
             */

            val uniqueName =

                if (
                    extension.isNotBlank()
                ) {

                    "${baseName}_${System.currentTimeMillis()}.$extension"

                } else {

                    "${baseName}_${System.currentTimeMillis()}"
                }


            val file =
                File(
                    directory,
                    uniqueName
                )


            val input =
                context.contentResolver
                    .openInputStream(uri)


            if (input == null) {

                return null
            }


            input.use {

                file.outputStream()
                    .use { output ->

                        it.copyTo(
                            output
                        )
                    }
            }


            file.absolutePath

        } catch (
            e: Exception
        ) {

            e.printStackTrace()

            null
        }
    }


    /* ========================================================
       OPEN FILE
       ======================================================== */

    fun openFile(
        path: String
    ) {

        try {

            val file =
                File(path)

            if (!file.exists()) {

                return
            }


            val uri =
                FileProvider.getUriForFile(

                    context,

                    "${context.packageName}.fileprovider",

                    file
                )


            val extension =
                file.extension
                    .lowercase(
                        Locale.getDefault()
                    )


            val mimeType =
                MimeTypeMap
                    .getSingleton()
                    .getMimeTypeFromExtension(
                        extension
                    )
                    ?: "*/*"


            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        uri,
                        mimeType
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }


            context.startActivity(
                Intent.createChooser(
                    intent,
                    "Open file with"
                )
            )

        } catch (
            e: Exception
        ) {

            /*
             * Fallback for uncommon files.
             */

            try {

                val file =
                    File(path)

                if (!file.exists()) {

                    return
                }


                val uri =
                    FileProvider.getUriForFile(

                        context,

                        "${context.packageName}.fileprovider",

                        file
                    )


                val intent =
                    Intent(
                        Intent.ACTION_VIEW
                    ).apply {

                        setDataAndType(
                            uri,
                            "*/*"
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }


                context.startActivity(
                    Intent.createChooser(
                        intent,
                        "Open file with"
                    )
                )

            } catch (
                ignored: Exception
            ) {
            }
        }
    }
}


/* ============================================================
   MAIN ACTIVITY
   ============================================================ */

class MainActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        val store =
            NoteStore(this)


        val openNewNote =
            intent.getBooleanExtra(
                "OPEN_NEW_NOTE",
                false
            )


        val openFavorites =
            intent.getBooleanExtra(
                "OPEN_FAVORITES",
                false
            )


        setContent {

            NexoraTheme(
                dark =
                    store.isDark
            ) {

                NexoraApp(

                    store =
                        store,

                    openNewNote =
                        openNewNote,

                    openFavorites =
                        openFavorites
                )
            }
        }
    }
}


/* ============================================================
   THEME
   ============================================================ */

@Composable
fun NexoraTheme(

    dark: Boolean,

    content: @Composable () -> Unit
) {

    val colors =

        if (dark) {

            darkColorScheme(

                primary =
                    Color(0xFFD5A47F),

                secondary =
                    Color(0xFFC18B68),

                background =
                    DarkBackground,

                surface =
                    DarkCard,

                surfaceVariant =
                    Color(0xFF45362E),

                onBackground =
                    Color(0xFFFFF8F0),

                onSurface =
                    Color(0xFFFFF8F0),

                onSurfaceVariant =
                    Color(0xFFE5D6CA)
            )

        } else {

            lightColorScheme(

                primary =
                    Hazel,

                secondary =
                    MediumHazel,

                background =
                    Cream,

                surface =
                    WarmWhite,

                surfaceVariant =
                    SoftCream,

                onBackground =
                    TextBrown,

                onSurface =
                    TextBrown,

                onSurfaceVariant =
                    MediumHazel
            )
        }


    MaterialTheme(

        colorScheme =
            colors,

        content =
            content
    )
}


/* ============================================================
   APP SCREEN
   ============================================================ */

enum class AppScreen {

    HOME,
    NOTES,
    FAVORITES,
    SUBJECT,
    SETTINGS
}


/* ============================================================
   MAIN APP
   ============================================================ */

@Composable
fun NexoraApp(

    store: NoteStore,

    openNewNote: Boolean = false,

    openFavorites: Boolean = false
) {

    var screen by remember {

        mutableStateOf(

            if (openFavorites)
                AppScreen.FAVORITES
            else
                AppScreen.HOME
        )
    }


    var selectedSubject by remember {

        mutableStateOf<String?>(null)
    }


    var sidebarOpen by remember {

        mutableStateOf(false)
    }


    var editorOpen by remember {

        mutableStateOf(false)
    }


    var editingNote by remember {

        mutableStateOf<Note?>(null)
    }


    LaunchedEffect(
        openNewNote
    ) {

        if (openNewNote) {

            editingNote =
                null

            editorOpen =
                true
        }
    }


    Box(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme
                        .colorScheme
                        .background
                )
    ) {


        /* ====================================================
           MAIN CONTENT
           ==================================================== */

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing
                    )
        ) {

            TopBar(

                title =

                    when (screen) {

                        AppScreen.HOME ->
                            "Your Knowledge Vault"

                        AppScreen.NOTES ->
                            "All Notes"

                        AppScreen.FAVORITES ->
                            "Favorites"

                        AppScreen.SUBJECT ->
                            selectedSubject
                                ?: "Subject"

                        AppScreen.SETTINGS ->
                            "Settings"
                    },

                onMenu = {

                    sidebarOpen =
                        true
                }
            )


            AnimatedVisibility(

                visible =
                    true,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(250)
                    ) +
                            slideInHorizontally(

                                initialOffsetX = {
                                    25
                                },

                                animationSpec =
                                    tween(250)
                            ),

                exit =
                    fadeOut(
                        animationSpec =
                            tween(150)
                    ) +
                            slideOutHorizontally(

                                targetOffsetX = {
                                    -25
                                },

                                animationSpec =
                                    tween(150)
                            ),

                modifier =
                    Modifier.fillMaxSize()
            ) {

                when (screen) {

                    AppScreen.HOME -> {

                        HomeScreen(

                            store =
                                store,

                            onNewNote = {

                                editingNote =
                                    null

                                editorOpen =
                                    true
                            },

                            onOpenNote = {

                                editingNote =
                                    it

                                editorOpen =
                                    true
                            },

                            onSeeAll = {

                                screen =
                                    AppScreen.NOTES
                            }
                        )
                    }


                    AppScreen.NOTES -> {

                        NotesScreen(

                            store =
                                store,

                            onOpenNote = {

                                editingNote =
                                    it

                                editorOpen =
                                    true
                            }
                        )
                    }


                    AppScreen.FAVORITES -> {

                        FavoritesScreen(

                            store =
                                store,

                            onOpenNote = {

                                editingNote =
                                    it

                                editorOpen =
                                    true
                            }
                        )
                    }


                    AppScreen.SUBJECT -> {

                        SubjectScreen(

                            store =
                                store,

                            subject =
                                selectedSubject
                                    ?: "",

                            onOpenNote = {

                                editingNote =
                                    it

                                editorOpen =
                                    true
                            }
                        )
                    }


                    AppScreen.SETTINGS -> {

                        SettingsScreen(
                            store =
                                store
                        )
                    }
                }
            }
        }


        /* ====================================================
           SIDEBAR SCRIM
           ==================================================== */

        AnimatedVisibility(

            visible =
                sidebarOpen,

            enter =
                fadeIn(
                    tween(250)
                ),

            exit =
                fadeOut(
                    tween(200)
                ),

            modifier =
                Modifier.fillMaxSize()
        ) {

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(

                            Color.Black.copy(

                                alpha =

                                    if (
                                        store.isDark
                                    )
                                        0.50f
                                    else
                                        0.28f
                            )
                        )
                        .clickable {

                            sidebarOpen =
                                false
                        }
            )
        }


        /* ====================================================
           SIDEBAR
           ==================================================== */

        AnimatedVisibility(

            visible =
                sidebarOpen,

            enter =

                slideInHorizontally(

                    initialOffsetX = {
                        -it
                    },

                    animationSpec =
                        tween(

                            400,

                            easing =
                                FastOutSlowInEasing
                        )
                ) +
                        fadeIn(
                            tween(250)
                        ),

            exit =

                slideOutHorizontally(

                    targetOffsetX = {
                        -it
                    },

                    animationSpec =
                        tween(

                            350,

                            easing =
                                FastOutSlowInEasing
                        )
                ) +
                        fadeOut(
                            tween(200)
                        ),

            modifier =
                Modifier.align(
                    Alignment.CenterStart
                )
        ) {

            Sidebar(

                store =
                    store,

                currentScreen =
                    screen,

                selectedSubject =
                    selectedSubject,

                onHome = {

                    screen =
                        AppScreen.HOME

                    selectedSubject =
                        null

                    sidebarOpen =
                        false
                },

                onNotes = {

                    screen =
                        AppScreen.NOTES

                    selectedSubject =
                        null

                    sidebarOpen =
                        false
                },

                onFavorites = {

                    screen =
                        AppScreen.FAVORITES

                    selectedSubject =
                        null

                    sidebarOpen =
                        false
                },

                onSubject = { subject ->

                    selectedSubject =
                        subject

                    screen =
                        AppScreen.SUBJECT

                    sidebarOpen =
                        false
                },

                onSettings = {

                    screen =
                        AppScreen.SETTINGS

                    selectedSubject =
                        null

                    sidebarOpen =
                        false
                },

                onNewNote = {

                    editingNote =
                        null

                    editorOpen =
                        true

                    sidebarOpen =
                        false
                },

                onClose = {

                    sidebarOpen =
                        false
                }
            )
        }


        /* ====================================================
           FAB
           ==================================================== */

        FloatingActionButton(

            onClick = {

                editingNote =
                    null

                editorOpen =
                    true
            },

            modifier =
                Modifier
                    .align(
                        Alignment.BottomEnd
                    )
                    .padding(
                        end = 22.dp,
                        bottom = 22.dp
                    )
                    .navigationBarsPadding(),

            containerColor =

                if (store.isDark)
                    Color(0xFFC18B68)
                else
                    Hazel,

            contentColor =
                Color.White

        ) {

            Icon(

                Icons.Default.Add,

                contentDescription =
                    "New Lesson"
            )
        }
    }


    /* ========================================================
       EDITOR
       ======================================================== */

    if (editorOpen) {

        Editor(

            store =
                store,

            note =
                editingNote,

            onClose = {

                editorOpen =
                    false
            }
        )
    }
}


/* ============================================================
   SIDEBAR
   ============================================================ */

@Composable
fun Sidebar(

    store: NoteStore,

    currentScreen: AppScreen,

    selectedSubject: String?,

    onHome: () -> Unit,

    onNotes: () -> Unit,

    onFavorites: () -> Unit,

    onSubject: (String) -> Unit,

    onSettings: () -> Unit,

    onNewNote: () -> Unit,

    onClose: () -> Unit
) {

    val subjects =
        store.notes
            .map {
                it.subject
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .sorted()


    val sidebarColor =

        if (store.isDark)
            DarkSidebar
        else
            Color(0xFF6F5040)


    Column(

        modifier =
            Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(
                    sidebarColor
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                )
    ) {


        Row(

            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Box(

                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .background(
                            Color.White.copy(
                                alpha = .15f
                            )
                        ),

                contentAlignment =
                    Alignment.Center

            ) {

                Text(

                    text =
                        "N",

                    color =
                        Color.White,

                    fontSize =
                        25.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                Modifier.width(12.dp)
            )


            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        "NEXORA",

                    color =
                        Color.White,

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(

                    text =
                        "KNOWLEDGE VAULT",

                    color =
                        Color.White.copy(
                            alpha = .65f
                        ),

                    fontSize =
                        9.sp,

                    letterSpacing =
                        1.5.sp
                )
            }


            IconButton(
                onClick =
                    onClose
            ) {

                Icon(

                    Icons.Default.Close,

                    contentDescription =
                        "Close",

                    tint =
                        Color.White.copy(
                            alpha = .85f
                        )
                )
            }
        }


        Spacer(
            Modifier.height(24.dp)
        )


        SidebarButton(

            icon =
                Icons.Default.Add,

            text =
                "New Lesson",

            selected =
                false,

            primary =
                true,

            onClick =
                onNewNote
        )


        Spacer(
            Modifier.height(20.dp)
        )


        SidebarSectionTitle(
            "WORKSPACE"
        )


        Spacer(
            Modifier.height(7.dp)
        )


        SidebarButton(

            icon =
                Icons.Default.Home,

            text =
                "Home",

            selected =
                currentScreen ==
                        AppScreen.HOME,

            onClick =
                onHome
        )


        SidebarButton(

            icon =
                Icons.Default.Note,

            text =
                "All Lessons",

            selected =
                currentScreen ==
                        AppScreen.NOTES,

            onClick =
                onNotes
        )


        SidebarButton(

            icon =
                Icons.Default.Favorite,

            text =
                "Favorites",

            selected =
                currentScreen ==
                        AppScreen.FAVORITES,

            onClick =
                onFavorites
        )


        Spacer(
            Modifier.height(18.dp)
        )


        SidebarSectionTitle(
            "SUBJECTS"
        )


        Spacer(
            Modifier.height(7.dp)
        )


        LazyColumn(
            modifier =
                Modifier.weight(1f)
        ) {

            items(subjects) { subject ->

                SidebarButton(

                    icon =
                        Icons.Default.Subject,

                    text =
                        subject,

                    selected =

                        currentScreen ==
                                AppScreen.SUBJECT &&
                                selectedSubject ==
                                subject,

                    onClick = {

                        onSubject(
                            subject
                        )
                    }
                )
            }
        }


        Divider(

            color =
                Color.White.copy(
                    alpha = .15f
                )
        )


        Spacer(
            Modifier.height(8.dp)
        )


        SidebarButton(

            icon =
                Icons.Default.Settings,

            text =
                "Settings",

            selected =
                currentScreen ==
                        AppScreen.SETTINGS,

            onClick =
                onSettings
        )


        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Icon(

                if (store.isDark)
                    Icons.Default.DarkMode
                else
                    Icons.Default.WbSunny,

                contentDescription =
                    null,

                tint =
                    Color.White.copy(
                        alpha = .85f
                    ),

                modifier =
                    Modifier.size(18.dp)
            )


            Spacer(
                Modifier.width(10.dp)
            )


            Text(

                text =
                    "Dark Mode",

                color =
                    Color.White.copy(
                        alpha = .9f
                    ),

                fontSize =
                    13.sp,

                modifier =
                    Modifier.weight(1f)
            )


            Switch(

                checked =
                    store.isDark,

                onCheckedChange =
                    store::setDarkMode
            )
        }
    }
}


/* ============================================================
   SIDEBAR SECTION TITLE
   ============================================================ */

@Composable
fun SidebarSectionTitle(
    text: String
) {

    Text(

        text =
            text,

        color =
            Color.White.copy(
                alpha = .52f
            ),

        fontSize =
            10.sp,

        fontWeight =
            FontWeight.Bold,

        letterSpacing =
            1.4.sp,

        modifier =
            Modifier.padding(
                horizontal = 12.dp
            )
    )
}


/* ============================================================
   SIDEBAR BUTTON
   ============================================================ */

@Composable
fun SidebarButton(

    icon: ImageVector,

    text: String,

    selected: Boolean,

    primary: Boolean = false,

    onClick: () -> Unit
) {

    val backgroundColor =

        when {

            primary ->
                Caramel

            selected ->
                Color.White.copy(
                    alpha = .16f
                )

            else ->
                Color.Transparent
        }


    Box(

        modifier =
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(
                    vertical = 3.dp
                )
                .clip(
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .background(
                    backgroundColor
                )
                .clickable(
                    onClick =
                        onClick
                )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 12.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            if (selected) {

                Box(

                    modifier =
                        Modifier
                            .width(4.dp)
                            .height(22.dp)
                            .clip(
                                RoundedCornerShape(
                                    4.dp
                                )
                            )
                            .background(
                                Color.White
                            )
                )


                Spacer(
                    Modifier.width(10.dp)
                )
            }


            Icon(

                icon,

                contentDescription =
                    null,

                tint =
                    Color.White.copy(

                        alpha =

                            if (
                                selected ||
                                primary
                            )
                                1f
                            else
                                .78f
                    ),

                modifier =
                    Modifier.size(19.dp)
            )


            Spacer(
                Modifier.width(12.dp)
            )


            Text(

                text =
                    text,

                color =
                    Color.White,

                fontSize =
                    13.sp,

                fontWeight =

                    if (
                        selected ||
                        primary
                    )
                        FontWeight.Bold
                    else
                        FontWeight.Normal,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}


/* ============================================================
   TOP BAR
   ============================================================ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(

    title: String,

    onMenu: () -> Unit
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 10.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        IconButton(
            onClick =
                onMenu
        ) {

            Icon(

                Icons.Default.Menu,

                contentDescription =
                    "Open menu",

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }


        Spacer(
            Modifier.width(6.dp)
        )


        Text(

            text =
                title,

            fontSize =
                22.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .onBackground,

            modifier =
                Modifier.weight(1f)
        )


        Box(

            modifier =
                Modifier
                    .size(42.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    ),

            contentAlignment =
                Alignment.Center

        ) {

            Icon(

                Icons.Default.Search,

                contentDescription =
                    "Search",

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}


/* ============================================================
   HOME
   ============================================================ */

@Composable
fun HomeScreen(

    store: NoteStore,

    onNewNote: () -> Unit,

    onOpenNote: (Note) -> Unit,

    onSeeAll: () -> Unit
) {

    val recent =
        store.notes.take(6)


    LazyColumn(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                15.dp
            )
    ) {

        item {

            Spacer(
                Modifier.height(5.dp)
            )


            Text(

                text =
                    "Welcome to NEXORA",

                fontSize =
                    14.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .secondary
            )


            Text(

                text =
                    "Your knowledge,\norganized beautifully.",

                fontSize =
                    30.sp,

                lineHeight =
                    36.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onBackground
            )
        }


        item {

            StatsRow(
                store =
                    store
            )
        }


        item {

            QuickActions(
                onNewNote =
                    onNewNote
            )
        }


        item {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(

                    text =
                        "Recent Lessons",

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onBackground,

                    modifier =
                        Modifier.weight(1f)
                )


                TextButton(
                    onClick =
                        onSeeAll
                ) {

                    Text(

                        "See all",

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }
        }


        if (recent.isEmpty()) {

            item {

                EmptyNotes(
                    onNewNote =
                        onNewNote
                )
            }

        } else {

            itemsIndexed(

                items =
                    recent,

                key = {
                        _, note ->
                    note.id
                }

            ) { index, note ->

                AnimatedNoteCard(

                    note =
                        note,

                    index =
                        index,

                    store =
                        store,

                    onClick = {

                        onOpenNote(
                            note
                        )
                    }
                )
            }
        }


        item {

            Spacer(
                Modifier.height(80.dp)
            )
        }
    }
}


/* ============================================================
   STATS
   ============================================================ */

@Composable
fun StatsRow(
    store: NoteStore
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        StatCard(

            modifier =
                Modifier.weight(1f),

            icon =
                Icons.Default.Note,

            number =
                store.notes.size.toString(),

            label =
                "Lessons"
        )


        StatCard(

            modifier =
                Modifier.weight(1f),

            icon =
                Icons.Default.Favorite,

            number =
                store.notes
                    .count {
                        it.favorite
                    }
                    .toString(),

            label =
                "Favorites"
        )


        StatCard(

            modifier =
                Modifier.weight(1f),

            icon =
                Icons.Default.Folder,

            number =
                store.notes
                    .map {
                        it.subject
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()
                    .size
                    .toString(),

            label =
                "Subjects"
        )
    }
}


/* ============================================================
   STAT CARD
   ============================================================ */

@Composable
fun StatCard(

    modifier: Modifier,

    icon: ImageVector,

    number: String,

    label: String
) {

    var visible by remember {

        mutableStateOf(false)
    }


    LaunchedEffect(Unit) {

        visible =
            true
    }


    val scale by
    animateFloatAsState(

        targetValue =

            if (visible)
                1f
            else
                0.85f,

        animationSpec =
            tween(450),

        label =
            "statScale"
    )


    Card(

        modifier =
            modifier.scale(
                scale
            ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),

        elevation =
            CardDefaults
                .cardElevation(
                    2.dp
                )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    15.dp
                )
        ) {

            Icon(

                icon,

                contentDescription =
                    null,

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,

                modifier =
                    Modifier.size(
                        22.dp
                    )
            )


            Spacer(
                Modifier.height(8.dp)
            )


            Text(

                text =
                    number,

                fontSize =
                    23.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )


            Text(

                text =
                    label,

                fontSize =
                    11.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


/* ============================================================
   QUICK ACTIONS
   ============================================================ */

@Composable
fun QuickActions(
    onNewNote: () -> Unit
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Box(

                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary
                        ),

                contentAlignment =
                    Alignment.Center

            ) {

                Icon(

                    Icons.Default.Add,

                    contentDescription =
                        null,

                    tint =
                        Color.White
                )
            }


            Spacer(
                Modifier.width(14.dp)
            )


            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        "Capture a new lesson",

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )


                Text(

                    text =
                        "Save ideas, lessons and files.",

                    fontSize =
                        12.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


            TextButton(
                onClick =
                    onNewNote
            ) {

                Text(

                    text =
                        "Create",

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


/* ============================================================
   NOTES SCREEN
   ============================================================ */

@Composable
fun NotesScreen(

    store: NoteStore,

    onOpenNote: (Note) -> Unit
) {

    var search by remember {

        mutableStateOf("")
    }


    val filtered =
        store.notes.filter {

            it.title.contains(
                search,
                true
            ) ||

                    it.content.contains(
                        search,
                        true
                    ) ||

                    it.subject.contains(
                        search,
                        true
                    ) ||

                    it.tags.contains(
                        search,
                        true
                    )
        }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp
                )
    ) {

        OutlinedTextField(

            value =
                search,

            onValueChange = {
                search = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            singleLine =
                true,

            leadingIcon = {

                Icon(

                    Icons.Default.Search,

                    contentDescription =
                        null
                )
            },

            placeholder = {

                Text(
                    "Search your lessons..."
                )
            },

            shape =
                RoundedCornerShape(
                    16.dp
                )
        )


        Spacer(
            Modifier.height(15.dp)
        )


        LazyColumn(

            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            items(

                items =
                    filtered,

                key = {
                    it.id
                }

            ) { note ->

                AnimatedNoteCard(

                    note =
                        note,

                    index =
                        0,

                    store =
                        store,

                    onClick = {

                        onOpenNote(
                            note
                        )
                    }
                )
            }


            item {

                Spacer(
                    Modifier.height(70.dp)
                )
            }
        }
    }
}


/* ============================================================
   FAVORITES
   ============================================================ */

@Composable
fun FavoritesScreen(

    store: NoteStore,

    onOpenNote: (Note) -> Unit
) {

    val favorites =
        store.notes.filter {
            it.favorite
        }


    LazyColumn(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        item {

            Text(

                text =
                    "Your saved favorites",

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                fontSize =
                    14.sp
            )
        }


        if (favorites.isEmpty()) {

            item {

                Empty(

                    title =
                        "No favorites yet",

                    subtitle =
                        "Favorite important lessons to find them here."
                )
            }

        } else {

            items(

                items =
                    favorites,

                key = {
                    it.id
                }

            ) { note ->

                AnimatedNoteCard(

                    note =
                        note,

                    index =
                        0,

                    store =
                        store,

                    onClick = {

                        onOpenNote(
                            note
                        )
                    }
                )
            }
        }
    }
}


/* ============================================================
   SUBJECT
   ============================================================ */

@Composable
fun SubjectScreen(

    store: NoteStore,

    subject: String,

    onOpenNote: (Note) -> Unit
) {

    val notes =
        store.notes.filter {
            it.subject == subject
        }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp
                )
    ) {

        Text(

            text =
                subject,

            fontSize =
                26.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
        )


        Text(

            text =
                "${notes.size} lesson${if (notes.size == 1) "" else "s"}",

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,

            fontSize =
                13.sp
        )


        Spacer(
            Modifier.height(16.dp)
        )


        if (notes.isEmpty()) {

            Empty(

                title =
                    "No lessons yet",

                subtitle =
                    "Create a new lesson for this subject."
            )

        } else {

            LazyColumn(

                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                items(

                    items =
                        notes,

                    key = {
                        it.id
                    }

                ) { note ->

                    AnimatedNoteCard(

                        note =
                            note,

                        index =
                            0,

                        store =
                            store,

                        onClick = {

                            onOpenNote(
                                note
                            )
                        }
                    )
                }


                item {

                    Spacer(
                        Modifier.height(
                            80.dp
                        )
                    )
                }
            }
        }
    }
}


/* ============================================================
   ANIMATED NOTE CARD
   ============================================================ */

@Composable
fun AnimatedNoteCard(

    note: Note,

    index: Int,

    store: NoteStore,

    onClick: () -> Unit
) {

    var visible by remember {

        mutableStateOf(false)
    }


    LaunchedEffect(Unit) {

        delay(
            minOf(
                index * 70L,
                350L
            )
        )

        visible =
            true
    }


    AnimatedVisibility(

        visible =
            visible,

        enter =
            fadeIn(
                tween(400)
            ) +
                    slideInHorizontally(

                        initialOffsetX = {
                            60
                        },

                        animationSpec =
                            tween(
                                400,
                                easing =
                                    FastOutSlowInEasing
                            )
                    )
    ) {

        NoteCard(

            note =
                note,

            store =
                store,

            onClick =
                onClick
        )
    }
}


/* ============================================================
   NOTE CARD
   ============================================================ */

@Composable
fun NoteCard(

    note: Note,

    store: NoteStore,

    onClick: () -> Unit
) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),

        elevation =
            CardDefaults
                .cardElevation(
                    2.dp
                )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {


            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Box(

                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                            ),

                    contentAlignment =
                        Alignment.Center

                ) {

                    Icon(

                        Icons.Default.Description,

                        contentDescription =
                            null,

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }


                Spacer(
                    Modifier.width(12.dp)
                )


                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            note.title.ifBlank {
                                "Untitled Lesson"
                            },

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            16.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )


                    Text(

                        text =
                            note.subject.ifBlank {
                                "General"
                            },

                        fontSize =
                            11.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                IconButton(

                    onClick = {

                        store.toggleFavorite(
                            note.id
                        )
                    }

                ) {

                    Icon(

                        if (note.favorite)
                            Icons.Default.Favorite
                        else
                            Icons.Default.FavoriteBorder,

                        contentDescription =
                            "Favorite",

                        tint =

                            if (note.favorite)

                                MaterialTheme
                                    .colorScheme
                                    .primary

                            else

                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                    )
                }
            }


            Spacer(
                Modifier.height(10.dp)
            )


            Text(

                text =
                    note.content,

                maxLines =
                    3,

                overflow =
                    TextOverflow.Ellipsis,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
                        .copy(
                            alpha = .72f
                        ),

                fontSize =
                    13.sp,

                lineHeight =
                    19.sp
            )


            /* =================================================
               MULTIPLE ATTACHMENTS
               ================================================= */

            if (
                note.attachments.isNotEmpty()
            ) {

                Spacer(
                    Modifier.height(10.dp)
                )


                note.attachments.forEach { path ->

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                )
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                )
                                .clickable {

                                    store.openFile(
                                        path
                                    )
                                }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {

                        Icon(

                            Icons.Default.AttachFile,

                            contentDescription =
                                "Open attachment",

                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary,

                            modifier =
                                Modifier.size(
                                    20.dp
                                )
                        )


                        Spacer(
                            Modifier.width(8.dp)
                        )


                        Text(

                            text =
                                File(path).name,

                            modifier =
                                Modifier.weight(1f),

                            fontSize =
                                11.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,

                            maxLines =
                                1,

                            overflow =
                                TextOverflow.Ellipsis
                        )


                        Text(

                            text =
                                "OPEN",

                            fontSize =
                                10.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                    }


                    Spacer(
                        Modifier.height(5.dp)
                    )
                }
            }


            Spacer(
                Modifier.height(10.dp)
            )


            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(

                    text =
                        note.updated,

                    fontSize =
                        10.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    modifier =
                        Modifier.weight(1f)
                )


                if (note.pinned) {

                    Icon(

                        Icons.Default.Bookmark,

                        contentDescription =
                            "Pinned",

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,

                        modifier =
                            Modifier.size(
                                17.dp
                            )
                    )
                }
            }
        }
    }
}


/* ============================================================
   EMPTY
   ============================================================ */

@Composable
fun Empty(

    title: String,

    subtitle: String
) {

    Box(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 60.dp
                ),

        contentAlignment =
            Alignment.Center

    ) {

        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally

        ) {

            Box(

                modifier =
                    Modifier
                        .size(70.dp)
                        .clip(
                            CircleShape
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                        ),

                contentAlignment =
                    Alignment.Center

            ) {

                Icon(

                    Icons.Default.Note,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    modifier =
                        Modifier.size(
                            32.dp
                        )
                )
            }


            Spacer(
                Modifier.height(14.dp)
            )


            Text(

                text =
                    title,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    18.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onBackground
            )


            Spacer(
                Modifier.height(5.dp)
            )


            Text(

                text =
                    subtitle,

                fontSize =
                    12.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


/* ============================================================
   EMPTY NOTES
   ============================================================ */

@Composable
fun EmptyNotes(
    onNewNote: () -> Unit
) {

    Column(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Empty(

            title =
                "Your vault is empty",

            subtitle =
                "Create your first lesson to get started."
        )


        TextButton(
            onClick =
                onNewNote
        ) {

            Text(

                text =
                    "Create First Lesson",

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}


/* ============================================================
   SETTINGS
   ============================================================ */

@Composable
fun SettingsScreen(
    store: NoteStore
) {

    LazyColumn(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp
                )
    ) {

        item {

            SettingsCard(

                title =
                    "Appearance",

                subtitle =
                    "Customize how Nexora looks."

            ) {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Icon(

                        if (store.isDark)
                            Icons.Default.DarkMode
                        else
                            Icons.Default.WbSunny,

                        contentDescription =
                            null,

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )


                    Spacer(
                        Modifier.width(12.dp)
                    )


                    Column(

                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(

                            text =
                                "Dark Mode",

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )


                        Text(

                            text =
                                "Switch between light and dark mode.",

                            fontSize =
                                11.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }


                    Switch(

                        checked =
                            store.isDark,

                        onCheckedChange =
                            store::setDarkMode
                    )
                }
            }
        }


        item {

            SettingsCard(

                title =
                    "Storage",

                subtitle =
                    "Your lessons are stored locally."

            ) {

                Text(

                    text =
                        "${store.notes.size} lessons saved",

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )
            }
        }


        item {

            SettingsCard(

                title =
                    "About Nexora",

                subtitle =
                    "Your Personal Knowledge Hub"

            ) {

                Text(

                    text =
                        "NEXORA",

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )


                Text(

                    text =
                        "YOUR KNOWLEDGE VAULT",

                    fontSize =
                        9.sp,

                    letterSpacing =
                        1.5.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}


/* ============================================================
   SETTINGS CARD
   ============================================================ */

@Composable
fun SettingsCard(

    title: String,

    subtitle: String,

    content: @Composable () -> Unit
) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 14.dp
                ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(

                text =
                    title,

                fontSize =
                    17.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )


            Text(

                text =
                    subtitle,

                fontSize =
                    11.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


            Spacer(
                Modifier.height(18.dp)
            )


            content()
        }
    }
}


/* ============================================================
   EDITOR
   ============================================================ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Editor(

    store: NoteStore,

    note: Note?,

    onClose: () -> Unit
) {

    var title by remember {

        mutableStateOf(
            note?.title ?: ""
        )
    }


    var content by remember {

        mutableStateOf(
            note?.content ?: ""
        )
    }


    var subject by remember {

        mutableStateOf(
            note?.subject ?: ""
        )
    }


    var tags by remember {

        mutableStateOf(
            note?.tags ?: ""
        )
    }


    var favorite by remember {

        mutableStateOf(
            note?.favorite ?: false
        )
    }


    var pinned by remember {

        mutableStateOf(
            note?.pinned ?: false
        )
    }


    /*
     * MULTIPLE ATTACHMENTS
     */

    var attachments by remember {

        mutableStateOf(
            note?.attachments
                ?: emptyList()
        )
    }


    /* ========================================================
       MULTIPLE FILE PICKER
       ======================================================== */

    val picker =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts
                    .OpenMultipleDocuments()

        ) { uris ->

            if (uris.isNotEmpty()) {

                val newFiles =
                    uris.mapNotNull { uri ->

                        store.copyFileToApp(
                            uri
                        )
                    }


                if (newFiles.isNotEmpty()) {

                    attachments =
                        attachments + newFiles
                }
            }
        }


    Surface(

        modifier =
            Modifier.fillMaxSize(),

        color =
            MaterialTheme
                .colorScheme
                .background
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
        ) {


            /* =================================================
               EDITOR TOP BAR
               ================================================= */

            TopAppBar(

                title = {

                    Text(

                        text =

                            if (note == null)
                                "New Lesson"
                            else
                                "Edit Lesson"
                    )
                },


                navigationIcon = {

                    IconButton(
                        onClick =
                            onClose
                    ) {

                        Icon(

                            Icons.Default.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },


                actions = {

                    IconButton(

                        onClick = {

                            favorite =
                                !favorite
                        }

                    ) {

                        Icon(

                            if (favorite)
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,

                            contentDescription =
                                "Favorite"
                        )
                    }


                    IconButton(

                        onClick = {

                            pinned =
                                !pinned
                        }

                    ) {

                        Icon(

                            if (pinned)
                                Icons.Default.Bookmark
                            else
                                Icons.Default.BookmarkBorder,

                            contentDescription =
                                "Pin"
                        )
                    }


                    TextButton(

                        onClick = {

                            val date =
                                SimpleDateFormat(

                                    "dd MMM yyyy, HH:mm",

                                    Locale.getDefault()

                                ).format(
                                    Date()
                                )


                            val newNote =
                                Note(

                                    id =
                                        note?.id
                                            ?: System.currentTimeMillis(),

                                    title =
                                        title.trim(),

                                    content =
                                        content,

                                    subject =
                                        subject.trim(),

                                    tags =
                                        tags.trim(),

                                    favorite =
                                        favorite,

                                    pinned =
                                        pinned,

                                    updated =
                                        date,

                                    attachments =
                                        attachments
                                )


                            if (note == null) {

                                store.addNote(
                                    newNote
                                )

                            } else {

                                store.updateNote(
                                    newNote
                                )
                            }


                            onClose()
                        }

                    ) {

                        Text(

                            text =
                                "Save",

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                },


                colors =

                    TopAppBarDefaults
                        .topAppBarColors(

                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .background,

                            titleContentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onBackground,

                            navigationIconContentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onBackground,

                            actionIconContentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onBackground
                        )
            )


            LazyColumn(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 20.dp
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    )
            ) {


                /* =================================================
                   LESSON TITLE
                   ================================================= */

                item {

                    OutlinedTextField(

                        value =
                            title,

                        onValueChange = {
                            title = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        singleLine =
                            true,

                        label = {

                            Text(
                                "Lesson Title"
                            )
                        },

                        placeholder = {

                            Text(
                                "Example: Introduction to Machine Learning"
                            )
                        },

                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    )
                }


                /* =================================================
                   SUBJECT
                   ================================================= */

                item {

                    OutlinedTextField(

                        value =
                            subject,

                        onValueChange = {
                            subject = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        singleLine =
                            true,

                        label = {

                            Text(
                                "Subject"
                            )
                        },

                        placeholder = {

                            Text(
                                "Example: Machine Learning"
                            )
                        },

                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    )
                }


                /* =================================================
                   TAGS
                   ================================================= */

                item {

                    OutlinedTextField(

                        value =
                            tags,

                        onValueChange = {
                            tags = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        singleLine =
                            true,

                        label = {

                            Text(
                                "Tags"
                            )
                        },

                        placeholder = {

                            Text(
                                "Example: AI, ML, Classification"
                            )
                        },

                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    )
                }


                /* =================================================
                   MULTI-LINE LESSON
                   ================================================= */

                item {

                    OutlinedTextField(

                        value =
                            content,

                        onValueChange = {
                            content = it
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    320.dp
                                ),

                        label = {

                            Text(
                                "Lesson Description"
                            )
                        },

                        placeholder = {

                            Text(
                                "Write your lesson here...\n\nYou can write multiple lines, explanations, examples, formulas, important points, etc."
                            )
                        },

                        singleLine =
                            false,

                        minLines =
                            12,

                        maxLines =
                            20,

                        shape =
                            RoundedCornerShape(
                                18.dp
                            )
                    )
                }


                /* =================================================
                   MULTIPLE ATTACHMENTS
                   ================================================= */

                item {

                    Column {

                        /*
                         * ADD FILE BUTTON
                         */

                        Card(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {

                                        picker.launch(
                                            arrayOf(
                                                "*/*"
                                            )
                                        )
                                    },

                            shape =
                                RoundedCornerShape(
                                    18.dp
                                ),

                            colors =
                                CardDefaults
                                    .cardColors(

                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                    )
                        ) {

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            18.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically

                            ) {

                                Icon(

                                    Icons.Default.AttachFile,

                                    contentDescription =
                                        null,

                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,

                                    modifier =
                                        Modifier.size(
                                            24.dp
                                        )
                                )


                                Spacer(
                                    Modifier.width(12.dp)
                                )


                                Column(

                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                ) {

                                    Text(

                                        text =
                                            "Add Attachments",

                                        fontWeight =
                                            FontWeight.Bold,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurface
                                    )


                                    Text(

                                        text =
                                            if (
                                                attachments.isEmpty()
                                            ) {

                                                "Select one or multiple files"

                                            } else {

                                                "${attachments.size} file${if (attachments.size == 1) "" else "s"} attached"
                                            },

                                        fontSize =
                                            11.sp,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )


                                    Text(

                                        text =
                                            "PDF, Word, Excel, PowerPoint, images and more",

                                        fontSize =
                                            10.sp,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }


                                Icon(

                                    Icons.Default.Add,

                                    contentDescription =
                                        "Add files",

                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )
                            }
                        }


                        /*
                         * LIST OF ATTACHMENTS
                         */

                        if (
                            attachments.isNotEmpty()
                        ) {

                            Spacer(
                                Modifier.height(
                                    10.dp
                                )
                            )


                            attachments.forEachIndexed {

                                    index,
                                    path ->

                                Row(

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                vertical = 4.dp
                                            )
                                            .clip(
                                                RoundedCornerShape(
                                                    12.dp
                                                )
                                            )
                                            .background(
                                                MaterialTheme
                                                    .colorScheme
                                                    .surfaceVariant
                                            )
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 10.dp
                                            ),

                                    verticalAlignment =
                                        Alignment.CenterVertically

                                ) {

                                    Icon(

                                        Icons.Default.AttachFile,

                                        contentDescription =
                                            null,

                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        modifier =
                                            Modifier.size(
                                                20.dp
                                            )
                                    )


                                    Spacer(
                                        Modifier.width(8.dp)
                                    )


                                    Text(

                                        text =
                                            File(
                                                path
                                            ).name,

                                        modifier =
                                            Modifier
                                                .weight(
                                                    1f
                                                )
                                                .clickable {

                                                    store.openFile(
                                                        path
                                                    )
                                                },

                                        fontSize =
                                            12.sp,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurface,

                                        maxLines =
                                            1,

                                        overflow =
                                            TextOverflow.Ellipsis
                                    )


                                    TextButton(

                                        onClick = {

                                            /*
                                             * Delete the actual
                                             * copied file too.
                                             */

                                            try {

                                                val file =
                                                    File(
                                                        path
                                                    )

                                                if (
                                                    file.exists()
                                                ) {

                                                    file.delete()
                                                }

                                            } catch (
                                                ignored: Exception
                                            ) {
                                            }


                                            attachments =
                                                attachments
                                                    .filterIndexed {

                                                            i,
                                                            _ ->

                                                        i != index
                                                    }
                                        }

                                    ) {

                                        Text(

                                            text =
                                                "REMOVE",

                                            fontSize =
                                                10.sp,

                                            color =
                                                Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                /* =================================================
                   DELETE LESSON
                   ================================================= */

                if (note != null) {

                    item {

                        TextButton(

                            onClick = {

                                store.deleteNote(
                                    note.id
                                )

                                onClose()
                            }

                        ) {

                            Icon(

                                Icons.Default.Delete,

                                contentDescription =
                                    null,

                                tint =
                                    Color.Red
                            )


                            Spacer(
                                Modifier.width(6.dp)
                            )


                            Text(

                                text =
                                    "Delete lesson",

                                color =
                                    Color.Red
                            )
                        }
                    }
                }


                item {

                    Spacer(
                        Modifier.height(
                            60.dp
                        )
                    )
                }
            }
        }
    }
}