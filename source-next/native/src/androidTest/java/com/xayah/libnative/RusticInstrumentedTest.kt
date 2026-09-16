package com.xayah.libnative

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class RusticInstrumentedTest {
    private lateinit var workspace: TestWorkspace

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        workspace = TestWorkspace.createIn(context.filesDir)
        logStep("Created test workspace: ${workspace.root.absolutePath}")
    }

    @After
    fun tearDown() {
        if (::workspace.isInitialized) {
            logStep("Deleting test workspace: ${workspace.root.absolutePath}")
            workspace.delete()
        }
    }

    @Test
    fun repositoryLifecycleCreatesAndRestoresSnapshot() {
        logStep("Writing source file: $SOURCE_FILE")
        workspace.writeSourceFile(SOURCE_FILE, SOURCE_CONTENT)

        logStep("Initializing repository: ${workspace.repositoryPath}")
        Rustic.initRepository(workspace.repositoryPath, PASSWORD)

        val snapshotId = createSnapshot()
        assertTrue(snapshotId.matches(Regex("[0-9a-f]{64}")))

        logStep("Restoring snapshot $snapshotId to ${workspace.restorePath}")
        Rustic.restoreSnapshot(workspace.repositoryPath, PASSWORD, snapshotId, workspace.restorePath)

        logStep("Checking repository integrity: ${workspace.repositoryPath}")
        Rustic.checkRepository(workspace.repositoryPath, PASSWORD)

        val restoredFile = workspace.requireRestoredFile(SOURCE_FILE)
        logStep("Verifying restored file content: ${restoredFile.absolutePath}")
        assertEquals(SOURCE_CONTENT, restoredFile.readText())
        assertEquals("metadata", File(workspace.restore, "$METADATA_DIRECTORY/manifest.json").readText())
        assertEquals("mapped file", File(workspace.restore, "custom/renamed.txt").readText())
        assertFalse(File(workspace.restore, workspace.sourcePath.removePrefix("/") + "/private.txt").exists())
        assertFalse(File(workspace.restore, workspace.sourcePath.removePrefix("/") + "/cache/rustic/config/123").exists())
    }

    @Test
    fun restoresSelectedFileAndDirectoryThroughJni() {
        workspace.writeSourceFile(SOURCE_FILE, SOURCE_CONTENT)
        Rustic.initRepository(workspace.repositoryPath, PASSWORD)
        val snapshotId = createSnapshot()
        val apk = File(workspace.restore, "staging/base.apk")
        Rustic.restoreSnapshot(
            workspace.repositoryPath, PASSWORD, "$snapshotId:custom/renamed.txt", apk.absolutePath,
            Rustic.RestoreOptions(numericId = true, verifyExisting = true),
        )
        assertEquals("mapped file", apk.readText())
        val metadata = File(workspace.restore, "metadata")
        metadata.mkdirs()
        File(metadata, "stale").writeText("remove me")
        Rustic.restoreSnapshot(
            workspace.repositoryPath, PASSWORD, "$snapshotId:$METADATA_DIRECTORY", metadata.absolutePath,
            Rustic.RestoreOptions(delete = true, noOwnership = true, verifyExisting = true),
        )
        assertEquals("metadata", File(metadata, "manifest.json").readText())
        assertEquals(setOf("staging", "metadata"), workspace.restore.list()!!.toSet())
        assertEquals(setOf("manifest.json"), metadata.list()!!.toSet())
    }

    private fun createSnapshot(): String {
        logStep("Creating snapshot from source: ${workspace.sourcePath}")
        val staging = File(workspace.source, "cache/rustic/config/123").apply { mkdirs() }
        File(staging, "manifest.json").writeText("metadata")
        val movedFile = File(workspace.source, "private.txt").apply { writeText("mapped file") }
        val progress = RecordingProgress()
        val snapshotId = Rustic.createSnapshot(
            repositoryPath = workspace.repositoryPath,
            password = PASSWORD,
            sourcePaths = mapOf(
                workspace.sourcePath to workspace.sourcePath,
                staging.absolutePath to METADATA_DIRECTORY,
                movedFile.absolutePath to "custom/renamed.txt",
            ),
            tags = listOf(SNAPSHOT_TAG),
            callback = progress,
        )

        assertTrue("Snapshot ID should not be blank", snapshotId.isNotBlank())
        assertTrue("JNI callback should receive backup progress", progress.count.get() > 0)
        val path = "$METADATA_DIRECTORY/manifest.json"
        val metadata = JSONObject(Rustic.readSnapshotTextFiles(workspace.repositoryPath, PASSWORD, snapshotId, listOf(path)))
        assertEquals("metadata", metadata.getString(path))
        logStep("Created snapshot: $snapshotId")
        return snapshotId
    }

    private class RecordingProgress {
        val count = AtomicInteger()

        fun onProgress(bytesWritten: Long, speed: Long, progress: Float) {
            count.incrementAndGet()
        }
    }

    private fun logStep(message: String) {
        Log.i(TAG, message)
    }

    private class TestWorkspace private constructor(
        val root: File,
        val repository: File,
        val source: File,
        val restore: File,
    ) {
        val repositoryPath: String = repository.absolutePath
        val sourcePath: String = source.absolutePath
        val restorePath: String = restore.absolutePath

        fun writeSourceFile(path: String, content: String) {
            source.resolve(path).apply {
                parentFile?.mkdirs()
                writeText(content)
            }
        }

        fun requireRestoredFile(path: String): File {
            val matches = restore.walkTopDown()
                .filter(File::isFile)
                .filter { it.invariantSeparatorsPath.endsWith("/$path") }
                .toList()

            assertEquals("Expected exactly one restored file ending with $path", 1, matches.size)
            return matches.single()
        }

        fun delete() {
            root.deleteRecursively()
        }

        companion object {
            fun createIn(parent: File): TestWorkspace {
                val root = parent.resolve("rustic-instrumented-${System.nanoTime()}").apply {
                    deleteRecursively()
                    mkdirs()
                }

                return TestWorkspace(
                    root = root,
                    repository = root.resolve("repo"),
                    source = root.resolve("source"),
                    restore = root.resolve("restore"),
                )
            }
        }
    }

    private companion object {
        init {
            System.loadLibrary("rustic")
            Rustic.initLogger()
        }

        const val METADATA_DIRECTORY = ".databackup"
        const val TAG = "RusticInstrumentedTest"
        const val PASSWORD = "instrumented-password"
        const val SNAPSHOT_TAG = "instrumented"
        const val SOURCE_FILE = "nested/note.txt"
        const val SOURCE_CONTENT = "Hello from Rust"
    }
}
