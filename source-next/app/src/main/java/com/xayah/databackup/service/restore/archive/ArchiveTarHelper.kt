package com.xayah.databackup.service.restore.archive

import android.system.Os
import android.system.StructStat
import com.xayah.databackup.service.archive.ArchiveStreamHelper
import java.io.ByteArrayOutputStream
import java.io.File

internal enum class ArchiveTarType { Apk, InternalData, ExternalData }

internal class ArchiveTarHelper(
    private val mArchive: File,
    private val mWorkDir: File,
    private val mPackageName: String,
    private val mArchiveType: ArchiveTarType,
) {
    internal data class Entry(val path: String, val kind: Char, val uid: Int, val link: String)

    private var mEntries: List<Entry> = emptyList()
    private var mSourceStat: StructStat? = null
    val sourceUid: Int get() = mEntries.single { it.path.isEmpty() }.uid

    suspend fun validate() {
        mEntries = emptyList()
        mSourceStat = Os.stat(mArchive.path)
        val listing = File(mWorkDir, "entries.list")
        ArchiveStreamHelper.decompressAndProcessTar(
            mArchive,
            mWorkDir,
            listing.path,
            "--list",
            "--verbose",
            "--numeric-owner",
            "--full-time",
            "--quoting-style=c",
        )
        // C quoting keeps filenames containing whitespace, newlines or link delimiters unambiguous.
        // This format is supplied by the bundled GNU tar's simple_print_header in list.c.
        val entries = listing.useLines { lines -> lines.map { parseEntry(it, mPackageName, mArchiveType) }.toList() }
        validateEntries(entries, mArchiveType != ArchiveTarType.Apk)

        // FIFO input cannot seek past payloads: GNU tar's skim_file reads them and rejects truncation.
        // The producer also drains Zstandard to EOF, including its checksum, before validation succeeds.
        validateSource()
        mEntries = entries
    }

    fun validateSource() {
        val validatedStat = checkNotNull(mSourceStat) { "Archive has not been validated" }
        val currentStat = Os.stat(mArchive.path)
        check(
            currentStat.st_dev == validatedStat.st_dev && currentStat.st_ino == validatedStat.st_ino &&
                currentStat.st_size == validatedStat.st_size && currentStat.st_mtime == validatedStat.st_mtime &&
                currentStat.st_ctime == validatedStat.st_ctime
        ) { "Archive changed during restore" }
    }

    suspend fun extract(target: File) {
        check(mEntries.isNotEmpty()) { "Archive has not been validated" }
        validateSource()
        val args = mutableListOf("--extract", "--directory=${target.path}", "--numeric-owner", "--no-overwrite-dir")
        if (mArchiveType != ArchiveTarType.Apk) {
            args.add("--strip-components=1")
        }
        if (mArchiveType == ArchiveTarType.InternalData) {
            args.addAll(listOf("--same-owner", "--same-permissions"))
        } else {
            args.addAll(listOf("--no-same-owner", "--no-same-permissions"))
        }
        // Preserve the platform-created CE/DE and cache directory inodes and metadata.
        // GNU tar strips the package prefix from member names and hard-link targets, but not symlink targets.
        ArchiveStreamHelper.decompressAndProcessTar(mArchive, mWorkDir, "/dev/null", *args.toTypedArray())
    }

    companion object {
        internal fun parseEntry(line: String, packageName: String, archiveType: ArchiveTarType): Entry {
            val nameOffset = line.indexOf('"')
            require(nameOffset > 0) { "Invalid tar listing" }
            val metadata = line.substring(0, nameOffset).trim().split(Regex("\\s+"))
            require(metadata.size == 5) { "Invalid tar metadata" }
            val kind = metadata[0].first()
            require(kind in listOf('-', 'd') || (archiveType == ArchiveTarType.InternalData && kind in listOf('l', 'h'))) {
                "Unsupported archive entry type"
            }
            val owner = metadata[1].split('/')
            require(owner.size == 2 && owner.all { it.toIntOrNull()?.let { uid -> uid >= 0 } == true }) { "Invalid archive owner" }
            val (name, end) = readQuotedName(line, nameOffset)
            val path = relativePath(name, packageName, archiveType)
            require(path.isNotEmpty() || kind == 'd') { "Invalid app data root" }
            if (archiveType == ArchiveTarType.Apk) {
                require(kind == '-' && '/' !in path && path.endsWith(".apk")) { "Invalid APK archive" }
            }
            if (path in listOf("cache", "code_cache")) require(kind == 'd') { "Invalid app cache root" }
            val link = if (kind in listOf('l', 'h')) {
                val offset = line.indexOf('"', end)
                require(offset > end) { "Missing archive link target" }
                val (target, targetEnd) = readQuotedName(line, offset)
                require(targetEnd == line.length) { "Invalid archive link target" }
                if (kind == 'h') relativePath(target, packageName, archiveType) else target
            } else {
                require(end == line.length) { "Invalid tar listing suffix" }
                ""
            }
            return Entry(path, kind, owner[0].toInt(), link)
        }

        private fun readQuotedName(text: String, start: Int): Pair<String, Int> {
            require(text.getOrNull(start) == '"') { "Missing quoted archive name" }
            val bytes = ByteArrayOutputStream()
            var index = start + 1
            while (index < text.length) {
                when (val char = text[index++]) {
                    '"' -> return bytes.toString(Charsets.UTF_8.name()) to index
                    '\\' -> {
                        require(index < text.length) { "Truncated quoted archive name" }
                        val escaped = text[index++]
                        if (escaped in '0'..'7') {
                            var value = escaped - '0'
                            repeat(2) {
                                if (index < text.length && text[index] in '0'..'7') value = value * 8 + (text[index++] - '0')
                            }
                            require(value in 1..255) { "Invalid archive name escape" }
                            bytes.write(value)
                        } else {
                            val value = when (escaped) {
                                'a' -> 7
                                'b' -> 8
                                'f' -> 12
                                'n' -> 10
                                'r' -> 13
                                't' -> 9
                                'v' -> 11
                                '\\', '"', '\'', '?' -> escaped.code
                                else -> error("Invalid archive name escape")
                            }
                            bytes.write(value)
                        }
                    }
                    else -> {
                        val begin = index - 1
                        while (index < text.length && text[index] != '\\' && text[index] != '"') index++
                        bytes.write(text.substring(begin, index).toByteArray(Charsets.UTF_8))
                    }
                }
            }
            error("Unterminated archive name")
        }

        internal fun relativePath(name: String, packageName: String, archiveType: ArchiveTarType): String {
            require(!name.startsWith('/') && '\u0000' !in name) { "Absolute archive path" }
            val parts = name.removeSuffix("/").split('/')
            require(parts.none { it.isEmpty() || it == "." || it == ".." }) { "Invalid archive path" }
            if (archiveType == ArchiveTarType.Apk) {
                return parts.joinToString("/")
            }
            require(parts.first() == packageName) { "Archive does not belong to selected package" }
            return parts.drop(1).joinToString("/")
        }

        internal fun validateEntries(entries: List<Entry>, hasRoot: Boolean) {
            require(entries.isNotEmpty() && entries.map { it.path }.toSet().size == entries.size) { "Empty or duplicate archive entries" }
            val byPath = entries.associateBy { it.path }
            if (hasRoot) require(byPath[""]?.kind == 'd') { "Missing app data root" }
            entries.forEach { entry ->
                var parent = entry.path.substringBeforeLast('/', "")
                while (parent.isNotEmpty()) {
                    require(byPath[parent]?.kind == 'd') { "Missing or invalid archive parent directory" }
                    parent = parent.substringBeforeLast('/', "")
                }
                if (entry.kind == 'h') require(byPath[entry.link]?.kind == '-') { "Invalid archive hard link" }
            }
        }
    }
}
