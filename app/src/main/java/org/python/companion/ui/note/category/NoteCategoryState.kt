package org.python.companion.ui.note.category

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.createRoute
import org.python.companion.ui.note.NoteState
import org.python.companion.viewmodels.NoteCategoryViewModel
import timber.log.Timber

class NoteCategoryState(
    private val navController: NavHostController,
    private val noteCategoryViewModel: NoteCategoryViewModel,
    private val scaffoldState: ScaffoldState
) {
    fun load() {
        noteCategoryViewModel.load()
    }

    fun NavGraphBuilder.categoryGraph() {
        navigation(startDestination = noteCategoryDestination, route = "category") {
            composable(
                route = "$noteCategoryDestination/select/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { entry ->
                val noteCategories by noteCategoryViewModel.noteCategories.collectAsState()
                val isLoading by noteCategoryViewModel.isLoading.collectAsState()

                val noteId: Long = entry.arguments?.getLong("noteId")!!
                val selectedCategory by noteCategoryViewModel.categoryForNoteLive(noteId).collectAsState(NoteCategory.DEFAULT)

                val searchParameters by noteCategoryViewModel.searchParameters.collectAsState()

                val defaultPadding = dimensionResource(id = R.dimen.padding_default)

                NoteCategoryScreen(
                    header = {
                        NoteCategoryScreenListHeader(
                            message = "Select a category.",
                            onSearchClick = { noteCategoryViewModel.toggleSearchQuery() }
                        )

                        searchParameters?.let {
                            Spacer(modifier = Modifier.height(defaultPadding))
                            NoteCategoryScreenSearchListHeader(
                                searchParameters = it,
                                onBack = { noteCategoryViewModel.toggleSearchQuery() },
                                onUpdate = { params -> noteCategoryViewModel.updateSearchQuery(params) }
                            )
                        }
                    },
                    list = {
                        NoteCategoryScreenListRadio(
                            noteCategories = noteCategories,
                            selectedItem = selectedCategory,
                            isLoading = isLoading,
                            onNewClick = { navigateToNoteCategoryCreate(navController) },
                            onNoteCategoryClick = { navigateToNoteCategoryEdit(navController, it) },
                            onSelectClick = {category ->
                                noteCategoryViewModel.viewModelScope.launch { noteCategoryViewModel.updateCategoryForNote(noteId, category.categoryId) }
                            },
                            onFavoriteClick = { noteCategory ->
                                noteCategoryViewModel.viewModelScope.launch { noteCategoryViewModel.setFavorite(noteCategory, !noteCategory.favorite) }
                            }
                        )
                    }
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
                    onDeleteClick = { navController.navigateUp() }, // new category, just return without saving
                    onSaveClick = { toSaveNoteCategory ->
                        Timber.d("Found new noteCategory: ${toSaveNoteCategory.name}, ${toSaveNoteCategory.color}, ${toSaveNoteCategory.categoryId}, ${toSaveNoteCategory.favorite}")
                        noteCategoryViewModel.viewModelScope.launch {
                            val conflict = noteCategoryViewModel.getbyName(toSaveNoteCategory.name)
                            Timber.d("New noteCategory: conflict: ${conflict!=null}")
                            when {
                                conflict == null -> {
                                    noteCategoryViewModel.add(toSaveNoteCategory)
                                    navController.navigateUp()
                                }
                                conflict.categoryId == NoteCategory.DEFAULT.categoryId -> { // conflict with default category
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = "Cannot override the default category",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                else ->
                                UiUtil.UIUtilState.navigateToOverride(navController) {
                                    Timber.d("New noteCategory: Overriding ${toSaveNoteCategory.name}...")
                                    noteCategoryViewModel.viewModelScope.launch { noteCategoryViewModel.upsert(toSaveNoteCategory) }
                                    navController.navigateUp()
                                }
                            }
                        }
                    },
                    navController = navController
                )
            }

            composable(
                route = "$noteCategoryDestination/edit/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType }),
                deepLinks = listOf(navDeepLink { uriPattern = "companion://$noteCategoryDestination/edit/{categoryId}" }),
            ) { entry ->
                val categoryId = entry.arguments?.getLong("categoryId")!!
                NoteCategoryScreenEdit(
                    noteCategoryViewModel = noteCategoryViewModel,
                    id = categoryId,
                    onDeleteClick = when (categoryId) {
                            NoteCategory.DEFAULT.categoryId -> null
                            else -> { category ->
                                if (category != null) // existing category, delete and return
                                    noteCategoryViewModel.viewModelScope.launch { noteCategoryViewModel.delete(category) }
                                navController.navigateUp()
                            }
                        },
                    onSaveClick = { toSaveNoteCategory, existingNoteCategory ->
                        Timber.d("Found new noteCategory: ${toSaveNoteCategory.name}, ${toSaveNoteCategory.color}, ${toSaveNoteCategory.categoryId}, ${toSaveNoteCategory.favorite}")
                        noteCategoryViewModel.viewModelScope.launch {
                            // If category name == same as before, there is no conflict. Otherwise, we must check.
                            val conflict: NoteCategory? = if (toSaveNoteCategory.name == existingNoteCategory!!.name) null else noteCategoryViewModel.getbyName(toSaveNoteCategory.name)
                            Timber.d("Edit noteCategory: edited category has changed name=${toSaveNoteCategory.name != existingNoteCategory.name}, now conflict: ${conflict != null}")
                            when {
                                conflict == null -> { // no conflict
                                    noteCategoryViewModel.update(toSaveNoteCategory)
                                    navController.navigateUp()
                                }
                                conflict.categoryId == NoteCategory.DEFAULT.categoryId -> { // conflict with default category
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = "Cannot override the default category",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                else -> // conflict on non-default category
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
                    },
                    navController = navController
                )
            }
        }
    }
    companion object {
        val noteCategoryDestination = "${NoteState.noteDestination}/category"

        fun navigateToCategorySelect(navController: NavController, noteId: Long) =
            navController.navigate(createRoute("$noteCategoryDestination/select", args = listOf(noteId.toString())))

        private fun navigateToNoteCategoryCreate(navController: NavController) = navController.navigate("$noteCategoryDestination/create")

        private fun navigateToNoteCategoryEdit(navController: NavController, noteCategory: NoteCategory) = navigateToNoteCategoryEdit(navController, noteCategory.categoryId)
        private fun navigateToNoteCategoryEdit(navController: NavController, categoryId: Long) = navController.navigate("$noteCategoryDestination/edit/$categoryId")

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteCategoryViewModel: NoteCategoryViewModel, scaffoldState: ScaffoldState) =
            remember(navController) { NoteCategoryState(navController, noteCategoryViewModel, scaffoldState) }
    }
}