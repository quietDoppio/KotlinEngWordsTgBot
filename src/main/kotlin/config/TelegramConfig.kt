package config

import config.TelegramConfig.Emojis.CHECKMARK
import config.TelegramConfig.Emojis.CROSSMARK
import config.TelegramConfig.Emojis.ENG_FLAG
import config.TelegramConfig.Emojis.HUNDRED
import config.TelegramConfig.Emojis.RU_FLAG

object TelegramConfig {
    object Api {
        const val API_BASE_URL = "https://api.telegram.org/bot"
        const val API_SEND_MESSAGE = "/sendMessage"
        const val API_DELETE_MESSAGES = "/deleteMessages"
        const val API_EDIT_MESSAGE_TEXT = "/editMessageText"
        const val API_GET_UPDATES = "/getUpdates"
        const val API_GET_FILE = "/getFile"
        const val API_SEND_PHOTO = "/sendPhoto"
    }

    object Emojis {
        val answerOptionEmojis = ('1'..'4').map { it -> "$it$DIGITS" }

        const val CHECKMARK = "\u2705"
        const val CROSSMARK = "\u274C"
        const val HUNDRED = "\uD83D\uDCAF"
        const val ENG_FLAG = "\uD83C\uDDFA\uD83C\uDDF8"
        const val RU_FLAG = "\uD83C\uDDF7\uD83C\uDDFA"
        const val DIGITS = "\uFE0F\u20E3"
    }

    object Messages {
        const val WORDS_NOT_ADDED = "Слова не добавлены\nНекорректный файл или данные"
        const val HINT_DELETED = "Подсказка удалена! $CHECKMARK"
        const val HINT_NOT_DELETED = "Нечего удалять"
        const val INCORRECT_INPUT = "Некорректная строка"
        const val STATISTIC_FORMAT = "Выучено %d из %d слов | %d%%"
        const val STATISTICS_RESET = "Статистика успешно сброшена"
        const val ALL_WORDS_DELETED = "Все слова удалены"
        const val NUM_OF_WORD_REQUESTED = "Напишите номер слова для редактирования: "
        const val DICTIONARY_IS_EMPTY = "Словарь пуст"
        const val ENTER_NUM_OF_WORDS_LIST = "Введите номер слова из списка"
        const val ENTER_CYRILLIC = "Введите слово на кириллице"
        const val ENTER_LATIN = "Введите слово на латинице"
        const val SET_PICTURE_REQUEST = "Прикрепите изображение"
        const val COMPRESSED_PICTURE_REQUIRED = "Необходимо сжатое изображение"
        const val WORDS_ADDED = "Слова добавлены!"
        const val LEARNING_COMPLETED = "$HUNDRED Вы выучили все слова в базе или они отсутствуют"
        const val WORD_CORRECT_ANSWER = "$CHECKMARK Верно!"
        const val WORD_INCORRECT_ANSWER = "$CROSSMARK Не верно!"
        const val WORD_ADDED = "Добавлено! $CHECKMARK"
        const val PICTURE_HAS_BEEN_SET = "Прикреплено! $CHECKMARK"
        const val WORD_HAS_CHANGED = "Изменено $CHECKMARK"
        const val WORD_HAS_NOT_CHANGED = "Слово \"%s\" уже существует. Невозможно изменить"
        const val WORD_HAS_NOT_ADDED = "Слово \"%s\" уже существует. Невозможно добавить"
        const val ORIGINAL_WORD_REQUEST = "Введите оригинал $ENG_FLAG:"
        const val TRANSLATE_REQUEST = "Введите перевод $RU_FLAG"
        const val WORD_DELETED = "Слово \"%s\" удалено! $CHECKMARK"
        const val INFO_ADD_WORDS =
            "Можно добавить слова с помощью .txt файла\n\n" +
                    "Создайте файл и впишите слова в формате -\n" +
                    "four|четыре\n" +
                    "bird|птица\n" +
                    "memory|память\n\n" +
                    "Отправьте файл в бот\n\n" +
                    "\'|\' - обязательный разделитель\n" +
                    "Каждая группа слов должна начинаться с новой строки\n" +
                    "Первое слова всегда на латинице, второе всегда на кириллице"
    }

    object CallbackData {
        const val ANSWER_PREFIX = "answer_"

        enum class CallbacksEnum(val data: String, val buttonTitle: String) {
            GO_BACK_TO_WORD_MENU_CLICKED("GO_BACK_TO_WORD_MENU_CLICKED", "Назад"),
            GO_BACK_TO_SELECT_WORD_SESSION_CLICKED("GO_BACK_TO_SELECT_WORD_SESSION_CLICKED", "Назад"),
            SET_PICTURE_SESSION_CLICKED("SET_PICTURE_SESSION_CLICKED", "Установить подсказку"),
            DELETE_WORD_CLICKED("DELETE_WORD_CLICKED", "Удалить"),
            EDIT_WORD_SESSION_CLICKED("EDIT_WORD_SESSION_CLICKED", "Редактировать"),
            STATISTICS_CLICKED("STATISTICS_CLICKED", "Статистика"),
            START_LEARNING_CLICKED("START_LEARNING_CLICKED", "Изучение слов"),
            RESET_CLICKED("RESET_CLICKED", "Сбросить статистику"),
            RETURN_MAIN_MENU_CLICKED("RETURN_MAIN_MENU_CLICKED", "Меню"),
            ADD_WORDS_SESSION_CLICKED("ADD_WORDS_SESSION_CLICKED", "Добавить слова"),
            SELECT_WORD_SESSION_CLICKED("SELECT_WORD_SESSION_CLICKED", "Показать словарь"),
            DELETE_DICTIONARY_CLICKED("DELETE_DICTIONARY_CLICKED", "Удалить всё"),
            DELETE_HINT_CLICKED("DELETE_HINT_CLICKED", "Удалить подсказку");

            companion object {
                fun fromKey(key: String): CallbacksEnum? = entries.find { it.data == key }
            }
        }
    }

    enum class KeyWords(val key: String) {
        START("/start"),
        INFO("/info");

        companion object {
            fun getKeyWordFromKey(key: String): KeyWords? =
                KeyWords.entries.find { it.key == key }
        }
    }
}