package com.xayah.feature.main.verify

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.core.rootservice.service.RemoteRootService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

data class VerificationStatus(
    val isVerifying: Boolean = false,
    val results: Map<String, String> = emptyMap(),
    val overallResult: Boolean = true
)

@HiltViewModel
class VerifyBackupViewModel @Inject constructor(
    private val rootService: RemoteRootService
) : ViewModel() {
    private val _verificationStatus = MutableStateFlow(VerificationStatus())
    val verificationStatus: StateFlow<VerificationStatus> = _verificationStatus

    fun startVerification(context: Context, storageMode: String, cloudName: String?, backupDir: String) {
        viewModelScope.launch {
            _verificationStatus.value = VerificationStatus(isVerifying = true)
            val results = mutableMapOf<String, String>()
            var overallSuccess = true

            // Determine the actual backup path
            // This logic might need to be more sophisticated depending on cloud storage implementation
            val actualBackupDir = if (storageMode == "Cloud" && cloudName != null) {
                // Placeholder for cloud path resolution - this needs actual implementation
                // For now, assuming backupDir is the relevant path or needs combining with cloudName
                File(context.cacheDir, cloudName) // Example: download to a temporary local cache
                // In a real scenario, you'd list files from the cloud, download the ZIPs, then verify
                // This part is highly dependent on how cloud storage is structured and accessed
                // For simplicity, we'll assume backupDir points to a local copy or accessible path
                // If it's a remote path, files need to be downloaded first.
                // This example will proceed as if backupDir is a local directory containing ZIPs.
                // This part needs significant work for actual cloud support.
                File(backupDir) // Simplified for now
            } else {
                File(backupDir)
            }

            if (!actualBackupDir.exists() || !actualBackupDir.isDirectory) {
                results["Error"] = "Backup directory not found or is not a directory."
                overallSuccess = false
                _verificationStatus.value = VerificationStatus(isVerifying = false, results = results, overallResult = overallSuccess)
                return@launch
            }

            actualBackupDir.listFiles { _, name -> name.endsWith(".zip") }?.forEach { zipFile ->
                try {
                    val filePfd = rootService.openFileForStreaming(zipFile.absolutePath)
                    if (filePfd == null) {
                        results[zipFile.name] = "Error: Could not open ZIP file for reading."
                        overallSuccess = false
                        return@forEach
                    }

                    ParcelFileDescriptor.AutoCloseInputStream(filePfd).use { fis ->
                        ZipInputStream(fis).use { zis ->
                            val storedChecksums = mutableMapOf<String, Long>()
                            var entry = zis.nextEntry
                            while (entry != null) {
                                if (entry.name == "checksums.txt") {
                                    val checksumData = zis.readBytes()
                                    ByteArrayInputStream(checksumData).bufferedReader().forEachLine { line ->
                                        val parts = line.split(":")
                                        if (parts.size == 2) {
                                            storedChecksums[parts[0]] = parts[1].toLongOrNull() ?: 0L
                                        }
                                    }
                                }
                                entry = zis.nextEntry
                            }
                        }
                    }

                    // Re-open for verification pass (ZipInputStream can't be reset easily)
                    val verifyPfd = rootService.openFileForStreaming(zipFile.absolutePath)
                     if (verifyPfd == null) {
                        results[zipFile.name] = "Error: Could not re-open ZIP file for verification."
                        overallSuccess = false
                        return@forEach
                    }
                    ParcelFileDescriptor.AutoCloseInputStream(verifyPfd).use { fisVerify ->
                        ZipInputStream(fisVerify).use { zisVerify ->
                            var entryVerify = zisVerify.nextEntry
                            var allFileChecksPass = true
                            while (entryVerify != null) {
                                if (entryVerify.name != "checksums.txt" && !entryVerify.isDirectory) {
                                    val expectedCrc = storedChecksums[entryVerify.name]
                                    if (expectedCrc == null) {
                                        results["${zipFile.name}/${entryVerify.name}"] = "Missing checksum"
                                        allFileChecksPass = false
                                        overallSuccess = false
                                    } else {
                                        val checkedInputStream = CheckedInputStream(zisVerify, CRC32())
                                        // Drain the stream to calculate CRC
                                        val buffer = ByteArray(8192)
                                        while (checkedInputStream.read(buffer) != -1) { /*
                                            // just read to update checksum
                                        */ }
                                        val actualCrc = checkedInputStream.checksum.value
                                        if (actualCrc != expectedCrc) {
                                            results["${zipFile.name}/${entryVerify.name}"] = "CRC mismatch (Expected: $expectedCrc, Actual: $actualCrc)"
                                            allFileChecksPass = false
                                            overallSuccess = false
                                        } else {
                                           // results["${zipFile.name}/${entryVerify.name}"] = "OK" // Optionally report OK files
                                        }
                                    }
                                }
                                zisVerify.closeEntry() // Important: close entry before getting next
                                entryVerify = zisVerify.nextEntry
                            }
                             if (allFileChecksPass && results[zipFile.name] == null) { // Only mark as OK if no individual file errors
                                results[zipFile.name] = "OK"
                            } else if (!allFileChecksPass && results[zipFile.name] == null) {
                                results[zipFile.name] = "Error: One or more files failed verification."
                            }
                        }
                    }
                } catch (e: Exception) {
                    results[zipFile.name] = "Error: ${e.localizedMessage}"
                    overallSuccess = false
                }
            }

            if (actualBackupDir.listFiles { _, name -> name.endsWith(".zip") }?.isEmpty() == true) {
                results["Info"] = "No ZIP files found in the backup directory."
            }


            _verificationStatus.value = VerificationStatus(isVerifying = false, results = results, overallResult = overallSuccess)
        }
    }
}
