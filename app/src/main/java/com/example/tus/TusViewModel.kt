package com.example.tus

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.tus.android.client.TusAndroidUpload
import io.tus.android.client.TusPreferencesURLStore
import io.tus.java.client.TusClient
import io.tus.java.client.TusUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URL

sealed interface FileUploadUiState {
    data object Standby : FileUploadUiState
    data class Loading(val progress: Float) : FileUploadUiState
    data class Success(val url: URL) : FileUploadUiState
    data class Error(val error: Exception) : FileUploadUiState
}

class TusViewModel(
    sharedPrefsRepository: SharedPrefsRepository
) : ViewModel() {
    var uiState: FileUploadUiState by mutableStateOf(FileUploadUiState.Standby)
    var paused: Boolean = true
        private set

    private val client: TusClient = TusClient().apply {
        uploadCreationURL = URL("https://tusd.tusdemo.net/files")
        enableResuming(TusPreferencesURLStore(sharedPrefsRepository.tus))
    }

    private var fileUri: Uri? = null
    private var uploadJob: Job? = null

    fun startUpload(uri: Uri, context: Context) {
        fileUri = uri
        resumeUpload(context)
    }

    fun resumeUpload(context: Context) {
        paused = false
        try {
            val upload: TusUpload = TusAndroidUpload(fileUri, context)

            uploadJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    val uploader = client.resumeOrCreateUpload(upload)
                    val totalBytes = upload.size
                    var uploadedBytes = uploader.offset

                    // Upload file in 1MiB chunks
                    uploader.chunkSize = 1024 * 1024

                    while (isActive && uploader.uploadChunk() > 0) {
                        uploadedBytes = uploader.offset
                        uiState = FileUploadUiState.Loading(
                            uploadedBytes.toFloat() / totalBytes
                        )
                    }

                    uploader.finish()
                    if (uploadedBytes == totalBytes) {
                        uiState = FileUploadUiState.Success(uploader.uploadURL)
                    }
                } catch (e: Exception) {
                    uiState = FileUploadUiState.Error(e)
                    uploadJob?.cancel()
                }
            }
        } catch (e: Exception) {
            uiState = FileUploadUiState.Error(e)
        }
    }

    fun pauseUpload() {
        uploadJob?.cancel()
        uploadJob = null
        paused = true
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as TusApplication)
                val sharedPrefsRepository = SharedPrefsRepository(application)
                TusViewModel(sharedPrefsRepository)
            }
        }
    }
}
