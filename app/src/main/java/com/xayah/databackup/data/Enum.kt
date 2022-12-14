package com.xayah.databackup.data

enum class AppListType {
    InstalledApp,
    SystemApp,
}

enum class AppListSort {
    AlphabetAscending,
    AlphabetDescending,
    FirstInstallTimeAscending,
    FirstInstallTimeDescending,
}

enum class AppListFilter {
    None,
    Selected,
    NotSelected,
}

enum class AppListSelection {
    None,
    App,
    AppReverse,
    All,
    AllReverse,
}
