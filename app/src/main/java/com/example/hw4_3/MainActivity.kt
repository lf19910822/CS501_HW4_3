package com.example.hw4_3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hw4_3.ui.theme.HW4_3Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TemperatureReading(
    val timestamp: String,
    val temperature: Float
)

class TemperatureViewModel : ViewModel() {
    private val _readings = MutableStateFlow<List<TemperatureReading>>(emptyList())
    val readings: StateFlow<List<TemperatureReading>> = _readings.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        startTemperatureSimulation()
    }

    private fun startTemperatureSimulation() {
        viewModelScope.launch {
            while (true) {
                if (!_isPaused.value) {
                    val temperature = (65..85).random() + Math.random().toFloat()
                    val timestamp = dateFormat.format(Date())
                    val newReading = TemperatureReading(timestamp, temperature)

                    val currentReadings = _readings.value.toMutableList()
                    currentReadings.add(0, newReading)
                    if (currentReadings.size > 20) {
                        currentReadings.removeAt(currentReadings.size - 1)
                    }
                    _readings.value = currentReadings
                }
                delay(2000)
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HW4_3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TemperatureDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TemperatureDashboard(
    modifier: Modifier = Modifier
) {
    val viewModel = remember { TemperatureViewModel() }
    val readings by viewModel.readings.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()

    val current = readings.firstOrNull()?.temperature ?: 0f
    val average = if (readings.isNotEmpty()) readings.map { it.temperature }.average().toFloat() else 0f
    val min = readings.minOfOrNull { it.temperature } ?: 0f
    val max = readings.maxOfOrNull { it.temperature } ?: 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Temperature Dashboard",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { viewModel.togglePause() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isPaused) "Resume" else "Pause")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard("Current", String.format("%.1f°F", current))
            StatCard("Average", String.format("%.1f°F", average))
            StatCard("Min", String.format("%.1f°F", min))
            StatCard("Max", String.format("%.1f°F", max))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (readings.isNotEmpty()) {
            TemperatureChart(
                readings = readings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Readings:",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(readings) { reading ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = reading.timestamp)
                        Text(text = String.format("%.1f°F", reading.temperature))
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, fontSize = 12.sp)
            Text(text = value, fontSize = 14.sp)
        }
    }
}

@Composable
fun TemperatureChart(readings: List<TemperatureReading>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color(0xFFF5F5F5))) {
        if (readings.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 40f

        val temps = readings.reversed().map { it.temperature }
        val minTemp = temps.minOrNull() ?: 65f
        val maxTemp = temps.maxOrNull() ?: 85f
        val tempRange = maxTemp - minTemp

        if (tempRange == 0f || temps.size < 2) return@Canvas

        val stepX = (width - 2 * padding) / (temps.size - 1)

        val path = Path()
        temps.forEachIndexed { index, temp ->
            val x = padding + index * stepX
            val y = height - padding - ((temp - minTemp) / tempRange) * (height - 2 * padding)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            drawCircle(
                color = Color.Blue,
                radius = 4f,
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = Color.Blue,
            style = Stroke(width = 3f)
        )
    }
}