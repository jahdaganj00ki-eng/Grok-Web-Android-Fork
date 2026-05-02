package com.groklauncher.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {
    fun shareText(context: Context, subject: String, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    fun openInBrowser(context: Context, uri: Uri) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

