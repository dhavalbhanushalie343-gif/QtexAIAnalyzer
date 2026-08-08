package com.example.repository

import com.example.data.db.SignalDao
import com.example.data.db.SignalEntity
import kotlinx.coroutines.flow.Flow

class SignalRepository(private val signalDao: SignalDao) {
    val allSignals: Flow<List<SignalEntity>> = signalDao.getAllSignals()
    val winCount: Flow<Int> = signalDao.getWinCount()
    val lossCount: Flow<Int> = signalDao.getLossCount()
    val totalCount: Flow<Int> = signalDao.getTotalCount()

    suspend fun saveSignal(signal: SignalEntity): Long {
        return signalDao.insertSignal(signal)
    }

    suspend fun updateResult(id: Long, result: String) {
        signalDao.updateSignalResult(id, result)
    }

    suspend fun deleteSignal(id: Long) {
        signalDao.deleteSignalById(id)
    }

    suspend fun clearHistory() {
        signalDao.clearAllSignals()
    }
}
