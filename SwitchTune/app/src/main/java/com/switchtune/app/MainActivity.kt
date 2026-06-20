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

    override fun onResume() {
        super.onResume()
        // Path B (clipboard): only auto-detect when we were NOT opened via a
        // share, to avoid overriding the shared link and to limit clipboard reads.
        if (!lastWasShare) {
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

    /** Path B: detect a music link already on the clipboard when the app opens. */
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
