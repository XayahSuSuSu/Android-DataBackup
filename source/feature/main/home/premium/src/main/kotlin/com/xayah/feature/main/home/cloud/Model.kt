package com.xayah.feature.main.home.cloud

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import com.xayah.core.database.model.CloudEntity
import com.xayah.core.database.model.FTPExtra
import com.xayah.core.database.model.SMBExtra
import com.xayah.core.model.CloudType
import com.xayah.core.model.SmbVersion
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.home.premium.R

internal data class TextFieldConfig(
    val emphasizedState: MutableState<Boolean> = mutableStateOf(false),
    val value: MutableState<String> = mutableStateOf(""),
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    val placeholder: StringResourceToken,
    val leadingIcon: ImageVectorToken,
    val prefix: String? = null,
    val allowEmpty: Boolean = false,
)

internal data class SmbVersionConfig(
    val version: SmbVersion,
    val selected: MutableState<Boolean> = mutableStateOf(true),
)

internal data class TypeConfig(
    val typeDisplay: String,
    val type: CloudType,
    val name: MutableState<String> = mutableStateOf(""),
    val nameEmphasizedState: MutableState<Boolean> = mutableStateOf(false),
    val commonTextFields: List<TextFieldConfig>,
    val extraTextFields: List<TextFieldConfig>,
    val smbVersionConfigs: List<SmbVersionConfig>? = null,
) {
    val smbSelectedList: List<Int>
        get() {
            val selected = mutableListOf<Int>()
            if (smbVersionConfigs != null) {
                for ((index, i) in smbVersionConfigs.withIndex()) {
                    if (i.selected.value) selected.add(index)
                }
            }
            return selected
        }
}

internal object TextFieldConfigTokens {
    fun getUrl(value: String?) = TextFieldConfig(
        value = mutableStateOf(value ?: ""),
        placeholder = StringResourceToken.fromStringId(R.string.url),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_link),
    )

    fun getUsername(value: String?) = TextFieldConfig(
        value = mutableStateOf(value ?: ""),
        placeholder = StringResourceToken.fromStringId(R.string.username),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person),
    )

    fun getPassword(value: String?) = TextFieldConfig(
        value = mutableStateOf(value ?: ""),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        placeholder = StringResourceToken.fromStringId(R.string.password),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_key),
    )

    fun getHost(value: String?, prefix: String?) = TextFieldConfig(
        value = mutableStateOf(value ?: ""),
        prefix = prefix,
        placeholder = StringResourceToken.fromStringId(R.string.url),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_link),
    )

    fun getShare(value: String?) = TextFieldConfig(
        value = mutableStateOf(value ?: ""),
        placeholder = StringResourceToken.fromStringArgs(
            StringResourceToken.fromStringId(R.string.shared_dir),
            StringResourceToken.fromString("("),
            StringResourceToken.fromStringId(R.string.allow_empty),
            StringResourceToken.fromString(")"),
        ),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_folder_open),
        allowEmpty = true
    )

    fun getDomain(value: String?) = TextFieldConfig(
        value = mutableStateOf(value ?: ""),
        placeholder = StringResourceToken.fromStringArgs(
            StringResourceToken.fromStringId(R.string.domain),
            StringResourceToken.fromString("("),
            StringResourceToken.fromStringId(R.string.allow_empty),
            StringResourceToken.fromString(")"),
        ),
        leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Domain),
        allowEmpty = true
    )

    fun getPort(value: Int?, defaultValue: Int) = TextFieldConfig(
        value = mutableStateOf(value?.toString() ?: defaultValue.toString()),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        placeholder = StringResourceToken.fromStringId(R.string.port),
        leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_lan),
    )
}

internal object TypeConfigTokens {
    fun getFTP(entity: CloudEntity?, extra: FTPExtra?) = TypeConfig(
        name = mutableStateOf(entity?.name ?: ""),
        typeDisplay = "FTP",
        type = CloudType.FTP,
        commonTextFields = listOf(
            TextFieldConfigTokens.getHost(entity?.host, "ftp://"),
            TextFieldConfigTokens.getUsername(entity?.user),
            TextFieldConfigTokens.getPassword(entity?.pass),
        ),
        extraTextFields = listOf(
            TextFieldConfigTokens.getPort(extra?.port, 21),
        )
    )

    fun getWebDAV(entity: CloudEntity?) = TypeConfig(
        name = mutableStateOf(entity?.name ?: ""),
        typeDisplay = "WebDAV",
        type = CloudType.WEBDAV,
        commonTextFields = listOf(
            TextFieldConfigTokens.getUrl(entity?.host),
            TextFieldConfigTokens.getUsername(entity?.user),
            TextFieldConfigTokens.getPassword(entity?.pass),
        ),
        extraTextFields = listOf()
    )

    fun getSMB(entity: CloudEntity?, extra: SMBExtra?) = TypeConfig(
        name = mutableStateOf(entity?.name ?: ""),
        typeDisplay = "SMB / CIFS",
        type = CloudType.SMB,
        commonTextFields = listOf(
            TextFieldConfigTokens.getHost(entity?.host, "smb://"),
            TextFieldConfigTokens.getUsername(entity?.user),
            TextFieldConfigTokens.getPassword(entity?.pass),
        ),
        extraTextFields = listOf(
            TextFieldConfigTokens.getShare(extra?.share),
            TextFieldConfigTokens.getPort(extra?.port, 445),
            TextFieldConfigTokens.getDomain(extra?.domain),
        ),
        smbVersionConfigs = listOf(
            SmbVersionConfig(
                selected = mutableStateOf((extra?.version?.getOrNull(0) ?: SmbVersion.SMB_2_0_2) == SmbVersion.SMB_2_0_2),
                version = SmbVersion.SMB_2_0_2
            ),
            SmbVersionConfig(
                selected = mutableStateOf((extra?.version?.getOrNull(0) ?: SmbVersion.SMB_2_1) == SmbVersion.SMB_2_1),
                version = SmbVersion.SMB_2_1
            ),
            SmbVersionConfig(
                selected = mutableStateOf((extra?.version?.getOrNull(0) ?: SmbVersion.SMB_3_0) == SmbVersion.SMB_3_0),
                version = SmbVersion.SMB_3_0
            ),
            SmbVersionConfig(
                selected = mutableStateOf((extra?.version?.getOrNull(0) ?: SmbVersion.SMB_3_0_2) == SmbVersion.SMB_3_0_2),
                version = SmbVersion.SMB_3_0_2
            ),
            SmbVersionConfig(
                selected = mutableStateOf((extra?.version?.getOrNull(0) ?: SmbVersion.SMB_3_1_1) == SmbVersion.SMB_3_1_1),
                version = SmbVersion.SMB_3_1_1
            ),
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
