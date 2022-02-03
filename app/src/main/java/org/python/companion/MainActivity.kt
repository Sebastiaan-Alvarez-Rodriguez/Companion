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
import org.python.backend.security.*
import org.python.companion.support.LoadState
import org.python.companion.support.UiUtil
import org.python.companion.ui.anniversary.AnniversaryBody
import org.python.companion.ui.cactus.CactusBody
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.components.CompanionTabRow
import org.python.companion.ui.note.*
import org.python.companion.ui.note.security.PasswordDialogMiniState
import org.python.companion.ui.note.security.PasswordSetupDialogMiniState
import org.python.companion.ui.note.security.SecurityDialogPickMiniState
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
                    authState = AuthenticationState.rememberState(this, noteViewModel),
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

class AuthenticationUIState(
    open: MutableState<Boolean>,
    val authState: AuthenticationState,
    private val viewmodel: NoteViewModel
) : UiUtil.DialogMiniState(open) {
    @Composable
    fun Dialog() {
        if (open.value) {
            when (authState.securityActor?.type) {
                SecurityActor.TYPE_UNDEFINED, null -> DialogSetup()
                SecurityActor.TYPE_BIO -> DialogBio()
                SecurityActor.TYPE_PASS -> DialogPassword()
                else -> {throw RuntimeException("AuthUIState: How did you get here?")}
            }
        }
    }

    /** Setup dialog: Warning - should only call when it is certain that the user has not picked a type yet. */
    @Composable
    private fun DialogSetup() {
        val pickedState = remember { mutableStateOf(SecurityActor.TYPE_UNDEFINED) }
        when (pickedState.value) {
            SecurityActor.TYPE_UNDEFINED -> DialogSetupPick(pickedActorType = pickedState)
            SecurityActor.TYPE_BIO -> {}
            SecurityActor.TYPE_PASS -> {
                DialogSetupPassword()
            }
        }
    }

    @Composable
    private fun DialogSetupPick(pickedActorType: MutableState<Int>) {
        val securityPickerState = SecurityDialogPickMiniState.rememberState()
        securityPickerState.Dialog(
            onDismiss = { securityPickerState.close() },
            onNegativeClick = { securityPickerState.close() },
            onPositiveClick = { type -> when(type) {
                SecurityActor.TYPE_BIO -> viewmodel.with {
                    pickedActorType.value = type
                    securityPickerState.close()
                    authState.changeSecurityActor(type)
                }
                SecurityActor.TYPE_PASS -> {
                    pickedActorType.value = type
                    securityPickerState.close()
                }
                else -> Timber.e("SecuritySetup: User picked unacceptable type $type")
            }
            }
        )
        securityPickerState.open()
    }

    @Composable
    private fun DialogSetupPassword() {
        val passwordSetupDialogState = PasswordSetupDialogMiniState.rememberState()
        passwordSetupDialogState.Dialog(
            onDismiss = { passwordSetupDialogState.close() },
            onNegativeClick = { passwordSetupDialogState.close() },
            onPositiveClick = { token -> TODO("Store token") }
        )
        passwordSetupDialogState.open()
    }

    @Composable
    private fun DialogBio() { viewmodel.with { authState.authenticate() } }

    @Composable
    private fun DialogPassword() {
        val passwordDialogMiniState = PasswordDialogMiniState.rememberState()
        passwordDialogMiniState.Dialog(
            onDismiss = { passwordDialogMiniState.close() },
            onNegativeClick = { passwordDialogMiniState.close() },
            onPositiveClick = {
                viewmodel.with {
                    passwordDialogMiniState.state.value = LoadState.STATE_LOADING
                    val msg = authState.authenticate(it)
                    passwordDialogMiniState.state.value = when (msg.type) {
                        VerificationMessage.SEC_CORRECT -> {
                            passwordDialogMiniState.close()
                            LoadState.STATE_OK
                        }
                        else -> {
                            passwordDialogMiniState.stateMessage.value = msg.body?.userMessage
                            LoadState.STATE_FAILED
                        }
                    }
                    close()
                }
            }
        )
    }

    companion object {
        @Composable
        fun rememberState(
            authState: AuthenticationState,
            viewmodel: NoteViewModel,
            open: Boolean = false
        ) = remember(open) { AuthenticationUIState(mutableStateOf(open), authState, viewmodel) }
    }
}

class AuthenticationState(var securityActor: SecurityActor?, private val activity: FragmentActivity) {
    var authenticated: Boolean = false
        private set
    var token: VerificationToken? = null
        private set

    constructor(activity: FragmentActivity) : this(
        securityActor =
            when (loadPreferredActor(activity)) {
                SecurityActor.TYPE_UNDEFINED -> null
                else -> SecurityProvider.getActor(loadPreferredActor(activity), activity)
            },
        activity = activity
    )

    suspend fun changeSecurityActor(@SecurityType newType: Int) {
        activity.baseContext
            .getSharedPreferences(SecurityActor.security_storage, Context.MODE_PRIVATE).edit()
            .putInt(SecurityActor.preferred_actor_key, newType)
            .commit()
        securityActor = SecurityProvider.getActor(
            type = activity
                .baseContext
                .getSharedPreferences(SecurityActor.security_storage, Context.MODE_PRIVATE)
                .getInt(SecurityActor.preferred_actor_key, SecurityActor.preferred_actor_default),
            activity = activity)
    }

    suspend fun authenticate(securityToken: VerificationToken? = null): VerificationMessage {
        val msg = securityActor?.verify(securityToken)
            ?: return VerificationMessage.createOther(StatusBody.OtherBody("Did not initialize security actor"))
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
        fun rememberState(activity: FragmentActivity, viewmodel: NoteViewModel) =
            remember(activity) { AuthenticationState(activity) }

        private fun loadPreferredActor(activity: FragmentActivity): @SecurityType Int =
            activity
                .baseContext
                .getSharedPreferences(SecurityActor.security_storage, Context.MODE_PRIVATE)
                .getInt(SecurityActor.preferred_actor_key, SecurityActor.preferred_actor_default)
    }
}

class NoteState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
    private val authState: AuthenticationState
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

                val authUIState = AuthenticationUIState.rememberState(authState, noteViewModel)

                val securityStruct = if (hasSecureNotes) {
                    Timber.w("Authstate authed: ${authState.authenticated}")
                    if (authState.authenticated)
                        NoteScreenListSecurityStruct(
                            securityText = "Lock secure notes",
                            onSecurityClick = {
                                Timber.w("Authstate disable busy: ${authState.authenticated}")
                                authState.reset()
                                Timber.w("Authstate disable complete: ${authState.authenticated}")
                            }
                        )
                    else
                        NoteScreenListSecurityStruct(
                            securityText = "Unlock secure notes",
                            onSecurityClick = {
                                when (authState.securityActor?.type) {
                                    SecurityActor.TYPE_BIO -> noteViewModel.with {
                                        authUIState.close()
                                        authState.authenticate()
                                    }
                                    SecurityActor.TYPE_PASS -> authUIState.open()
                                    else -> { authUIState.open() }
                                }
                            }
                        )
                } else {
                    null
                }
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
                        securityStruct = securityStruct,
                    ),
                    authUIState = authUIState,
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
            authState: AuthenticationState,
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