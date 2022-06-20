package org.python.exim

interface Exportable {
    suspend fun export(): Unit
}

object Export {

    suspend fun exportParquet(exportables: Collection<Exportable>) {

    }
}