package com.weclimb.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun PermissionSettingsCardUi(openSettings: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .semantics { testTag = "screen-permission-settings" },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .1f)),
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .16f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("▣", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Text(
                    "카메라 권한이 필요해요",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "클라이밍 영상을 찍으려면 카메라·마이크 접근을 허용해야 해요. 권한을 거부해서 지금은 촬영할 수 없어요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        "설정  ›  권한  ›  카메라  ›  허용",
                        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = openSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .semantics { testTag = "cta-open-permission-settings" },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("설정 열기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
