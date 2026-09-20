package com.danzuniga.vitalsbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VitalsSyncWorker.schedule(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VitalsBridgeScreen()
                }
            }
        }
    }
}

@Composable
private fun VitalsBridgeScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val healthConnect = remember { HealthConnectRepository(context) }
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(tokenStore.githubToken ?: "") }
    var permissionsGranted by remember { mutableStateOf<Boolean?>(null) }
    var status by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        permissionsGranted = granted.containsAll(healthConnect.permissions)
        status = if (permissionsGranted == true) "Permiso de heart rate concedido" else "Permiso denegado"
    }

    LaunchedEffect(Unit) {
        permissionsGranted = healthConnect.isAvailable() && healthConnect.hasPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Galaxy Vitals Bridge", style = MaterialTheme.typography.headlineSmall)
        Text("Lee heart rate de Health Connect y lo publica en danzuniga.xyz/status.")

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

        Text("Health Connect: ${permissionsGranted?.let { if (it) "permiso OK" else "sin permiso" } ?: "chequeando..."}")
        Button(onClick = { permissionLauncher.launch(healthConnect.permissions) }) {
            Text("Pedir permiso de heart rate")
        }

        HorizontalDivider()

        Button(onClick = {
            scope.launch {
                status = "Sincronizando..."
                val bpm = healthConnect.latestHeartRateBpm()
                val currentToken = tokenStore.githubToken
                if (currentToken.isNullOrBlank()) {
                    status = "Falta el token"
                    return@launch
                }
                val result = GitHubPublisher(currentToken).publish(bpm)
                status = if (result.isSuccess) {
                    "Publicado: ${bpm ?: "sin lectura"} bpm"
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
