package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analyzer.ChartFrameData
import com.example.analyzer.SignalEngine
import com.example.analyzer.TechnicalIndicators
import com.example.data.MarketCandle
import com.example.data.db.AppDatabase
import com.example.data.db.SignalEntity
import com.example.data.model.AnalysisResult
import com.example.data.model.DataQuality
import com.example.data.model.ForexPair
import com.example.data.model.SignalType
import com.example.data.remote.MarketDataProvider
import com.example.repository.SignalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SignalRepository
    private val marketDataProvider = MarketDataProvider()

    init {
        val dao = AppDatabase.getDatabase(application).signalDao()
        repository = SignalRepository(dao)
    }

    // ---------------- HISTORY ----------------

    val historySignals: StateFlow<List<SignalEntity>> =
        repository.allItemsFlow()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val winCount: StateFlow<Int> =
        repository.winCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

    val lossCount: StateFlow<Int> =
        repository.lossCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

    val totalCount: StateFlow<Int> =
        repository.totalCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

    // ---------------- SCREEN CAPTURE ----------------
    // फिलहाल UI compatibility के लिए रखा है।
    // Analysis अब screen से नहीं बल्कि Market API से होगा।

    private val _isCaptureActive = MutableStateFlow(false)
    val isCaptureActive: StateFlow<Boolean> =
        _isCaptureActive.asStateFlow()

    // ---------------- SETTINGS ----------------

    private val _isAnalysisPaused =
        MutableStateFlow(false)

    val isAnalysisPaused: StateFlow<Boolean> =
        _isAnalysisPaused.asStateFlow()

    /*
     * API को हर 500ms call नहीं करना है।
     * Twelve Data usage बचाने के लिए minimum 60 seconds रखा है।
     */
    private val _analysisIntervalMs =
        MutableStateFlow(60_000L)

    val analysisIntervalMs: StateFlow<Long> =
        _analysisIntervalMs.asStateFlow()

    private val _minConfidenceThreshold =
        MutableStateFlow(65)

    val minConfidenceThreshold: StateFlow<Int> =
        _minConfidenceThreshold.asStateFlow()

    private val _selectedMode =
        MutableStateFlow("FOREX")

    val selectedMode: StateFlow<String> =
        _selectedMode.asStateFlow()

    private val _selectedPair =
        MutableStateFlow("EUR/USD")

    val selectedPair: StateFlow<String> =
        _selectedPair.asStateFlow()

    private val _selectedTimeframe =
        MutableStateFlow("1 MIN")

    val selectedTimeframe: StateFlow<String> =
        _selectedTimeframe.asStateFlow()

    // ---------------- CURRENT RESULT ----------------

    private val _currentResult =
        MutableStateFlow<AnalysisResult?>(null)

    val currentResult: StateFlow<AnalysisResult?> =
        _currentResult.asStateFlow()

    private val _statusMessage =
        MutableStateFlow("Connecting to Live Forex Market...")

    val statusMessage: StateFlow<String> =
        _statusMessage.asStateFlow()

    private var analysisJob: Job? = null
    private var lastSavedSignalTime = 0L

    // ---------------- SUPPORTED PAIRS ----------------

    val supportedPairs = listOf(
        ForexPair(
            "EUR/USD",
            "Euro / US Dollar",
            1.17342
        ),
        ForexPair(
            "GBP/USD",
            "British Pound / US Dollar",
            1.31250
        ),
        ForexPair(
            "USD/JPY",
            "US Dollar / Japanese Yen",
            154.200
        ),
        ForexPair(
            "USD/CHF",
            "US Dollar / Swiss Franc",
            0.88450
        ),
        ForexPair(
            "AUD/USD",
            "Australian Dollar / US Dollar",
            0.65820
        ),
        ForexPair(
            "USD/CAD",
            "US Dollar / Canadian Dollar",
            1.36500
        ),
        ForexPair(
            "NZD/USD",
            "New Zealand Dollar / US Dollar",
            0.59810
        )
    )

    init {
        startAnalysisLoop()
    }

    // ============================================================
    // LIVE MARKET ANALYSIS
    // ============================================================

    private fun startAnalysisLoop() {

        analysisJob?.cancel()

        analysisJob = viewModelScope.launch {

            // पहला API request तुरंत
            analyzeLiveMarket()

            while (true) {

                val waitTime =
                    _analysisIntervalMs.value.coerceAtLeast(60_000L)

                delay(waitTime)

                if (_isAnalysisPaused.value) {
                    _statusMessage.value =
                        "Analysis Paused"
                    continue
                }

                analyzeLiveMarket()
            }
        }
    }

    private suspend fun analyzeLiveMarket() {

        val pair = _selectedPair.value
        val timeframe = _selectedTimeframe.value

        try {

            _statusMessage.value =
                "Fetching live $pair market data..."

            // ---------------------------------------------
            // Get REAL candles from API
            // ---------------------------------------------

            val candles =
                marketDataProvider.getLatestCandles(
                    pair = pair,
                    timeframe = timeframe,
                    limit = 100
                )

            if (candles.isEmpty()) {

                _statusMessage.value =
                    "Live data unavailable — check API key/network"

                return
            }

            if (candles.size < 15) {

                _statusMessage.value =
                    "Waiting for enough market candles..."

                return
            }

            // ---------------------------------------------
            // Convert API candles → technical candles
            // ---------------------------------------------

            val technicalCandles =
                candles.map {
                    TechnicalIndicators.Candle(
                        open = it.open,
                        high = it.high,
                        low = it.low,
                        close = it.close
                    )
                }

            // ---------------------------------------------
            // Close price history
            // ---------------------------------------------

            val priceHistory =
                candles.map {
                    it.close
                }

            val currentPrice =
                candles.last().close

            // ---------------------------------------------
            // Count bullish / bearish candles
            // ---------------------------------------------

            var bullishCount = 0
            var bearishCount = 0

            candles.forEach {

                when {
                    it.close > it.open ->
                        bullishCount++

                    it.close < it.open ->
                        bearishCount++
                }
            }

            // ---------------------------------------------
            // Build chart data from REAL candles
            // ---------------------------------------------

            val frameData =
                ChartFrameData(
                    isValidChart = true,

                    dataQualityScore =
                        if (candles.size >= 50) 95 else 80,

                    dataQuality =
                        if (candles.size >= 50)
                            DataQuality.HIGH
                        else
                            DataQuality.MEDIUM,

                    bullishPixelCount =
                        bullishCount,

                    bearishPixelCount =
                        bearishCount,

                    detectedPrice =
                        currentPrice,

                    detectedPair =
                        pair,

                    detectedTimeframe =
                        timeframe,

                    recentCandles =
                        technicalCandles.takeLast(20)
                )

            // ---------------------------------------------
            // AI / Technical Signal Engine
            // ---------------------------------------------

            val result =
                SignalEngine.generateSignal(

                    frameData = frameData,

                    priceHistory =
                        priceHistory,

                    minConfidenceThreshold =
                        _minConfidenceThreshold.value,

                    assetName =
                        pair,

                    timeframeName =
                        timeframe
                )

            _currentResult.value = result

            _statusMessage.value =
                "LIVE • $pair • $timeframe • ${candles.size} candles"

            // ---------------------------------------------
            // Save UP / DOWN signal
            // ---------------------------------------------

            val now =
                System.currentTimeMillis()

            if (
                (result.signalType == SignalType.UP ||
                 result.signalType == SignalType.DOWN) &&
                now - lastSavedSignalTime > 60_000L
            ) {

                lastSavedSignalTime = now

                saveSignalToDb(result)
            }

        } catch (e: Exception) {

            _statusMessage.value =
                "Market API error: ${e.message ?: "Unknown error"}"
        }
    }

    // ============================================================
    // DATABASE
    // ============================================================

    private fun saveSignalToDb(
        result: AnalysisResult
    ) {

        viewModelScope.launch {

            repository.saveSignal(

                SignalEntity(

                    timestamp =
                        result.timestamp,

                    asset =
                        result.asset,

                    price =
                        result.price,

                    timeframe =
                        result.timeframe,

                    signalType =
                        result.signalType.name,

                    confidence =
                        result.confidence,

                    reason =
                        result.reason,

                    dataQuality =
                        result.dataQuality.name,

                    userResult =
                        "PENDING"
                )
            )
        }
    }

    // ============================================================
    // SETTINGS FUNCTIONS
    // ============================================================

    fun toggleAnalysisPause() {

        _isAnalysisPaused.value =
            !_isAnalysisPaused.value
    }

    fun setAnalysisInterval(ms: Long) {

        /*
         * API को बहुत तेजी से call करने से बचाने के लिए
         * minimum 60 seconds.
         */
        _analysisIntervalMs.value =
            ms.coerceAtLeast(60_000L)

        startAnalysisLoop()
    }

    fun setMinConfidenceThreshold(
        threshold: Int
    ) {

        _minConfidenceThreshold.value =
            threshold.coerceIn(50, 95)
    }

    fun setSelectedMode(
        mode: String
    ) {

        _selectedMode.value =
            mode
    }

    fun setSelectedPair(
        pair: String
    ) {

        _selectedPair.value =
            pair

        // Pair बदलते ही नया data
        viewModelScope.launch {
            analyzeLiveMarket()
        }
    }

    fun setSelectedTimeframe(
        timeframe: String
    ) {

        _selectedTimeframe.value =
            timeframe

        // Timeframe बदलते ही नया data
        viewModelScope.launch {
            analyzeLiveMarket()
        }
    }

    // ============================================================
    // HISTORY FUNCTIONS
    // ============================================================

    fun markSignalOutcome(
        id: Long,
        outcome: String
    ) {

        viewModelScope.launch {

            repository.updateResult(
                id,
                outcome
            )
        }
    }

    fun deleteSignal(
        id: Long
    ) {

        viewModelScope.launch {

            repository.deleteSignal(id)
        }
    }

    fun clearSignalHistory() {

        viewModelScope.launch {

            repository.clearHistory()
        }
    }

    override fun onCleared() {

        analysisJob?.cancel()

        super.onCleared()
    }
}

// ================================================================
// Repository Flow bridge
// ================================================================

private fun SignalRepository.allItemsFlow() =
    allSignals
