package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.ChipDropdownMenu
import com.xayah.databackup.ui.component.CommonButton
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.RoundedTextField
import com.xayah.databackup.ui.component.emphasizedOffset
import com.xayah.databackup.ui.component.ignorePaddingHorizontal
import com.xayah.databackup.ui.component.material3.spacedByWithFooter
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.librootservice.util.withIOContext
import com.xayah.librootservice.util.withMainContext
import kotlinx.coroutines.launch

@Composable
fun PageCreateAccount(navController: NavHostController) {
    val viewModel = hiltViewModel<CreateAccountViewModel>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val uiState by viewModel.uiState
    val current = uiState.typeList[uiState.typeIndex]
    val currentTextFields = current.textFields

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .paddingHorizontal(CommonTokens.PaddingMedium),
        verticalArrangement = spacedByWithFooter(CommonTokens.PaddingLarge),
    ) {
        item {
            Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
            Row(
                modifier = Modifier
                    .ignorePaddingHorizontal(CommonTokens.PaddingMedium)
                    .horizontalScroll(rememberScrollState())
                    .paddingHorizontal(CommonTokens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
            ) {
                ChipDropdownMenu(
                    leadingIcon = ImageVector.vectorResource(R.drawable.ic_rounded_deployed_code),
                    defaultSelectedIndex = uiState.typeIndex,
                    list = uiState.typeList.map { it.typeDisplay },
                    onSelected = { index, _ ->
                        viewModel.setTypeIndex(index)
                    },
                    onClick = {}
                )
            }
        }

        item {
            var text by current.name
            val emphasizedState by current.nameEmphasizedState
            val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)

            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = emphasizedOffset),
                value = text,
                placeholder = stringResource(R.string.name),
                leadingIcon = ImageVector.vectorResource(R.drawable.ic_rounded_badge),
            ) {
                text = it
            }
        }

        items(items = currentTextFields) { config ->
            var text by config.value
            val emphasizedState by config.emphasizedState
            val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)
            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = emphasizedOffset),
                value = text,
                placeholder = config.placeholder,
                leadingIcon = config.leadingIcon,
            ) {
                text = it
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingBottom(CommonTokens.PaddingMedium),
                horizontalArrangement = Arrangement.End
            ) {
                CommonButton(text = stringResource(R.string.confirm)) {
                    scope.launch {
                        withIOContext {
                            val name by current.name
                            if (name.isEmpty()) {
                                current.nameEmphasizedState.value = current.nameEmphasizedState.value.not()
                            } else {
                                var allFilled = true
                                current.textFields.forEach {
                                    if (it.value.value.isEmpty()) {
                                        it.emphasizedState.value = it.emphasizedState.value.not()
                                        allFilled = false
                                    }
                                }
                                if (allFilled) {
                                    viewModel.create(context = context, dialogState = dialogSlot) {
                                        withMainContext {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
