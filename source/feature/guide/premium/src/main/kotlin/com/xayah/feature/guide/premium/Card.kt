package com.xayah.feature.guide.premium

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.IconTextButton
import com.xayah.core.ui.component.shimmer
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString

private fun Modifier.updateCardShimmer(visible: Boolean) = shimmer(visible, 0.5f, 0.3f)

@Composable
fun UpdateCard(shimmering: Boolean, content: String, version: String, link: String, onFailure: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level3), verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
            BodySmallText(modifier = Modifier.updateCardShimmer(shimmering), text = content, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (shimmering) {
                    BodySmallText(
                        modifier = Modifier
                            .updateCardShimmer(true)
                            .height(PaddingTokens.Level4),
                        text = version,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    IconTextButton(
                        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_link),
                        text = StringResourceToken.fromString(version)
                    ) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        }.onFailure {
                            onFailure.invoke()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateCardShimmer() {
    UpdateCard(
        shimmering = true,
        content = "ShimmerShimmerShimmerShimmerShimmer",
        version = "ShimmerShimmer",
        link = "ShimmerShimmer",
        onFailure = {}
    )
}
