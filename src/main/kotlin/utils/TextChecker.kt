package utils

object TextChecker {
    private val cyrillicCharRange = 'А'..'я'
    private val upperLatinRange = 'A'..'Z'
    private val lowerLatinRange = 'a'..'z'

    private fun isCharLatin(char: Char) = char in upperLatinRange || char in lowerLatinRange
    private fun isCharCyrillic(char: Char) = char in cyrillicCharRange || char == 'Ё' || char == 'ё'

    fun String.isTextCyrillic(): Boolean {
        val hasLatin = any { isCharLatin(it) }
        if (hasLatin) return false

        val hasCyrillic = any { isCharCyrillic(it) }
        return hasCyrillic
    }

    fun String.isTextLatin(): Boolean {
        val hasCyrillic = any { isCharCyrillic(it) }
        if (hasCyrillic) return false

        val hasLatin = any { isCharLatin(it) }
        return hasLatin
    }

    fun String.isInputCorrect(): Boolean =
        any { it.isLetter() } && firstOrNull()?.isLetter() == true && contains("\n") == false
}
