package org.python.companion

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.python.backend.data.datatype.Anniversary
import org.python.backend.data.datatype.Note
import org.python.backend.security.SecurityActor
import org.python.backend.security.SecurityProvider
import org.python.backend.security.VerificationMessage
import org.python.backend.security.VerificationToken
import org.python.companion.ui.anniversary.AnniversaryBody
import org.python.companion.ui.cactus.CactusBody
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.components.CompanionTabRow
import org.python.companion.ui.note.*
import org.python.companion.ui.splash.SplashBuilder
import org.python.companion.ui.theme.CompanionTheme
import org.python.companion.viewmodels.AnniversaryViewModel
import org.python.companion.viewmodels.NoteViewModel
import timber.log.Timber


class MainActivity : FragmentActivity() {
    private val noteViewModel by viewModels<NoteViewModel>()
    private val anniversaryViewModel by viewModels<AnniversaryViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompanionTheme {
                val allScreens = CompanionScreen.values().toList()
                val navController = rememberNavController()
                val backstackEntry = navController.currentBackStackEntryAsState()
                val selectedTabScreen = CompanionScreen.fromRoute(backstackEntry.value?.destination?.route)

                val noteState = NoteState.rememberState(
                    navController = navController,
                    authState = AuthenticationMiniState.rememberState(this),
                    noteViewModel = noteViewModel)
                val cactusState = CactusState.rememberState(navController = navController)
                val anniversaryState = AnniversaryState.rememberState(navController = navController, anniversaryViewModel = anniversaryViewModel)

                Scaffold(
                    topBar = {
                        CompanionTabRow(
                            allScreens = allScreens,
                            onTabSelected = { screen ->
                                    Timber.w("Got tab selected: ${screen.name}")
                                     navController.navigate(screen.name) },
                            currentScreen = selectedTabScreen
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash_screen",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash_screen") {
                            val splashScreenFunc = remember {
                                SplashBuilder(navController = navController, destination = CompanionScreen.Cactus.name).build {
                                    noteState.load()
                                    anniversaryState.load()
                                }
                            }
                            splashScreenFunc()
                        }
                        with(cactusState) { cactusGraph() }
                        with(noteState) { noteGraph() }
                        with(anniversaryState) { anniversaryGraph() }
                    }
                }
            }
        }
    }
}

class AuthenticationMiniState(var securityActor: SecurityActor) {
    var authenticated: Boolean = false
        private set
    var token: VerificationToken? = null
        private set

    constructor(activity: FragmentActivity) : this(
        securityActor = SecurityProvider.getActor(
            type = activity
                .baseContext
                .getSharedPreferences(SecurityActor.security_storage, Context.MODE_PRIVATE)
                .getInt(SecurityActor.preferred_actor_key, SecurityActor.preferred_actor_default),
            activity = activity)
    )

    suspend fun authenticate(securityToken: VerificationToken? = null): VerificationMessage {
        val msg = securityActor.verify(securityToken)
        if (msg.type == VerificationMessage.SEC_CORRECT) {
            authenticated = true
            token = securityToken
        }
        return msg
    }

    fun reset() {
        token = null
        authenticated = false
    }

    companion object {
        @Composable
        fun rememberState(securityActor: SecurityActor) =
            remember(securityActor) { AuthenticationMiniState(securityActor) }

        @Composable
        fun rememberState(activity: FragmentActivity) =
            remember(activity) { AuthenticationMiniState(activity) }
    }
}

