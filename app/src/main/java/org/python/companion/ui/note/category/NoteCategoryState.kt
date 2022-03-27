package org.python.companion.ui.note.category

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.NoteState
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.createRoute
import org.python.companion.viewmodels.NoteCategoryViewModel
import timber.log.Timber

class NoteCategoryState(
    private val navController: NavHostController,
    private val noteCategoryViewModel: NoteCategoryViewModel,
) {
    fun load() {
        noteCategoryViewModel.load()
    }

    fun NavGraphBuilder.categoryGraph() {
        navigation(startDestination = noteCategoryDestination, route = "category") {
            composable(noteCategoryDestination) {
                val noteCategories by noteCategoryViewModel.noteCategories.collectAsState()
                val isLoading by noteCategoryViewModel.isLoading.collectAsState()

                NoteCategoryScreen(
                    noteCategoryScreenListHeaderStruct = NoteCategoryScreenListHeaderStruct(
                        onSearchClick = { /* TODO */ }
                    ),
                    noteCategoryScreenListStruct = NoteCategoryScreenListStruct(
                        noteCategories = noteCategories,
                        isLoading = isLoading,
                        onNewClick = { navigateToNoteCategoryCreate(navController) },
                        onNoteCategoryClick = { navigateToNoteCategoryEdit(navController, it)},
                        onCheckClick = {category, nowChecked ->
                            //TODO: Show delete button
                        },
                        onFavoriteClick = { noteCategory ->
                            noteCategoryViewModel.viewModelScope.launch { noteCategoryViewModel.setFavorite(noteCategory, !noteCategory.favorite) }
                        }
                    )
                )
            }

            composable(
                route = "$noteCategoryDestination/select?selectedId={selectedId}",
                arguments = listOf(navArgument("selectedId") { defaultValue = NoteCategory.DEFAULT.categoryId; type = NavType.LongType })
            ) { entry ->
                val noteCategories by noteCategoryViewModel.noteCategories.collectAsState()
                val isLoading by noteCategoryViewModel.isLoading.collectAsState()

                val selectedId by remember { mutableStateOf(entry.arguments?.getLong("selectedId")) }
                NoteCategoryScreen(
                    noteCategoryScreenListHeaderStruct = NoteCategoryScreenListHeaderStruct(
                        onSearchClick = { /* TODO */ }
                    ),
                    noteCategoryScreenListStruct = NoteCategoryScreenListStruct(
                        noteCategories = noteCategories,
                        isLoading = isLoading,
                        onNewClick = { navigateToNoteCategoryCreate(navController) },
                        onNoteCategoryClick = { navigateToNoteCategoryEdit(navController, it)},
                        onCheckClick = {category, nowChecked ->
                            //TODO: Return selected id?
                            // non-intuitive UI...
                        },
                        onFavoriteClick = { noteCategory ->
                            noteCategoryViewModel.viewModelScope.launch { noteCategoryViewModel.setFavorite(noteCategory, !noteCategory.favorite) }
                        }
                    )
                )
            }

            composable(
                route = "${noteCategoryDestination}/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://${noteCategoryDestination}/create"
                    }
                )
            ) {
                NoteCategoryScreenEditNew(
                    onSaveClick = { toSaveNoteCategory ->
                        Timber.d("Found new noteCategory: ${toSaveNoteCategory.name}, ${toSaveNoteCategory.color}, ${toSaveNoteCategory.categoryId}, ${toSaveNoteCategory.favorite}")
                        noteCategoryViewModel.viewModelScope.launch {
                            val conflict = noteCategoryViewModel.getbyName(toSaveNoteCategory.name)
                            Timber.d("New noteCategory: conflict: ${conflict!=null}")
                            if (conflict == null) {
                                if (noteCategoryViewModel.add(toSaveNoteCategory))
                                    navController.navigateUp()
                                else
                                    TODO("Let user know there was a problem while adding noteCategory")
                            } else {
                                UiUtil.UIUtilState.navigateToOverride(navController) {
                                    Timber.d("New noteCategory: Overriding ${toSaveNoteCategory.name}...")
                                    noteCategoryViewModel.viewModelScope.launch { noteCategoryViewModel.upsert(toSaveNoteCategory) }
                                    navController.navigateUp()
                                }
                            }
                        }
                    }
                )
            }

            composable(
                route = "$noteCategoryDestination/edit/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType }),
                deepLinks = listOf(navDeepLink { uriPattern = "companion://${NoteState.noteDestination}/edit/{categoryId}" }),
            ) { entry ->
                val categoryId = entry.arguments?.getLong("categoryId")!!
                NoteCategoryScreenEdit(
                    noteCategoryViewModel = noteCategoryViewModel,
                    id = categoryId,
                    onSaveClick = { toSaveNoteCategory, existingNoteCategory ->
                        Timber.d("Found new noteCategory: ${toSaveNoteCategory.name}, ${toSaveNoteCategory.color}, ${toSaveNoteCategory.categoryId}, ${toSaveNoteCategory.favorite}")
                        noteCategoryViewModel.viewModelScope.launch {
                            // If category name == same as before, there is no conflict. Otherwise, we must check.
                            val conflict: NoteCategory? = if (toSaveNoteCategory.name == existingNoteCategory!!.name) null else noteCategoryViewModel.getbyName(toSaveNoteCategory.name)
                            Timber.d("Edit noteCategory: edited category has changed name=${toSaveNoteCategory.name != existingNoteCategory.name}, now conflict: ${conflict != null}")
                            if (conflict == null) {
                                noteCategoryViewModel.update(toSaveNoteCategory)
                                navController.navigateUp()
                            } else {
                                UiUtil.UIUtilState.navigateToOverride(navController) {
                                    Timber.d("Edit noteCategory: Overriding category (new name=${toSaveNoteCategory.name})")
                                    noteCategoryViewModel.viewModelScope.launch {
                                        noteCategoryViewModel.delete(existingNoteCategory)
                                        noteCategoryViewModel.upsert(toSaveNoteCategory)
                                    }
                                    navController.navigateUp()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
    companion object {
        val noteCategoryDestination: String = "${NoteState.noteDestination}/category"

        fun navigateToCategoryScreen(navController: NavController) {
            navController.navigate(noteCategoryDestination) {
                launchSingleTop = true
            }
        }
        fun navigateToCategorySelectOrCreate(navController: NavController, noteCategory: NoteCategory?) =
            navigateToCategorySelectOrCreate(navController, noteCategory?.categoryId)
        fun navigateToCategorySelectOrCreate(navController: NavController, selectedId: Long?) {
            navController.navigate(
                    createRoute("$noteCategoryDestination/select",
                        optionals = mapOf(
                            "selectedId" to selectedId?.toString()
                    )
                )
            ) {
                launchSingleTop = true
            }
        }

        private fun navigateToNoteCategoryCreate(navController: NavController) = navController.navigate("$noteCategoryDestination/create")

        private fun navigateToNoteCategoryEdit(navController: NavController, noteCategory: NoteCategory) = navigateToNoteCategoryEdit(navController, noteCategory.categoryId)
        private fun navigateToNoteCategoryEdit(navController: NavController, categoryId: Long) = navController.navigate("$noteCategoryDestination/edit/$categoryId")

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteCategoryViewModel: NoteCategoryViewModel) =
            remember(navController) { NoteCategoryState(navController, noteCategoryViewModel) }
    }
}