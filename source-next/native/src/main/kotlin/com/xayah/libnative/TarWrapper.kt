package com.xayah.libnative

object TarWrapper {
    external fun callCli(stdOut: String, stdErr: String, argv: Array<String>): Int
}
