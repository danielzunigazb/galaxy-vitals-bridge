package com.danzuniga.vitalsbridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
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

    private val currentIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VitalsSyncWorker.schedule(applicationContext)
        currentIntentState.value = intent

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VitalsBridgeScreen(this, currentIntentState)
                }
            }
        }
    }

    // Spotify's OAuth redirect (galaxyvitalsbridge://callback?code=...) lands
    // here since MainActivity is launchMode="singleTask".
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
    }
}

@Composable
private fun VitalsBridgeScreen(activity: Activity, intentState: MutableState<Intent?>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val samsungHealth = remember { SamsungHealthRepository(context) }
    val spotifyAuth = remember { SpotifyAuth(tokenStore) }
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(tokenStore.githubToken ?: "") }
    var permissionsGranted by remember { mutableStateOf<Boolean?>(null) }
    var spotifyConnected by remember { mutableStateOf(spotifyAuth.isConnected()) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        permissionsGranted = samsungHealth.hasPermission()
    }

    // Consumes the OAuth redirect once, whenever a new one comes in.
    LaunchedEffect(intentState.value) {
        val uri = intentState.value?.data ?: return@LaunchedEffect
        if (uri.scheme == "galaxyvitalsbridge") {
            val code = uri.getQueryParameter("code")
            status = when {
                code != null -> {
                    val result = spotifyAuth.exchangeCode(code)
                    spotifyConnected = result.isSuccess
                    if (result.isSuccess) "Spotify conectado" else "Error conectando Spotify: ${result.exceptionOrNull()?.message}"
                }
                uri.getQueryParameter("error") != null -> "Spotify: ${uri.getQueryParameter("error")}"
                else -> status
            }
        }
        intentState.value = null // consumed, don't reprocess on recomposition
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

        Text("Spotify: ${if (spotifyConnected) "conectado" else "no conectado"}")
        Button(onClick = {
            val url = spotifyAuth.buildAuthUrl()
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }) {
            Text(if (spotifyConnected) "Reconectar Spotify" else "Conectar Spotify")
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
                val nowPlaying = if (spotifyAuth.isConnected()) SpotifyRepository(spotifyAuth).currentlyPlaying() else null
                val result = GitHubPublisher(currentToken).publish(snapshot, nowPlaying)
                status = if (result.isSuccess) {
                    "Publicado: ${snapshot.heartRateBpm ?: "sin lectura"} bpm, " +
                        "${snapshot.stepsToday ?: 0} pasos, " +
                        "SpO2 ${snapshot.oxygenSaturationPct ?: "-"}%, " +
                        "sueño ${snapshot.sleepDurationMinutes?.let { "${it}min" } ?: "-"}" +
                        (nowPlaying?.takeIf { it.isPlaying }?.let { ", sonando: ${it.track}" } ?: "")
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
