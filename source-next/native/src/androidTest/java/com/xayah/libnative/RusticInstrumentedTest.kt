package com.xayah.libnative

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

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

        logStep("Restoring snapshot $snapshotId to ${workspace.restorePath}")
        Rustic.restoreSnapshot(workspace.repositoryPath, PASSWORD, snapshotId, workspace.restorePath)

        logStep("Checking repository integrity: ${workspace.repositoryPath}")
        Rustic.checkRepository(workspace.repositoryPath, PASSWORD)

        val restoredFile = workspace.requireRestoredFile(SOURCE_FILE)
        logStep("Verifying restored file content: ${restoredFile.absolutePath}")
        assertEquals(SOURCE_CONTENT, restoredFile.readText())
    }

    private fun createSnapshot(): String {
        logStep("Creating snapshot from source: ${workspace.sourcePath}")
        val snapshotId = Rustic.createSnapshot(
            repositoryPath = workspace.repositoryPath,
            password = PASSWORD,
            sourcePaths = listOf(workspace.sourcePath),
            tags = listOf(SNAPSHOT_TAG),
        )

        assertTrue("Snapshot ID should not be blank", snapshotId.isNotBlank())
        logStep("Created snapshot: $snapshotId")
        return snapshotId
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

        const val TAG = "RusticInstrumentedTest"
        const val PASSWORD = "instrumented-password"
        const val SNAPSHOT_TAG = "instrumented"
        const val SOURCE_FILE = "nested/note.txt"
        const val SOURCE_CONTENT = "Hello from Rust"
    }
}
