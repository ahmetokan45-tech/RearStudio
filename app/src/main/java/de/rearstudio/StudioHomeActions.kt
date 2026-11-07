package de.rearstudio

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp

/** Compose controls share the native scene editor and existing rear renderer. */
fun studioHomeActions(context:Context,editor:()->Unit,designs:()->Unit,elements:()->Unit,settings:()->Unit)=ComposeView(context).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    setContent {
        MaterialTheme(colorScheme=darkColorScheme(primary=Color(StudioScene.load(context).accent),surface=Color(StudioUi.PANEL))) {
            Column(Modifier.fillMaxWidth().padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                val labels=listOf("editor","design_library","elements","settings")
                val actions=listOf(editor,designs,elements,settings)
                for(row in 0..1) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    for(column in 0..1){val index=row*2+column
                        FilledTonalButton(onClick=actions[index],modifier=Modifier.weight(1f).heightIn(min=64.dp)) {
                            Text(StudioUi.text(context,labels[index]))
                        }
                    }
                }
            }
        }
    }
}
