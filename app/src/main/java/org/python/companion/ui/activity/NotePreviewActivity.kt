package org.python.companion.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import org.python.companion.datatype.Note
import org.python.companion.datatype.NoteContextParcel
import org.python.companion.ui.theme.CompanionTheme
import timber.log.Timber

class NotePreviewActivity : ComponentActivity() {
    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val noteContext = intent.getParcelableExtra<NoteContextParcel>("noteContext")?.member
        if (noteContext == null)
            Timber.e("Did not receive required intent key 'noteContext'")
        else
            setContent {
                CompanionTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background) {
                        Note(noteContext.note)
                    }
                }
            }
    }
}

@Composable
fun Note(note: Note) {
    // remember calculates the value passed to it only during the first composition.
    // It then returns the same value for every subsequent composition.
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.scrollable(state = scrollState, orientation = Orientation.Vertical)) {
        Text(text = note.content)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CompanionTheme {
        Note(note = Note("A nice title", LoremIpsum(40).values.joinToString(separator = " ")))
    }
}