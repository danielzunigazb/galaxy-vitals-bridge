package com.danzuniga.vitalsbridge

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VitalsSyncWorker.schedule(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VitalsBridgeScreen(this)
                }
            }
        }
    }
}

@Composable
private fun VitalsBridgeScreen(activity: Activity) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val samsungHealth = remember { SamsungHealthRepository(context) }
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(tokenStore.githubToken ?: "") }
    var permissionsGranted by remember { mutableStateOf<Boolean?>(null) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        permissionsGranted = samsungHealth.hasPermission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Galaxy Vitals Bridge", style = MaterialTheme.typography.headlineSmall)
        Text("Lee heart rate de Samsung Health y lo publica en danzuniga.xyz/status.")

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("GitHub token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            tokenStore.githubToken = token
            status = "Token guardado"
        }) {
            Text("Guardar token")
        }

        HorizontalDivider()

        Text("Samsung Health: ${permissionsGranted?.let { if (it) "permiso OK" else "sin permiso" } ?: "chequeando..."}")
        Button(onClick = {
            scope.launch {
                val granted = samsungHealth.requestPermission(activity)
                permissionsGranted = granted
                status = if (granted) "Permiso de heart rate concedido" else "Permiso denegado"
            }
        }) {
            Text("Pedir permiso de heart rate")
        }

        HorizontalDivider()

        Button(onClick = {
            scope.launch {
                status = "Sincronizando..."
                val snapshot = samsungHealth.readVitalsSnapshot()
                val currentToken = tokenStore.githubToken
                if (currentToken.isNullOrBlank()) {
                    status = "Falta el token"
                    return@launch
                }
                val result = GitHubPublisher(currentToken).publish(snapshot)
                status = if (result.isSuccess) {
                    "Publicado: ${snapshot.heartRateBpm ?: "sin lectura"} bpm, " +
                        "${snapshot.stepsToday ?: 0} pasos, " +
                        "SpO2 ${snapshot.oxygenSaturationPct ?: "-"}%, " +
                        "sueño ${snapshot.sleepDurationMinutes?.let { "${it}min" } ?: "-"}"
                } else {
                    "Error: ${result.exceptionOrNull()?.message}"
                }
            }
        }) {
            Text("Sync ahora")
        }

        Text(status)
    }
}
