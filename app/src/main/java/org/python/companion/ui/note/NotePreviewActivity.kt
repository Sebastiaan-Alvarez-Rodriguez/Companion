package org.python.companion.ui.note

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import org.python.companion.ui.theme.CompanionTheme
import timber.log.Timber

class NotePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val note = intent.getStringExtra("note")
        if (note == null)
            Timber.e("Did not receive required intent key 'noteContext'")
        else
            setContent {
                CompanionTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background) {
                        SingleNoteBody(note = note)
                    }
                }
            }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    CompanionTheme {
//        Note(note = Note("A nice title", LoremIpsum(40).values.joinToString(separator = " ")))
//    }
//}