package com.xayah.core.util

import android.annotation.TargetApi
import android.os.Build
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicLong

object FileUtil {
    fun listFilePaths(path: String, listFiles: Boolean = true, listDirs: Boolean = true): List<String> = runCatching {
        File(path).listFiles()!!.filter { (it.isFile && listFiles) || (it.isDirectory && listDirs) }.map { it.path }
    }.getOrElse { listOf() }

    fun calculateSize(path: String): Long = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            calculateSizeApi26(path)
        } else {
            calculateSizeApi24(path)
        }
    }

    private fun calculateSizeApi24(path: String): Long {
        val file = File(path)
        if (!file.exists()) {
            return 0
        }
        var size: Long = 0
        if (file.isFile) {
            size += file.length()
        } else if (file.isDirectory) {
            for (item in file.listFiles()!!) {
                size += calculateSizeApi24(item.absolutePath)
            }
        }
        return size
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun calculateSizeApi26(path: String): Long {
        val size = AtomicLong(0)
        runCatching {
            Files.walkFileTree(Paths.get(path), object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    if (file != null && attrs != null) {
                        size.addAndGet(attrs.size())
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }
            })
        }
        return size.get()
    }

    fun deleteRecursively(path: String): Boolean = runCatching { File(path).deleteRecursively() }.getOrElse { false }

    fun readText(path: String): String = runCatching { File(path).readText() }.getOrElse { "" }
}
