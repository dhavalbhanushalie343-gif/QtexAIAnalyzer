package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analyzer.ChartDetector
import com.example.analyzer.SignalEngine
import com.example.data.db.AppDatabase
import com.example.data.db.SignalEntity
import com.example.data.model.AnalysisResult
import com.example.data.model.DataQuality
import com.example.data.model.ForexPair
import com.example.data.model.SignalType
import com.example.repository.SignalRepository
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SignalRepository

    init {
        val dao = AppDatabase.getDatabase(application).signalDao()
        repository = SignalRepository(dao)
    }

    val historySignals: StateFlow<List<SignalEntity>> = repository.allItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val winCount: StateFlow<Int> = repository.winCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lossCount: StateFlow<Int> = repository.lossCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Capture state from Foreground Service
    val isCaptureActive: StateFlow<Boolean> = ScreenCaptureService.isCapturing

    // Configuration Settings
    private val _isAnalysisPaused = MutableStateFlow(false)
    val isAnalysisPaused: StateFlow<Boolean> = _isAnalysisPaused.asStateFlow()

    private val _analysisIntervalMs = MutableStateFlow(500L)
    val analysisIntervalMs: StateFlow<Long> = _analysisIntervalMs.asStateFlow()

    private val _minConfidenceThreshold = MutableStateFlow(65)
    val minConfidenceThreshold: StateFlow<Int> = _minConfidenceThreshold.asStateFlow()

    private val _selectedMode = MutableStateFlow("QTEX") // "QTEX" or "FOREX"
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _selectedPair = MutableStateFlow("EUR/USD")
    val selectedPair: StateFlow<String> = _selectedPair.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow("1 MIN")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    // Current Analysis Output
    private val _currentResult = MutableStateFlow<AnalysisResult?>(null)
    val currentResult: StateFlow<AnalysisResult?> = _currentResult.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready for Screen Capture")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Price History buffer for indicator calculations
    private val priceHistoryMap = mutableMapOf<String, MutableList<Double>>()
    private var analysisJob: Job? = null
    private var lastSavedSignalTime = 0L

    val supportedPairs = listOf(
        ForexPair("EUR/USD", "Euro / US Dollar", 1.17342),
        ForexPair("GBP/USD", "British Pound / US Dollar", 1.31250),
        ForexPair("USD/JPY", "US Dollar / Japanese Yen", 154.200),
        ForexPair("USD/CHF", "US Dollar / Swiss Franc", 0.88450),
        ForexPair("AUD/USD", "Australian Dollar / US Dollar", 0.65820),
        ForexPair("USD/CAD", "US Dollar / Canadian Dollar", 1.36500),
        ForexPair("NZD/USD", "New Zealand Dollar / US Dollar", 0.59810)
    )

    init {
        // Initialize base price history for supported pairs
        supportedPairs.forEach { pair ->
            val list = mutableListOf<Double>()
            var p = pair.basePrice
            for (i in 0..30) {
                p += (Random.nextDouble() - 0.49) * 0.00040
                list.add(p)
            }
            priceHistoryMap[pair.symbol] = list
        }

        startAnalysisLoop()
    }

    private fun startAnalysisLoop() {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            while (true) {
                delay(_analysisIntervalMs.value)

                if (_isAnalysisPaused.value) {
                    _statusMessage.value = "Analysis Paused"
                    continue
                }

                val currentPairSymbol = _selectedPair.value
                val history = priceHistoryMap.getOrPut(currentPairSymbol) { mutableListOf() }

                // Simulate realistic price movement tick
                val lastP = history.lastOrNull() ?: 1.17350
                val tickChange = (Random.nextDouble() - 0.495) * 0.00018
                val newPrice = (lastP + tickChange).coerceAtLeast(0.0001)
                history.add(newPrice)
                if (history.size > 100) history.removeAt(0)

                val capturedBitmap: Bitmap? = ScreenCaptureService.latestFrame.value

                val frameData = if (isCaptureActive.value) {
                    _statusMessage.value = "Screen Capture Active — Analyzing Qtex chart"
                    ChartDetector.analyzeFrame(
                        bitmap = capturedBitmap,
                        fallbackPair = currentPairSymbol,
                        fallbackTimeframe = _selectedTimeframe.value,
                        lastKnownPrice = newPrice
                    )
                } else {
                    _statusMessage.value = "Live Forex Market Mode — Tap Start Screen Capture for Qtex"
                    ChartDetector.analyzeFrame(
                        bitmap = null,
                        fallbackPair = currentPairSymbol,
                        fallbackTimeframe = _selectedTimeframe.value,
                        lastKnownPrice = newPrice
                    )
                }

                val result = SignalEngine.generateSignal(
                    frameData = frameData,
                    priceHistory = history,
                    minConfidenceThreshold = _minConfidenceThreshold.value,
                    assetName = currentPairSymbol,
                    timeframeName = _selectedTimeframe.value
                )

                _currentResult.value = result

                // Auto-save signal to Room Database if it's an UP or DOWN signal (throttle every 12 sec)
                val currentTime = System.currentTimeMillis()
                if ((result.signalType == SignalType.UP || result.signalType == SignalType.DOWN) &&
                    (currentTime - lastSavedSignalTime > 12000)
                ) {
                    lastSavedSignalTime = currentTime
                    saveSignalToDb(result)
                }
            }
        }
    }

    private fun saveSignalToDb(result: AnalysisResult) {
        viewModelScope.launch {
            repository.saveSignal(
                SignalEntity(
                    timestamp = result.timestamp,
                    asset = result.asset,
                    price = result.price,
                    timeframe = result.timeframe,
                    signalType = result.signalType.name,
                    confidence = result.confidence,
                    reason = result.reason,
                    dataQuality = result.dataQuality.name,
                    userResult = "PENDING"
                )
            )
        }
    }

    fun toggleAnalysisPause() {
        _isAnalysisPaused.value = !_isAnalysisPaused.value
    }

    fun setAnalysisInterval(ms: Long) {
        _analysisIntervalMs.value = ms
        startAnalysisLoop()
    }

    fun setMinConfidenceThreshold(threshold: Int) {
        _minConfidenceThreshold.value = threshold.coerceIn(50, 95)
    }

    fun setSelectedMode(mode: String) {
        _selectedMode.value = mode
    }

    fun setSelectedPair(pair: String) {
        _selectedPair.value = pair
    }

    fun setSelectedTimeframe(timeframe: String) {
        _selectedTimeframe.value = timeframe
    }

    fun markSignalOutcome(id: Long, outcome: String) {
        viewModelScope.launch {
            repository.updateResult(id, outcome)
        }
    }

    fun deleteSignal(id: Long) {
        viewModelScope.launch {
            repository.deleteSignal(id)
        }
    }

    fun clearSignalHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

// Extension to bridge Flow in repository
private fun SignalRepository.allItemsFlow() = allSignals
