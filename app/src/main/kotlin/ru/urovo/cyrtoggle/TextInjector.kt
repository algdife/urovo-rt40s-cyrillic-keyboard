package ru.urovo.cyrtoggle

interface TextInjector {
    /** Inserts [char] at the current cursor in the focused editable field.
     *  Returns true on success, false if no field could be reached. */
    fun insert(char: Char): Boolean
}
