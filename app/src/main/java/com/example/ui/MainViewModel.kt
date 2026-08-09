package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analyzer.ChartDetector
import com.example.analyzer.SignalEngine
import com.example.data.db.AppDatabase
import com.example.data.db.SignalEntity
import com.example.data.model.AnalysisResult
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SignalRepository

    init {
        val dao = AppDatabase.getDatabase(application).signalDao()
        repository = SignalRepository(dao)
    }

    val historySignals: StateFlow<List<SignalEntity>> =
        repository.allItemsFlow()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val winCount: StateFlow<Int> =
        repository.winCount
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )

    val lossCount: StateFlow<Int> =
        repository.lossCount
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )

    val totalCount: StateFlow<Int> =
        repository.totalCount
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )

    /*
     * Screen Capture state
     */
    val isCaptureActive: StateFlow<Boolean> =
        ScreenCaptureService.isCapturing

    /*
     * Analysis settings
     */
    private val _isAnalysisPaused =
        MutableStateFlow(false)

    val isAnalysisPaused: StateFlow<Boolean> =
        _isAnalysisPaused.asStateFlow()

    private val _analysisIntervalMs =
        MutableStateFlow(1000L)

    val analysisIntervalMs: StateFlow<Long> =
        _analysisIntervalMs.asStateFlow()

    private val _minConfidenceThreshold =
        MutableStateFlow(65)

    val minConfidenceThreshold: StateFlow<Int> =
        _minConfidenceThreshold.asStateFlow()

    private val _selectedMode =
        MutableStateFlow("QTEX")

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

    /*
     * Current result
     */
    private val _currentResult =
        MutableStateFlow<AnalysisResult?>(null)

    val currentResult: StateFlow<AnalysisResult?> =
        _currentResult.asStateFlow()

    private val _statusMessage =
        MutableStateFlow("Ready — Start Qtex Screen Capture")

    val statusMessage: StateFlow<String> =
        _statusMessage.asStateFlow()

    /*
     * Price history.
     *
     * IMPORTANT:
     * No Random/fake market prices are generated here.
     *
     * Prices are added only when ChartDetector reports
     * a detected price from the captured frame.
     */
    private val priceHistoryMap =
        mutableMapOf<String, MutableList<Double>>()

    private var analysisJob: Job? = null

    private var lastSavedSignalTime = 0L

    /*
     * Supported Forex pairs
     */
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
        /*
         * Create empty history.
         *
         * We deliberately do NOT create fake prices.
         */
        supportedPairs.forEach { pair ->
            priceHistoryMap[pair.symbol] =
                mutableListOf()
        }

        startAnalysisLoop()
    }

    /*
     * Main analysis loop
     */
    private fun startAnalysisLoop() {

        analysisJob?.cancel()

        analysisJob =
            viewModelScope.launch {

                while (true) {

                    delay(_analysisIntervalMs.value)

                    if (_isAnalysisPaused.value) {

                        _statusMessage.value =
                            "Analysis Paused"

                        continue
                    }

                    /*
                     * Qtex mode requires screen capture.
                     */
                    if (!isCaptureActive.value) {

                        _statusMessage.value =
                            "Waiting for Qtex Screen Capture"

                        continue
                    }

                    val pair =
                        _selectedPair.value

                    val timeframe =
                        _selectedTimeframe.value

                    /*
                     * Get latest captured frame.
                     */
                    val bitmap =
                        ScreenCaptureService.latestFrame.value

                    if (bitmap == null) {

                        _statusMessage.value =
                            "Waiting for chart frame..."

                        continue
                    }

                    /*
                     * Analyze the actual captured screen.
                     *
                     * NOTE:
                     * ChartDetector currently performs
                     * visual/pixel analysis.
                     */
                    val frameData =
                        ChartDetector.analyzeFrame(
                            bitmap = bitmap,
                            fallbackPair = pair,
                            fallbackTimeframe = timeframe,
                            lastKnownPrice = 0.0
                        )

                    /*
                     * If chart was not detected,
                     * do not manufacture a price.
                     */
                    if (!frameData.isValidChart) {

                        _statusMessage.value =
                            "WAIT — Qtex chart not detected"

                        continue
                    }

                    /*
                     * Get detected price.
                     */
                    val detectedPrice =
                        frameData.detectedPrice

                    if (detectedPrice == null ||
                        detectedPrice <= 0.0
                    ) {

                        _statusMessage.value =
                            "WAIT — Price not detected from chart"

                        continue
                    }

                    /*
                     * Add only detected price.
                     */
                    val history =
                        priceHistoryMap.getOrPut(pair) {
                            mutableListOf()
                        }

                    history.add(detectedPrice)

                    /*
                     * Keep the latest 200 observations.
                     */
                    if (history.size > 200) {
                        history.removeAt(0)
                    }

                    _statusMessage.value =
                        "Qtex chart detected — Analyzing"

                    /*
                     * Generate technical-analysis signal.
                     */
                    val result =
                        SignalEngine.generateSignal(
                            frameData = frameData,
                            priceHistory = history,
                            minConfidenceThreshold =
                                _minConfidenceThreshold.value,
                            assetName = pair,
                            timeframeName = timeframe
                        )

                    _currentResult.value =
                        result

                    /*
                     * Save UP / DOWN signals.
                     *
                     * Only save once every 12 seconds.
                     */
                    val now =
                        System.currentTimeMillis()

                    if (
                        (
                            result.signalType ==
                                SignalType.UP ||
                            result.signalType ==
                                SignalType.DOWN
                        ) &&
                        now - lastSavedSignalTime > 12000
                    ) {

                        lastSavedSignalTime = now

                        saveSignalToDb(result)
                    }
                }
            }
    }

    /*
     * Save signal to Room database.
     */
    private fun saveSignalToDb(
        result: AnalysisResult
    ) {

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

    /*
     * Pause / Resume analysis
     */
    fun toggleAnalysisPause() {

        _isAnalysisPaused.value =
            !_isAnalysisPaused.value
    }

    /*
     * Change analysis interval.
     */
    fun setAnalysisInterval(ms: Long) {

        _analysisIntervalMs.value =
            ms.coerceIn(250L, 10000L)

        startAnalysisLoop()
    }

    /*
     * Change confidence threshold.
     */
    fun setMinConfidenceThreshold(
        threshold: Int
    ) {

        _minConfidenceThreshold.value =
            threshold.coerceIn(50, 95)
    }

    /*
     * Change mode.
     */
    fun setSelectedMode(
        mode: String
    ) {

        _selectedMode.value =
            mode
    }

    /*
     * Change Forex pair.
     */
    fun setSelectedPair(
        pair: String
    ) {

        _selectedPair.value =
            pair
    }

    /*
     * Change timeframe.
     */
    fun setSelectedTimeframe(
        timeframe: String
    ) {

        _selectedTimeframe.value =
            timeframe
    }

    /*
     * Mark signal result.
     */
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

    /*
     * Delete one signal.
     */
    fun deleteSignal(
        id: Long
    ) {

        viewModelScope.launch {

            repository.deleteSignal(id)
        }
    }

    /*
     * Clear complete signal history.
     */
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

/*
 * Repository Flow bridge.
 */
private fun SignalRepository.allItemsFlow() =
    allSignals
