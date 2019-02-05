package com.atmalone.swordsexpress

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.atmalone.swordsexpress.viewmodels.BusInfoViewModel

class busInfoViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return BusInfoViewModel() as T
    }
}