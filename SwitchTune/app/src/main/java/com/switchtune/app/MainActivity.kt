package com.switchtune.app

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.switchtune.app.core.IncomingLinkBus
import com.switchtune.app.core.linkparser.LinkParser
import com.switchtune.app.ui.SwitchTuneRoot
import com.switchtune.app.ui.theme.SwitchTuneTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var incomingLinkBus: IncomingLinkBus

    private var lastWasShare = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            SwitchTuneTheme {
                SwitchTuneRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Path B (clipboard): read the clipboard when the app actually gains window
     * focus. Android 10+ only allows clipboard access to the focused app, so
     * reading in onResume() is unreliable — onWindowFocusChanged is the correct
     * hook and also re-fires every time the user returns to the app.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !lastWasShare) {
            readClipboard()
        }
    }

    /** Path A: a link shared into SwitchTune via the system Share Sheet. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            lastWasShare = true
            incomingLinkBus.post(shared, IncomingLinkBus.Source.SHARE)
        } else {
            lastWasShare = false
        }
    }

    /** Detect a music link already on the clipboard when the app opens/returns. */
    private fun readClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = clipboard.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
        // Only surface clipboard content if it actually contains a music link;
        // otherwise leave the normal empty state untouched (spec edge case).
        if (LinkParser.isMusicLink(text)) {
            incomingLinkBus.post(text, IncomingLinkBus.Source.CLIPBOARD)
        }
    }
}
