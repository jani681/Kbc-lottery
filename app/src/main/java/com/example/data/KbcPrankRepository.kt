package com.example.data

import kotlinx.coroutines.flow.Flow

class KbcPrankRepository(private val kbcPrankDao: KbcPrankDao) {
    val allPranks: Flow<List<KbcPrank>> = kbcPrankDao.getAllPranks()

    suspend fun insertPrank(prank: KbcPrank) {
        kbcPrankDao.insertPrank(prank)
    }

    suspend fun deletePrankById(id: Int) {
        kbcPrankDao.deletePrankById(id)
    }

    suspend fun clearAll() {
        kbcPrankDao.clearAllPranks()
    }
}
