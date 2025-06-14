package database.utils

interface DataCleaner {
    fun clearData(tableNames: List<String>)
}