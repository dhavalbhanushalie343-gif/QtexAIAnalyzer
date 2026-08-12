package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analyzer.ChartDetector
import com.example.analyzer.ChartFrameData
import com.example.analyzer.SignalEngine
import com.example.analyzer.TechnicalIndicators
import com.example.data.db.AppDatabase
import com.example.data.db.SignalEntity
import com.example.data.model.AnalysisResult
import com.example.data.model.DataQuality
import com.example.data.model.ForexPair
import com.example.data.model.SignalType
import com.example.data.remote.MarketDataProvider
import com.example.repository.SignalRepository
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ============================================================
    // DATABASE
    // ============================================================

    private lateinit var repository: SignalRepository

    private val marketDataProvider = MarketDataProvider()

    init {
        try {
            val dao = AppDatabase
                .getDatabase(application)
                .signalDao()

            repository = SignalRepository(dao)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        startScreenCaptureObserver()

        viewModelScope.launch {
            delay(1000L)

            if (!ScreenCaptureService.isCapturing.value) {
                startForexAnalysisLoop()
            }
        }
    }

    // ============================================================
    // HISTORY
    // ============================================================

    val historySignals: StateFlow<List<SignalEntity>> =
        if (::repository.isInitialized) {
            repository.allSignals.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                emptyList()
            )
        } else {
            MutableStateFlow(emptyList())
        }

    val winCount: StateFlow<Int> =
        if (::repository.isInitialized) {
            repository.winCount.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                0
            )
        } else {
            MutableStateFlow(0)
        }

    val lossCount: StateFlow<Int> =
        if (::repository.isInitialized) {
            repository.lossCount.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                0
            )
        } else {
            MutableStateFlow(0)
        }

    val totalCount: StateFlow<Int> =
        if (::repository.isInitialized) {
            repository.totalCount.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                0
            )
        } else {
            MutableStateFlow(0)
        }

    // ============================================================
    // SCREEN CAPTURE
    // ============================================================

    val isCaptureActive: StateFlow<Boolean> =
        ScreenCaptureService.isCapturing

    // ============================================================
    // SETTINGS
    // ============================================================

    private val _isAnalysisPaused =
        MutableStateFlow(false)

    val isAnalysisPaused: StateFlow<Boolean> =
        _isAnalysisPaused.asStateFlow()

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

    // ============================================================
    // CURRENT RESULT
    // ============================================================

    private val _currentResult =
        MutableStateFlow<AnalysisResult?>(null)

    val currentResult: StateFlow<AnalysisResult?> =
        _currentResult.asStateFlow()

    private val _statusMessage =
        MutableStateFlow("Connecting to Live Forex Market...")

    val statusMessage: StateFlow<String> =
        _statusMessage.asStateFlow()

    // ============================================================
    // JOBS
    // ============================================================

    private var analysisJob: Job? = null
    private var screenAnalysisJob: Job? = null

    private var lastSavedSignalTime = 0L

    // ============================================================
    // SUPPORTED FOREX PAIRS
    // ============================================================

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

    // ============================================================
    // SCREEN CAPTURE OBSERVER
    // ============================================================

    private fun startScreenCaptureObserver() {

        screenAnalysisJob?.cancel()

        screenAnalysisJob = viewModelScope.launch {

            ScreenCaptureService.isCapturing
                .collectLatest { active ->

                    if (active) {

                        _statusMessage.value =
                            "Qtex screen capture active — analyzing chart..."

                        startQtexScreenAnalysis()

                    } else {

                        stopQtexScreenAnalysis()

                        if (!_isAnalysisPaused.value) {
                            _statusMessage.value =
                                "Live Forex mode — waiting for market data..."

                            startForexAnalysisLoop()
                        }
                    }
                }
        }
    }

    // ============================================================
    // QTEX SCREEN ANALYSIS
    // ============================================================

    private fun startQtexScreenAnalysis() {

        analysisJob?.cancel()

        analysisJob = viewModelScope.launch {

            var lastFrameTime = 0L

            while (
                isActive &&
                ScreenCaptureService.isCapturing.value
            ) {

                if (!_isAnalysisPaused.value) {

                    val frame =
                        ScreenCaptureService.latestFrame.value

                    if (frame != null) {

                        val now =
                            System.currentTimeMillis()

                        if (now - lastFrameTime >= 1000L) {

                            lastFrameTime = now

                            analyzeQtexFrame(frame)
                        }

                    } else {

                        _statusMessage.value =
                            "Waiting for Qtex screen frame..."
                    }
                }

                delay(250L)
            }
        }
    }

    private suspend fun analyzeQtexFrame(
        bitmap: Bitmap
    ) {

        try {

            _statusMessage.value =
                "Analyzing Qtex chart..."

            val frameData =
                ChartDetector.analyzeFrame(
                    bitmap = bitmap,
                    fallbackPair = _selectedPair.value,
                    fallbackTimeframe = _selectedTimeframe.value,
                    lastKnownPrice = getLastKnownPrice()
                )

            val priceHistory =
                buildPriceHistoryFromFrame(frameData)

            val result =
                SignalEngine.generateSignal(
                    frameData = frameData,
                    priceHistory = priceHistory,
                    minConfidenceThreshold =
                        _minConfidenceThreshold.value,
                    assetName =
                        frameData.detectedPair
                            ?: _selectedPair.value,
                    timeframeName =
                        frameData.detectedTimeframe
                            ?: _selectedTimeframe.value
                )

            _currentResult.value = result

            _statusMessage.value =
                if (frameData.dataQuality == DataQuality.LOW) {
                    "Qtex chart detected — data quality LOW"
                } else {
                    "Qtex LIVE • ${result.asset} • ${result.timeframe}"
                }

            saveSignalIfNeeded(result)

        } catch (e: Exception) {

            e.printStackTrace()

            _statusMessage.value =
                "Qtex analysis error: ${e.message ?: "Unknown error"}"
        }
    }

    // ============================================================
    // BUILD PRICE HISTORY
    // ============================================================

    private fun buildPriceHistoryFromFrame(
        frameData: ChartFrameData
    ): List<Double> {

        val prices =
            frameData.recentCandles.map {
                it.close
            }

        return if (prices.size >= 15) {
            prices
        } else {
            generateFallbackPriceHistory(
                frameData.detectedPrice
                    ?: getLastKnownPrice()
            )
        }
    }

    // ============================================================
    // FALLBACK PRICE HISTORY
    // ============================================================

    private fun generateFallbackPriceHistory(
        currentPrice: Double
    ): List<Double> {

        val result = mutableListOf<Double>()

        var current = currentPrice - 0.0020

        for (i in 0 until 30) {

            val movement = when {
                i % 5 == 0 -> 0.00020
                i % 3 == 0 -> -0.00010
                else -> 0.00005
            }

            current += movement

            result.add(current)
        }

        return result
    }

    // ============================================================
    // LAST KNOWN PRICE
    // ============================================================

    private fun getLastKnownPrice(): Double {

        val selectedPairPrice =
            supportedPairs
                .firstOrNull {
                    it.symbol == _selectedPair.value
                }
                ?.price

        return selectedPairPrice ?: 1.17350
    }

    // ============================================================
    // FOREX LIVE MARKET ANALYSIS LOOP
    // ============================================================

    private fun startForexAnalysisLoop() {

        if (ScreenCaptureService.isCapturing.value) {
            return
        }

        analysisJob?.cancel()

        analysisJob = viewModelScope.launch {

            if (!_isAnalysisPaused.value) {
                analyzeLiveMarket()
            }

            while (isActive) {

                delay(
                    _analysisIntervalMs.value
                        .coerceAtLeast(60_000L)
                )

                if (_isAnalysisPaused.value) {

                    _statusMessage.value =
                        "Analysis Paused"

                    continue
                }

                if (ScreenCaptureService.isCapturing.value) {
                    continue
                }

                analyzeLiveMarket()
            }
        }
    }

    // ============================================================
    // FOREX MARKET ANALYSIS
    // ============================================================

    private suspend fun analyzeLiveMarket() {

        if (ScreenCaptureService.isCapturing.value) {
            return
        }

        if (_isAnalysisPaused.value) {
            return
        }

        val pair =
            _selectedPair.value

        val timeframe =
            _selectedTimeframe.value

        try {

            _statusMessage.value =
                "Fetching live $pair market data..."

            val candles =
                marketDataProvider.getLatestCandles(
                    pair = pair,
                    timeframe = timeframe,
                    limit = 100
                )

            if (candles.isEmpty()) {

                _statusMessage.value =
                    "Live data unavailable — check API/network"

                return
            }

            if (candles.size < 15) {

                _statusMessage.value =
                    "Waiting for enough market candles..."

                return
            }

            val technicalCandles =
                candles.map {

                    TechnicalIndicators.Candle(
                        open = it.open,
                        high = it.high,
                        low = it.low,
                        close = it.close
                    )
                }

            val priceHistory =
                candles.map {
                    it.close
                }

            val currentPrice =
                candles.last().close

            var bullishCount = 0
            var bearishCount = 0

            candles.forEach {

                when {
                    it.close > it.open -> bullishCount++
                    it.close < it.open -> bearishCount++
                }
            }

            val frameData =
                ChartFrameData(
                    isValidChart = true,
                    dataQualityScore =
                        if (candles.size >= 50) 95 else 80,
                    dataQuality =
                        if (candles.size >= 50) {
                            DataQuality.HIGH
                        } else {
                            DataQuality.MEDIUM
                        },
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

            val result =
                SignalEngine.generateSignal(
                    frameData = frameData,
                    priceHistory = priceHistory,
                    minConfidenceThreshold =
                        _minConfidenceThreshold.value,
                    assetName = pair,
                    timeframeName = timeframe
                )

            _currentResult.value = result

            _statusMessage.value =
                "LIVE • $pair • $timeframe • ${candles.size} candles"

            saveSignalIfNeeded(result)

        } catch (e: Exception) {

            e.printStackTrace()

            _statusMessage.value =
                "Market API error: ${e.message ?: "Unknown error"}"
        }
    }

    // ============================================================
    // SAVE SIGNAL
    // ============================================================

    private fun saveSignalIfNeeded(
        result: AnalysisResult
    ) {

        if (!::repository.isInitialized) {
            return
        }

        if (
            result.signalType != SignalType.UP &&
            result.signalType != SignalType.DOWN
        ) {
            return
        }

        val now =
            System.currentTimeMillis()

        if (now - lastSavedSignalTime < 60_000L) {
            return
        }

        lastSavedSignalTime = now

        viewModelScope.launch {

            try {

                repository.saveSignal(

                    SignalEntity(
                        timestamp =
                            result.timestamp,

                        timestampMillis =
                            now,

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

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    fun toggleAnalysisPause() {

        _isAnalysisPaused.value =
            !_isAnalysisPaused.value

        if (_isAnalysisPaused.value) {

            _statusMessage.value =
                "Analysis Paused"

            analysisJob?.cancel()
            analysisJob = null

        } else {

            _statusMessage.value =
                if (ScreenCaptureService.isCapturing.value) {
                    "Qtex screen analysis resumed"
                } else {
                    "Forex analysis resumed"
                }

            if (ScreenCaptureService.isCapturing.value) {
                startQtexScreenAnalysis()
            } else {
                startForexAnalysisLoop()
            }
        }
    }

    fun setAnalysisInterval(
        ms: Long
    ) {

        _analysisIntervalMs.value =
            ms.coerceAtLeast(60_000L)

        if (!ScreenCaptureService.isCapturing.value &&
            !_isAnalysisPaused.value
        ) {
            startForexAnalysisLoop()
        }
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

        viewModelScope.launch {

            if (ScreenCaptureService.isCapturing.value) {

                _statusMessage.value =
                    "Qtex mode • $pair selected"

            } else if (!_isAnalysisPaused.value) {

                analyzeLiveMarket()
            }
        }
    }

    fun setSelectedTimeframe(
        timeframe: String
    ) {

        _selectedTimeframe.value =
            timeframe

        viewModelScope.launch {

            if (ScreenCaptureService.isCapturing.value) {

                _statusMessage.value =
                    "Qtex mode • $timeframe selected"

            } else if (!_isAnalysisPaused.value) {

                analyzeLiveMarket()
            }
        }
    }

    // ============================================================
    // HISTORY
    // ============================================================

    fun markSignalOutcome(
        id: Long,
        outcome: String
    ) {

        if (!::repository.isInitialized) {
            return
        }

        viewModelScope.launch {

            try {
                repository.updateResult(
                    id,
                    outcome
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSignal(
        id: Long
    ) {

        if (!::repository.isInitialized) {
            return
        }

        viewModelScope.launch {

            try {
                repository.deleteSignal(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSignalHistory() {

        if (!::repository.isInitialized) {
            return
        }

        viewModelScope.launch {

            try {
                repository.clearHistory()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    private fun stopQtexScreenAnalysis() {

        analysisJob?.cancel()
        analysisJob = null
    }

    override fun onCleared() {

        analysisJob?.cancel()
        screenAnalysisJob?.cancel()

        super.onCleared()
    }
}
