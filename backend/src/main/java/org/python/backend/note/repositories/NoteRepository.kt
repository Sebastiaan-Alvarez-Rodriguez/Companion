package org.python.backend.note.repositories

class NoteRepository(private val todoDatabaseDao: TodoDatabaseDao) {    val readAllData : LiveData<List<TodoItem>> =  todoDatabaseDao.getAll()    suspend fun addTodo(todoItem: TodoItem) {
    todoDatabaseDao.insert(todoItem)
}    suspend fun updateTodo(todoItem: TodoItem) {
    todoDatabaseDao.update(todoItem)
}    suspend fun deleteTodo(todoItem: TodoItem) {
    todoDatabaseDao.delete(todoItem)
}    suspend fun deleteAllTodos() {
    todoDatabaseDao.deleteAllTodos()
}
}