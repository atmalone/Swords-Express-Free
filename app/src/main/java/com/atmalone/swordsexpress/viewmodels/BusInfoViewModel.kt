package com.atmalone.swordsexpress.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.atmalone.swordsexpress.models.BusInfo
import com.atmalone.swordsexpress.BusInfoRepository

class BusInfoViewModel : ViewModel() {

    val reposResult = MutableLiveData<MutableList<BusInfo>>()

    private val busInfoRepository = BusInfoRepository.instance()

    init {
        loadBusInfoListFromUrl()
    }

    fun loadBusInfoListFromUrl(): MutableList<BusInfo> {
        return busInfoRepository.getBusInfoListFromResponseBody { repos ->
            reposResult.postValue(repos)
        }
    }
}