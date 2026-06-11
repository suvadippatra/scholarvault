package com.scholarvault.ui.tools.scanner

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.scholarvault.data.repository.ScannedDocumentRepository
import com.scholarvault.ui.viewmodel.ScannerViewModel
import com.scholarvault.ui.viewmodel.ScannerViewModelFactory

@Composable
fun getScannerViewModel(context: Context): ScannerViewModel {
    val app = context.applicationContext as com.scholarvault.MainApplication
    val repo = remember { ScannedDocumentRepository(app.database.scannedDocumentDao()) }
    val factory = ScannerViewModelFactory(repo)
    return androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
}
