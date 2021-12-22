package com.xayah.databackup.util

import android.content.Context
import com.topjohnwu.superuser.Shell

class ShellUtil(private val mContext: Context) {
    companion object {
        fun ls(path: String): Boolean {
            return Shell.su("ls -i $path").exec().isSuccess
        }

        fun unzip(filePath: String, out: String) {
            if (mkdir(out))
                Shell.su("unzip $filePath -d $out").exec()
        }

        fun mkdir(path: String): Boolean {
            return Shell.su("mkdir -p $path").exec().isSuccess
        }

        fun rm(path: String): Boolean {
            return Shell.su("rm -rf $path").exec().isSuccess
        }

        fun countLine(path: String): Int {
            var out = 2
            try {
                out = Shell.su("wc -l $path").exec().out.joinToString().split(" ")[0].toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            return out
        }

        fun getAppPackages(appListFilePath: String): MutableList<String> {
            return Shell.su("tail -n +3 $appListFilePath").exec().out
        }

        fun readLine(line: Int, path: String): String {
            return Shell.su("sed -n '${line}p' $path").exec().out.joinToString()
        }

        fun writeLine(line: Int, content: String, path: String): Boolean {
            return Shell.su("sed -i \"${line}c $content\" $path").exec().isSuccess
        }

        fun writeFile(content: String, path: String): Boolean {
            return Shell.su("echo \"$content\" > $path").exec().isSuccess
        }

        fun countSelected(appListFilePath: String): Int {
            val appList = Shell.su("tail -n +3 $appListFilePath").exec().out
            return countLine(appListFilePath) - 2 - appList.joinToString().count { it == '#' }
        }

        fun replace(oldString: String, newString: String, filePath: String): Boolean {
            return Shell.su("sed -i 's/$oldString/$newString/g' $filePath").exec().isSuccess
        }

        fun rename(oldName: String, newName: String, path: String): Boolean {
            return Shell.su("mv $path/$oldName $path/$newName").exec().isSuccess
        }

        fun countFiles(path: String): MutableList<String> {
            val files = Shell.su("ls $path").exec().out
            return files
        }

        fun mv(oldPath: String, newPath: String): Boolean {
            return Shell.su("mv $oldPath $newPath").exec().isSuccess
        }
    }
}