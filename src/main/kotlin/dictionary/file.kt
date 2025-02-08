package dictionary

import java.io.File

fun main() {
    val wordsFile = File("words")
    wordsFile.createNewFile()
    wordsFile.forEachLine { println(it) }
}
