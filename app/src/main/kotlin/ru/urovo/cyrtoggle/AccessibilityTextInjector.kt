package ru.urovo.cyrtoggle

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityTextInjector(private val service: AccessibilityService) : TextInjector {

    private val handler = Handler(Looper.getMainLooper())
    private val clipboard: ClipboardManager =
        service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override fun insert(char: Char): Boolean {
        val focused = findFocusedEditable() ?: run {
            Log.d(TAG, "no focused editable")
            return false
        }
        if (tryActionSetText(focused, char)) {
            Log.d(TAG, "tier1 ACTION_SET_TEXT ok for '$char'")
            return true
        }
        if (tryClipboardPaste(focused, char)) {
            Log.d(TAG, "tier2 ACTION_PASTE ok for '$char'")
            return true
        }
        Log.w(TAG, "all tiers failed for '$char'")
        return false
    }

    private fun findFocusedEditable(): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.takeIf { it.isEditable }
    }

    private fun tryActionSetText(node: AccessibilityNodeInfo, char: Char): Boolean {
        if (!node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }) {
            return false
        }
        val current = node.text?.toString() ?: ""
        val selStart = node.textSelectionStart.coerceAtLeast(0).coerceAtMost(current.length)
        val selEnd = node.textSelectionEnd.coerceAtLeast(selStart).coerceAtMost(current.length)
        val newText = buildString {
            append(current, 0, selStart)
            append(char)
            append(current, selEnd, current.length)
        }
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return false
        // Move cursor to selStart + 1
        val cursorArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selStart + 1)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selStart + 1)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
        return true
    }

    private fun tryClipboardPaste(node: AccessibilityNodeInfo, char: Char): Boolean {
        if (!node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }) {
            return false
        }
        val savedClip = clipboard.primaryClip
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, char.toString()))
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        // Restore the user's clipboard after a short delay so paste completes first.
        handler.postDelayed({
            try {
                if (savedClip != null) clipboard.setPrimaryClip(savedClip)
            } catch (e: Exception) {
                Log.w(TAG, "clipboard restore failed: ${e.message}")
            }
        }, 100)
        return ok
    }

    companion object {
        private const val TAG = "CyrToggle.Inject"
        private const val CLIP_LABEL = "cyr-toggle"
    }
}
