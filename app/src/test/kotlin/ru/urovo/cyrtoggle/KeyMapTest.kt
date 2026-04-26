package ru.urovo.cyrtoggle

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyMapTest {

    private fun base(kc: Int) = KeyMap.lookup(kc, 0)
    private fun shift(kc: Int) = KeyMap.lookup(kc, KeyEvent.META_SHIFT_ON)
    private fun ctrl(kc: Int) = KeyMap.lookup(kc, KeyEvent.META_CTRL_ON)
    private fun ctrlShift(kc: Int) = KeyMap.lookup(kc,
        KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON)

    private fun ch(c: Char) = KeyMap.Lookup(KeyMap.Result.Char, c)
    private val silent = KeyMap.Lookup(KeyMap.Result.Silent, null)
    private val passThrough = KeyMap.Lookup(KeyMap.Result.PassThrough, null)

    @Test fun base_letters() {
        assertEquals(ch('а'), base(KeyEvent.KEYCODE_A))
        assertEquals(ch('б'), base(KeyEvent.KEYCODE_B))
        assertEquals(ch('ц'), base(KeyEvent.KEYCODE_C))
        assertEquals(ch('д'), base(KeyEvent.KEYCODE_D))
        assertEquals(ch('е'), base(KeyEvent.KEYCODE_E))
        assertEquals(ch('ф'), base(KeyEvent.KEYCODE_F))
        assertEquals(ch('г'), base(KeyEvent.KEYCODE_G))
        assertEquals(ch('х'), base(KeyEvent.KEYCODE_H))
        assertEquals(ch('и'), base(KeyEvent.KEYCODE_I))
        assertEquals(ch('й'), base(KeyEvent.KEYCODE_J))
        assertEquals(ch('к'), base(KeyEvent.KEYCODE_K))
        assertEquals(ch('л'), base(KeyEvent.KEYCODE_L))
        assertEquals(ch('м'), base(KeyEvent.KEYCODE_M))
        assertEquals(ch('н'), base(KeyEvent.KEYCODE_N))
        assertEquals(ch('о'), base(KeyEvent.KEYCODE_O))
        assertEquals(ch('п'), base(KeyEvent.KEYCODE_P))
        assertEquals(silent, base(KeyEvent.KEYCODE_Q))
        assertEquals(ch('р'), base(KeyEvent.KEYCODE_R))
        assertEquals(ch('с'), base(KeyEvent.KEYCODE_S))
        assertEquals(ch('т'), base(KeyEvent.KEYCODE_T))
        assertEquals(ch('ю'), base(KeyEvent.KEYCODE_U))
        assertEquals(ch('в'), base(KeyEvent.KEYCODE_V))
        assertEquals(silent, base(KeyEvent.KEYCODE_W))
        assertEquals(silent, base(KeyEvent.KEYCODE_X))
        assertEquals(ch('у'), base(KeyEvent.KEYCODE_Y))
        assertEquals(ch('з'), base(KeyEvent.KEYCODE_Z))
        assertEquals(ch('ъ'), base(KeyEvent.KEYCODE_COMMA))
        assertEquals(ch('ь'), base(KeyEvent.KEYCODE_PERIOD))
    }

    @Test fun shift_capitals() {
        assertEquals(ch('А'), shift(KeyEvent.KEYCODE_A))
        assertEquals(ch('Б'), shift(KeyEvent.KEYCODE_B))
        assertEquals(ch('Ц'), shift(KeyEvent.KEYCODE_C))
        assertEquals(ch('Е'), shift(KeyEvent.KEYCODE_E))
        assertEquals(ch('Ю'), shift(KeyEvent.KEYCODE_U))
        assertEquals(ch('У'), shift(KeyEvent.KEYCODE_Y))
        assertEquals(ch('З'), shift(KeyEvent.KEYCODE_Z))
        assertEquals(silent, shift(KeyEvent.KEYCODE_Q))
        assertEquals(silent, shift(KeyEvent.KEYCODE_W))
        assertEquals(silent, shift(KeyEvent.KEYCODE_X))
        assertEquals(ch('ъ'), shift(KeyEvent.KEYCODE_COMMA))
        assertEquals(ch('ь'), shift(KeyEvent.KEYCODE_PERIOD))
    }

    @Test fun ctrl_extras() {
        assertEquals(ch('э'), ctrl(KeyEvent.KEYCODE_A))
        assertEquals(ch('ч'), ctrl(KeyEvent.KEYCODE_C))
        assertEquals(ch('ё'), ctrl(KeyEvent.KEYCODE_E))
        assertEquals(ch('щ'), ctrl(KeyEvent.KEYCODE_S))
        assertEquals(ch('ш'), ctrl(KeyEvent.KEYCODE_T))
        assertEquals(ch('ы'), ctrl(KeyEvent.KEYCODE_U))
        assertEquals(ch('я'), ctrl(KeyEvent.KEYCODE_Y))
        assertEquals(ch('ж'), ctrl(KeyEvent.KEYCODE_Z))
        assertEquals(ch(','), ctrl(KeyEvent.KEYCODE_COMMA))
        assertEquals(ch('.'), ctrl(KeyEvent.KEYCODE_PERIOD))
        // Letters with no Ctrl extra → pass through (Ctrl+B, Ctrl+D, etc.)
        assertEquals(passThrough, ctrl(KeyEvent.KEYCODE_B))
        assertEquals(passThrough, ctrl(KeyEvent.KEYCODE_D))
        assertEquals(passThrough, ctrl(KeyEvent.KEYCODE_F))
    }

    @Test fun ctrl_shift_capital_extras() {
        assertEquals(ch('Э'), ctrlShift(KeyEvent.KEYCODE_A))
        assertEquals(ch('Ч'), ctrlShift(KeyEvent.KEYCODE_C))
        assertEquals(ch('Ё'), ctrlShift(KeyEvent.KEYCODE_E))
        assertEquals(ch('Щ'), ctrlShift(KeyEvent.KEYCODE_S))
        assertEquals(ch('Ш'), ctrlShift(KeyEvent.KEYCODE_T))
        assertEquals(ch('Ы'), ctrlShift(KeyEvent.KEYCODE_U))
        assertEquals(ch('Я'), ctrlShift(KeyEvent.KEYCODE_Y))
        assertEquals(ch('Ж'), ctrlShift(KeyEvent.KEYCODE_Z))
    }

    @Test fun unmappable_keys_pass_through() {
        assertEquals(passThrough, base(KeyEvent.KEYCODE_0))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_9))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_DEL))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_ENTER))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_TAB))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_ESCAPE))
        assertEquals(passThrough, base(KeyEvent.KEYCODE_F1))
    }
}