class NoteState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
    private val authState: AuthenticationMiniState
) {
    private val noteTabName = CompanionScreen.Note.name

    fun load() {
        noteViewModel.load(authState.token)
    }

    fun NavGraphBuilder.noteGraph() {
        navigation(startDestination = noteTabName, route = "note") {
            composable(noteTabName) {
                val notes by noteViewModel.notes.collectAsState()
                val isLoading by noteViewModel.isLoading.collectAsState()
                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState(initial = false)

                val passwordDialogMiniState = PasswordDialogMiniState.rememberState()

                NoteScreen(
                    noteScreenListHeaderStruct = NoteScreenListHeaderStruct(
                        onSearchClick = { /* TODO */ },
                        onSettingsClick = { navigateToNoteSettings(navController = navController) }
                    ),
                    noteScreenListStruct = NoteScreenListStruct(
                        notes = notes,
                        isLoading = isLoading,
                        onNewClick = { navigateToNoteCreate(navController = navController) },
                        onNoteClick = { note -> navigateToNoteSingle(navController = navController, note = note) },
                        onFavoriteClick = { note ->
                            noteViewModel.with {
                                noteViewModel.setFavorite(note, !note.favorite)
                            }
                        },
                        securityStruct = if (hasSecureNotes) {
                            if (authState.authenticated)
                                NoteScreenListSecurityStruct(
                                    securityText = "Lock secure notes",
                                    onSecurityClick = { authState.reset() }
                                )
                            else
                                NoteScreenListSecurityStruct(
                                    securityText = "Unlock secure notes",
                                    onSecurityClick = {
                                        when (authState.securityActor.type) {
                                            SecurityActor.TYPE_PASS -> { passwordDialogMiniState.open() }
                                            SecurityActor.TYPE_BIO -> noteViewModel.with {
                                                authState.authenticate()
                                            }
                                        }
                                    }
                                )
                        } else {
                            null
                        },
                    ),
                    passwordDialogStruct = PasswordDialogStruct(
                        state = passwordDialogMiniState,
                        onDismiss = { passwordDialogMiniState.close() },
                        onNegativeClick = { passwordDialogMiniState.close() },
                        onPositiveClick = {
                            noteViewModel.with {
                                val msg = authState.authenticate(it)
                                when (msg.type) {
                                    VerificationMessage.SEC_CORRECT -> passwordDialogMiniState.close()
                                    else -> passwordDialogMiniState.stateMessage.value = msg.body?.userMessage
                                }
                            }
                        }
                    )
                )
            }

            composable(
                route = "$noteTabName/settings",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteTabName/settings"
                    }
                )
            ) {
                Timber.d("Note Settings")
                NoteScreenSettings(
                    onExportClick = { /* TODO */ },
                    onImportClick = { /* TODO */ }
                )
            }
            composable(
                route = "$noteTabName/view/{note}",
                arguments = listOf(
                    navArgument("note") {
                        type = NavType.StringType
                    }
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteTabName/view/{note}"
                    }
                ),
            ) { entry ->
                val noteName = entry.arguments?.getString("note")
                if (noteName == null) {
                    Timber.e("View note: Navcontroller navigation - note name == null")
                } else {
                    var note by remember { mutableStateOf<Note?>(null) }
                    noteViewModel.with {
                        note = noteViewModel.getbyName(noteName)
                    }
                    if (note != null)
                        NoteScreenViewSingle(
                            note = note!!,
                            onEditClick = { navigateToNoteEdit(navController = navController, note = it) },
                            onDeleteClick = {
                                noteViewModel.with {
                                    noteViewModel.delete(it)
                                }
                                navController.navigateUp()
                            })
                }
            }
            composable(
                route = "$noteTabName/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteTabName/create"
                    }
                )
            ) {
                Timber.d("New note: Creating")
                val noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState(null, null, false)
                NoteScreenEdit(
                    note = null,
                    overrideDialogMiniState = noteOverrideDialogMiniState,
                    onSaveClick = { note ->
                        Timber.d("Found new note: ${note.name}, ${note.content}, ${note.id}, ${note.favorite}")
                        noteViewModel.with {
                            val conflict = noteViewModel.getbyName(note.name)
                            Timber.d("New note: conflict: ${conflict!=null}")
                            if (conflict == null) {
                                if (noteViewModel.add(note))
                                    navController.navigateUp()
                                else
                                    TODO("Let user know there was a problem while adding note")
                            } else {
                                noteOverrideDialogMiniState.open(note, conflict)
                            }
                        }
                    },
                    onOverrideAcceptClick = { note ->
                        Timber.d("New note: Overriding ${note.name}...")
                        noteViewModel.with {
                            noteViewModel.upsert(note)
                        }
                        noteOverrideDialogMiniState.close()
                        navController.navigateUp()
                    }
                )
            }
            composable(
                route = "$noteTabName/edit/{note}",
                arguments = listOf(
                    navArgument("note") {
                        type = NavType.StringType
                    }
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteTabName/edit/{note}"
                    }
                ),
            ) { entry ->
                val noteName = entry.arguments?.getString("note")
                if (noteName == null) {
                    Timber.e("Edit note: Navcontroller navigation: note name == null")
                } else {
                    Timber.d("Edit note: Editing")
                    val noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState()
                    var existingNote by remember { mutableStateOf<Note?>(null) }
                    noteViewModel.with {
                        existingNote = noteViewModel.getbyName(noteName)!!
                    }
                    if (existingNote != null) {
                        NoteScreenEdit(
                            note = existingNote,
                            overrideDialogMiniState = noteOverrideDialogMiniState,
                            onSaveClick = { note ->
                                Timber.d("Found new note: ${note.name}, ${note.content}, ${note.id}, ${note.favorite}")
                                noteViewModel.with {
                                    // If note name == same as before, there is no conflict. Otherwise, we must check.
                                    val conflict: Note? =
                                        if (note.name == noteName) null else noteViewModel.getbyName(note.name)
                                    Timber.d("Edit note: edited note has changed name=${note.name != noteName}, now conflict: ${conflict != null}")
                                    if (conflict == null) {
                                        val success = noteViewModel.update(existingNote!!, note)
                                        navController.navigateUp()
                                    } else {
                                        noteOverrideDialogMiniState.open(note, conflict)
                                    }
                                }
                            },
                            onOverrideAcceptClick = { note ->
                                Timber.d("Edit note: Overriding note (old name=${noteName}) (new name=${note.name})")
                                noteViewModel.with {
                                    noteViewModel.delete(existingNote!!)
                                    noteViewModel.upsert(note)
                                }
                                noteOverrideDialogMiniState.close()
                                navController.navigateUp()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun navigateToNoteSettings(navController: NavController) = navController.navigate("$noteTabName/settings")
    private fun navigateToNoteSingle(navController: NavController, note: Note) = navigateToNoteSingle(navController, note.name)
    private fun navigateToNoteSingle(navController: NavController, note: String) = navController.navigate("$noteTabName/view/$note")

    private fun navigateToNoteCreate(navController: NavController) = navController.navigate("$noteTabName/create")

    private fun navigateToNoteEdit(navController: NavController, note: Note) = navigateToNoteEdit(navController, note.name)
    private fun navigateToNoteEdit(navController: NavController, note: String) = navController.navigate("$noteTabName/edit/$note")

    companion object {
        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
            authState: AuthenticationMiniState,
            noteViewModel: NoteViewModel) =
            remember(navController) { NoteState(navController, noteViewModel, authState) }
    }
}

class CactusState(private val navController: NavHostController) {
    fun NavGraphBuilder.cactusGraph() {
        val cactusTabName = CompanionScreen.Cactus.name
        navigation(startDestination = cactusTabName, route = "cactus") {
            composable(cactusTabName) { // Overview
                CactusBody(onCactusClick = { })
            }
        }
    }

    companion object {
        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
        ) = remember(navController) {
            CactusState(navController)
        }
    }
}

class AnniversaryState(private val navController: NavHostController, private val anniversaryViewModel: AnniversaryViewModel) {
    fun load() {
        anniversaryViewModel.load()
    }

    fun NavGraphBuilder.anniversaryGraph() {
        val anniversaryTabName = CompanionScreen.Anniversary.name
        navigation(startDestination = anniversaryTabName, route = "anniversary") {
            composable(anniversaryTabName) { // Overview
                val anniversaries by anniversaryViewModel.anniversaries.collectAsState()

                AnniversaryBody(anniversaryList = anniversaries,
                    onNewClick = { navigateToCreateAnniversary(navController) },
                    onAnniversaryClick = {anniversary -> navigateToSingleAnniversary(navController = navController, anniversary = anniversary) },
                    onFavoriteClick = {anniversary -> })
            }
            composable(
                route = "$anniversaryTabName/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$anniversaryTabName/create"
                    }
                )
            ) {
                Timber.d("Creating a new anniversary")
                // TODO: Implement anniversary creation
            }
        }
    }

    private fun navigateToCreateAnniversary(navController: NavHostController) = navController.navigate("${CompanionScreen.Anniversary.name}/create")
    private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: Anniversary) = navigateToSingleAnniversary(navController, anniversary.name)
    private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: String) = navController.navigate("${CompanionScreen.Anniversary.name}/${anniversary}")


    companion object {
        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), anniversaryViewModel: AnniversaryViewModel)
        = remember(navController) { AnniversaryState(navController, anniversaryViewModel) }
    }
}


@Composable
fun Greeting(name: String) {
    Column {
        Text(text = "Hello $name!")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CompanionTheme {
        Greeting("Android")
    }
}