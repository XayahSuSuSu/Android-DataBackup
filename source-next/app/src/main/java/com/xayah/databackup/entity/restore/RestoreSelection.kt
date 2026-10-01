package com.xayah.databackup.entity.restore

import com.xayah.databackup.entity.backup.BackupSourceCategory

internal fun RestoreState.selectItem(id: String, checked: Boolean): RestoreState {
    return selectItems(setOf(id), checked)
}

internal fun RestoreState.selectCategory(category: RestoreCategory, checked: Boolean): RestoreState =
    selectItems(inventory?.getIds(category).orEmpty(), checked)

internal fun RestoreState.selectAppPart(id: String, part: BackupSourceCategory, checked: Boolean): RestoreState {
    val available = inventory?.availableAppParts?.get(id) ?: return this
    if (part !in available) return this
    val parts = appParts[id].orEmpty()
    val updated = if (checked) parts + part else parts - part
    val selected = if (updated.isEmpty()) selected - id else selected + id
    return copy(selected = selected, appParts = appParts + (id to updated))
}

internal fun RestoreState.selectItems(ids: Set<String>, checked: Boolean): RestoreState {
    val availableIds = ids.intersect(inventory?.allIds.orEmpty())
    val parts = inventory?.apps.orEmpty().keys.intersect(availableIds).associateWith {
        if (checked) inventory?.availableAppParts?.get(it).orEmpty() else emptySet()
    }
    return copy(selected = if (checked) selected + availableIds else selected - availableIds, appParts = appParts + parts)
}

internal fun RestoreState.selectAppParts(ids: Set<String>, parts: Set<BackupSourceCategory>, checked: Boolean): RestoreState {
    val updated = inventory?.apps.orEmpty().keys.intersect(ids).associateWith { id ->
        val available = inventory?.availableAppParts?.get(id).orEmpty().intersect(parts)
        val previous = appParts[id].orEmpty()
        if (checked) previous + available else previous - available
    }
    return copy(
        selected = selected - updated.keys + updated.filterValues { it.isNotEmpty() }.keys,
        appParts = appParts + updated,
    )
}
