package com.example.tus

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TusApp(
    viewModel: TusViewModel = viewModel(factory = TusViewModel.Factory)
) {
    val context = LocalContext.current

    val activityResult = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.startUpload(uri, context)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    val uiState = viewModel.uiState

    Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(contentPadding)
                .padding(8.dp)
                .fillMaxSize()
        ) {
            when (uiState) {
                is FileUploadUiState.Standby -> {
                    Text(
                        text = stringResource(R.string.welcome),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(16.dp))
                }
                is FileUploadUiState.Loading -> {
                    Text(stringResource(R.string.uploading_in_progress))
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text((uiState.progress * 100).toInt().toString() + '%')
                    Spacer(Modifier.height(16.dp))
                    if (viewModel.paused) {
                        Button(
                            onClick = { viewModel.resumeUpload(context) }
                        ) {
                            Text(stringResource(R.string.continue_uploading))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.pauseUpload() }
                        ) {
                            Text(stringResource(R.string.pause_uploading))
                        }
                    }
                }
                is FileUploadUiState.Success -> {
                    SelectionContainer {
                        Text(
                            text = uiState.url.toString(),
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                val webpage: Uri = uiState.url.toString().toUri()
                                val intent = Intent(Intent.ACTION_VIEW, webpage)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
                is FileUploadUiState.Error -> {
                    Text(stringResource(R.string.error, uiState.error))
                }
            }
            Button(
                onClick = {
                    activityResult.launch(
                        PickVisualMediaRequest(PickVisualMedia.VideoOnly)
                    )
                }
            ) {
                Text(stringResource(R.string.choose_file))
            }
        }
    }
}
