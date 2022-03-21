package org.python.companion.ui.note.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import kotlinx.coroutines.launch
import org.python.companion.NoteState
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteCategoryViewModel
import timber.log.Timber

class NoteCategoryState(
    private val navController: NavHostController,
    private val noteCategoryViewModel: NoteCategoryViewModel,
) {

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
                        onNewClick = { /* TODO: edit screen */ },
                        onNoteCategoryClick = { /* TODO: edit screen */},
                        onFavoriteClick = { /* TODO: toggle favorite */ }
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
        }
    }
    companion object {
        val noteCategoryDestination: String = "${NoteState.noteDestination}/category"

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteCategoryViewModel: NoteCategoryViewModel) =
            remember(navController) { NoteCategoryState(navController, noteCategoryViewModel) }
    }
}