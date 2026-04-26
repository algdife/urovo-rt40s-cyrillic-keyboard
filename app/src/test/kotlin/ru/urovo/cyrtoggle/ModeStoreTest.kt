package ru.urovo.cyrtoggle

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModeStoreTest {

    private lateinit var store: ModeStore

    @Before fun setUp() {
        store = ModeStore(ApplicationProvider.getApplicationContext())
        store.set(Mode.EN)
    }

    @Test fun defaults_to_EN() {
        assertEquals(Mode.EN, store.get())
    }

    @Test fun set_persists() {
        store.set(Mode.RU)
        assertEquals(Mode.RU, store.get())
        val store2 = ModeStore(ApplicationProvider.getApplicationContext())
        assertEquals(Mode.RU, store2.get())
    }

    @Test fun toggle_flips_and_returns_new_mode() {
        assertEquals(Mode.RU, store.toggle())
        assertEquals(Mode.RU, store.get())
        assertEquals(Mode.EN, store.toggle())
        assertEquals(Mode.EN, store.get())
    }
}
