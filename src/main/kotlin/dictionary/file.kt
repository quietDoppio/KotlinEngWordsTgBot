package dictionary

import java.io.File

fun main() {
    val wordsFile = File("words")
    wordsFile.createNewFile()
    for (i in wordsFile.readLines()) {
        println(i)
    }
}
