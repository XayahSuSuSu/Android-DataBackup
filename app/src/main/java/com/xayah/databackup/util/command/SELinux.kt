package com.xayah.databackup.util.command

import com.xayah.databackup.data.DataType
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.util.joinToLineString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SELinux {
    companion object {
        private const val QUOTE = '"'
        private const val USD = '$'

        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        suspend fun getContext(path: String): Pair<Boolean, String> {
            val exec = Command.execute("ls -Zd $QUOTE$path$QUOTE | awk 'NF>1{print ${USD}1}'; ls -Zd $QUOTE$path$QUOTE > /dev/null 2>&1")
            return Pair(exec.isSuccess, if (exec.isSuccess) exec.out.joinToLineString.trim() else "")
        }

        suspend fun setOwnerAndContext(dataType: DataType, packageName: String, path: String, userId: String, supportFixContext: Boolean, context: String): Pair<Boolean, String> {
            var isSuccess = true
            var out = ""

            runOnIO {
                val uid = RootService.getInstance().getPackageUid(packageName, userId.toInt())
                if (uid != -1) {
                    Command.execute("chown -hR $QUOTE$uid:$uid$QUOTE $QUOTE$path/$QUOTE").apply {
                        if (this.isSuccess.not()) isSuccess = false
                        out += this.out.joinToLineString + "\n"
                    }
                    if (supportFixContext) {
                        if (context.isNotEmpty()) {
                            Command.execute("chcon -hR $QUOTE$context$QUOTE $QUOTE$path/$QUOTE").apply {
                                if (this.isSuccess.not()) isSuccess = false
                                out += this.out.joinToLineString + "\n"
                            }
                        } else {
                            Command.execute("ls -Zd $QUOTE$path/../$QUOTE | awk 'NF>1{print ${USD}1}' | sed -e ${QUOTE}s/system_data_file/app_data_file/g$QUOTE; ls -Zd $QUOTE$path/../$QUOTE > /dev/null 2>&1").apply {
                                if (this.isSuccess.not()) {
                                    isSuccess = false
                                    out += this.out.joinToLineString + "\n"
                                    return@runOnIO
                                }
                                Command.execute("chcon -hR $QUOTE${this.out.joinToLineString}$QUOTE $QUOTE$path/$QUOTE").apply {
                                    if (this.isSuccess.not()) isSuccess = false
                                    out += this.out.joinToLineString + "\n"
                                }
                            }
                        }
                    }
                } else {
                    isSuccess = false
                    out = "Failed to get uid of $packageName."
                    return@runOnIO
                }
            }
            return Pair(isSuccess, out.trim())
        }
    }
}
