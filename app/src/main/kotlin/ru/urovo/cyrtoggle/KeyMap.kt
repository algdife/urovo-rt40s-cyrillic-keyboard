package ru.urovo.cyrtoggle

import android.view.KeyEvent

object KeyMap {

    enum class Result { Char, Silent, PassThrough }

    data class Lookup(val result: Result, val char: Char?) {
        companion object {
            fun ch(c: Char) = Lookup(Result.Char, c)
            val silent = Lookup(Result.Silent, null)
            val passThrough = Lookup(Result.PassThrough, null)
        }
    }

    fun lookup(keyCode: Int, metaState: Int): Lookup {
        // Shift OR Caps Lock (XOR — they cancel when both held), so users
        // can either hold Shift or toggle Caps Lock for capitals.
        val shiftBit = (metaState and KeyEvent.META_SHIFT_ON) != 0
        val capsBit = (metaState and KeyEvent.META_CAPS_LOCK_ON) != 0
        val shift = shiftBit xor capsBit
        val ctrl = (metaState and KeyEvent.META_CTRL_ON) != 0
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> letter('а', 'А', extra = 'э', extraCap = 'Э', ctrl, shift)
            KeyEvent.KEYCODE_B -> letter('б', 'Б', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_C -> letter('ц', 'Ц', extra = 'ч', extraCap = 'Ч', ctrl, shift)
            KeyEvent.KEYCODE_D -> letter('д', 'Д', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_E -> letter('е', 'Е', extra = 'ё', extraCap = 'Ё', ctrl, shift)
            KeyEvent.KEYCODE_F -> letter('ф', 'Ф', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_G -> letter('г', 'Г', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_H -> letter('х', 'Х', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_I -> letter('и', 'И', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_J -> letter('й', 'Й', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_K -> letter('к', 'К', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_L -> letter('л', 'Л', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_M -> letter('м', 'М', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_N -> letter('н', 'Н', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_O -> letter('о', 'О', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_P -> letter('п', 'П', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_Q -> if (ctrl) Lookup.passThrough else Lookup.silent
            KeyEvent.KEYCODE_R -> letter('р', 'Р', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_S -> letter('с', 'С', extra = 'щ', extraCap = 'Щ', ctrl, shift)
            KeyEvent.KEYCODE_T -> letter('т', 'Т', extra = 'ш', extraCap = 'Ш', ctrl, shift)
            KeyEvent.KEYCODE_U -> letter('ю', 'Ю', extra = 'ы', extraCap = 'Ы', ctrl, shift)
            KeyEvent.KEYCODE_V -> letter('в', 'В', extra = null, extraCap = null, ctrl, shift)
            KeyEvent.KEYCODE_W -> if (ctrl) Lookup.passThrough else Lookup.silent
            KeyEvent.KEYCODE_X -> if (ctrl) Lookup.passThrough else Lookup.silent
            KeyEvent.KEYCODE_Y -> letter('у', 'У', extra = 'я', extraCap = 'Я', ctrl, shift)
            KeyEvent.KEYCODE_Z -> letter('з', 'З', extra = 'ж', extraCap = 'Ж', ctrl, shift)
            KeyEvent.KEYCODE_COMMA  -> if (ctrl) Lookup.ch(',') else Lookup.ch('ъ')
            KeyEvent.KEYCODE_PERIOD -> if (ctrl) Lookup.ch('.') else Lookup.ch('ь')
            else -> Lookup.passThrough
        }
    }

    private fun letter(
        lower: Char, upper: Char,
        extra: Char?, extraCap: Char?,
        ctrl: Boolean, shift: Boolean
    ): Lookup = when {
        ctrl && shift && extraCap != null -> Lookup.ch(extraCap)
        ctrl && extra != null              -> Lookup.ch(extra)
        ctrl                               -> Lookup.passThrough
        shift                              -> Lookup.ch(upper)
        else                               -> Lookup.ch(lower)
    }
}
