package com.xayah.feature.main.home.cloud

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import com.xayah.core.database.model.CloudEntity
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.home.premium.R

internal data class TextFieldConfig(
    val emphasizedState: MutableState<Boolean> = mutableStateOf(false),
    val key: String,
    val value: MutableState<String> = mutableStateOf(""),
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    val placeholder: StringResourceToken,
    val leadingIcon: ImageVectorToken,
    val prefix: String? = null,
)

internal data class TypeConfig(
    val typeDisplay: String,
    val type: String,
    val name: MutableState<String> = mutableStateOf(""),
    val nameEmphasizedState: MutableState<Boolean> = mutableStateOf(false),
    val fixedArgs: List<String> = listOf(),
    val textFields: List<TextFieldConfig>,
)

internal object TextFieldConfigTokens {
    fun getUrl(value: String?) = TextFieldConfig(
        key = "url",
        value = mutableStateOf(value ?: ""),
        placeholder = StringResourceToken.fromStringId(R.string.url),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_link),
    )

    fun getUsername(value: String?) = TextFieldConfig(
        key = "user",
        value = mutableStateOf(value ?: ""),
        placeholder = StringResourceToken.fromStringId(R.string.username),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person),
    )

    fun getPassword(value: String?) = TextFieldConfig(
        key = "pass",
        value = mutableStateOf(value ?: ""),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        placeholder = StringResourceToken.fromStringId(R.string.password),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_key),
    )

    fun getHost(value: String?, prefix: String?) = TextFieldConfig(
        key = "host",
        value = mutableStateOf(value ?: ""),
        prefix = prefix,
        placeholder = StringResourceToken.fromStringId(R.string.url),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_link),
    )

    fun getPort(value: String?) = TextFieldConfig(
        key = "port",
        value = mutableStateOf(value ?: "21"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        placeholder = StringResourceToken.fromStringId(R.string.port),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_lan),
    )
}

internal object TypeConfigTokens {
    fun getFTP(account: CloudEntity?) = TypeConfig(
        name = mutableStateOf(account?.name ?: ""),
        typeDisplay = "FTP",
        type = "ftp",
        textFields = listOf(
            TextFieldConfigTokens.getHost(account?.account?.host, "ftp://"),
            TextFieldConfigTokens.getPort(account?.account?.port?.toString()),
            TextFieldConfigTokens.getUsername(account?.account?.user),
            TextFieldConfigTokens.getPassword(account?.account?.pass),
        )
    )

    fun getWebDAV(account: CloudEntity?) = TypeConfig(
        name = mutableStateOf(account?.name ?: ""),
        typeDisplay = "WebDAV",
        type = "webdav",
        fixedArgs = listOf("vendor=other"),
        textFields = listOf(
            TextFieldConfigTokens.getUrl(account?.account?.url),
            TextFieldConfigTokens.getUsername(account?.account?.user),
            TextFieldConfigTokens.getPassword(account?.account?.pass),
        )
    )

    fun getSMB(account: CloudEntity?) = TypeConfig(
        name = mutableStateOf(account?.name ?: ""),
        typeDisplay = "SMB / CIFS",
        type = "smb",
        textFields = listOf(
            TextFieldConfigTokens.getHost(account?.account?.host, "smb://"),
            TextFieldConfigTokens.getUsername(account?.account?.user),
            TextFieldConfigTokens.getPassword(account?.account?.pass),
        )
    )
}

internal fun getActionMenuPackagesItem(onClick: suspend () -> Unit) = ActionMenuItem(
    title = StringResourceToken.fromStringId(R.string.app_and_data),
    icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_palette),
    color = ColorSchemeKeyTokens.Primary,
    backgroundColor = ColorSchemeKeyTokens.PrimaryContainer,
    enabled = true,
    secondaryMenu = listOf(),
    onClick = onClick
)

internal fun getActionMenuMediumItem(onClick: suspend () -> Unit) = ActionMenuItem(
    title = StringResourceToken.fromStringId(R.string.media),
    icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_image),
    color = ColorSchemeKeyTokens.Primary,
    backgroundColor = ColorSchemeKeyTokens.PrimaryContainer,
    enabled = true,
    secondaryMenu = listOf(),
    onClick = onClick
)

internal fun getActionMenuTelephonyItem(onClick: suspend () -> Unit) = ActionMenuItem(
    title = StringResourceToken.fromStringId(R.string.telephony),
    icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_call),
    color = ColorSchemeKeyTokens.Primary,
    backgroundColor = ColorSchemeKeyTokens.PrimaryContainer,
    enabled = true,
    secondaryMenu = listOf(),
    onClick = onClick
)
