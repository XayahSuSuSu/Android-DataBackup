package com.xayah.databackup.ui.activity.guide.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R

data class UpdateItem(
    val version: String,
    val content: String,
    val link: String,
)

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun UpdateCardPreview() {
    UpdateCard(
        version = "Beta-4.3.15",
        content = "1. 添加对KernelSU支持\n2. 取消Rclone缓存\n3. 问题修复与优化",
        link = ""
    )
}

@ExperimentalMaterial3Api
@Composable
fun UpdateCard(
    version: String,
    content: String,
    link: String
) {
    val context = LocalContext.current
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Card(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(Modifier.padding(mediumPadding)) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(nonePadding, mediumPadding, nonePadding, nonePadding),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_link),
                        contentDescription = null,
                        modifier = Modifier.padding(
                            nonePadding,
                            nonePadding,
                            smallPadding,
                            nonePadding
                        ),
                    )
                    Text(
                        text = version,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}