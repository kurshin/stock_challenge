package com.tastytrade.kurshin.presentation.ui.main

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tastytrade.kurshin.data.persisted.AppDatabase
import com.tastytrade.kurshin.data.persisted.WatchListRepositoryDBImpl
import com.tastytrade.kurshin.data.remote.stock.StockRepositoryImpl
import com.tastytrade.kurshin.data.remote.stock.StockSimulationRepositoryImpl
import com.tastytrade.kurshin.data.remote.stockRetrofit
import com.tastytrade.kurshin.data.remote.symbol.SymbolRepositoryImpl
import com.tastytrade.kurshin.data.remote.symbolRetrofit
import com.tastytrade.kurshin.domain.DEFAULT_WATCHLIST
import com.tastytrade.kurshin.domain.Quote
import com.tastytrade.kurshin.domain.Symbol
import com.tastytrade.kurshin.domain.WatchList
import com.tastytrade.kurshin.domain.irepository.IStockRepository
import com.tastytrade.kurshin.domain.irepository.ISymbolRepository
import com.tastytrade.kurshin.domain.irepository.IWatchListRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class MainViewModel(
    private val stockRepo: IStockRepository,
    private val watchListRepo: IWatchListRepository,
    private val symbolRepo: ISymbolRepository
) : ViewModel() {

    val currentWatchlist = MutableLiveData(DEFAULT_WATCHLIST)
    var selectedSymbols = mutableListOf<Symbol>()

    val error = MutableLiveData<String>()
    val symbols = MutableLiveData<List<Symbol>>()
    var watchList: List<WatchList> = emptyList()

    init {
        fulfillWatchlist()
    }

    fun addWatchList(watchList: WatchList) = viewModelScope.launch {
        watchListRepo.addWatchlist(watchList)
        currentWatchlist.postValue(watchList)
    }

    fun updateWatchList(oldWatchlistName: String, watchList: WatchList) = viewModelScope.launch {
        selectedSymbols.forEach {
            if (oldWatchlistName == it.watchList.name) {
                it.watchList.name = watchList.name
            }
        }
        currentWatchlist.postValue(watchList)
        watchListRepo.updateWatchlist(watchList)
    }

    fun deleteWatchList(watchList: WatchList) = viewModelScope.launch {
        if (watchList.name == currentWatchlist.value?.name) {
            currentWatchlist.postValue(DEFAULT_WATCHLIST)
        }
        watchListRepo.removeWatchlist(watchList)
        selectedSymbols.removeIf { it.watchList ==  watchList}
    }

    suspend fun fetchQuoteData(symbol: String) = stockRepo.fetchQuote(symbol)

    fun searchSymbol(symbol: String) = viewModelScope.launch(errorHandler) {
        val result = symbolRepo.fetchSymbols(symbol)
        symbols.postValue(result)
    }

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        error.postValue(exception.message)
    }

    private fun fulfillWatchlist() = viewModelScope.launch {
        watchListRepo.getAllWatchLists().collect { watchList = it }
    }
}

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(
            StockSimulationRepositoryImpl(),
//            StockRepositoryImpl(stockRetrofit.service), // IEX surprisingly stopped working for free on June 1
            WatchListRepositoryDBImpl(appDatabase.watchListDao()), // Stores watchlist in DB
//            WatchListRepositoryPrefsImpl(context.getTastyTradeSharedPrefs()), // Stored watchlist in SharedPreferences
            SymbolRepositoryImpl(symbolRetrofit.service)
        ) as T
    }
}

fun Context.getTastyTradeSharedPrefs(): SharedPreferences {
    return getSharedPreferences("tastytrade_prefs", Context.MODE_PRIVATE)
}