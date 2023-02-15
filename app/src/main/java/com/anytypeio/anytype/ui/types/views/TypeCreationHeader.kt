package com.anytypeio.anytype.ui.types.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_ui.foundation.Dragger
import com.anytypeio.anytype.presentation.types.TypeCreationViewModel
import com.anytypeio.anytype.ui.library.views.list.items.noRippleClickable
import com.anytypeio.anytype.ui.settings.fonts
import com.anytypeio.anytype.ui.settings.typography
import com.anytypeio.anytype.ui.types.views.HeaderDefaults.ColorTextActive
import com.anytypeio.anytype.ui.types.views.HeaderDefaults.ColorTextDisabled

@Composable
fun TypeCreationHeader(
    vm: TypeCreationViewModel,
    nameValid: MutableState<Boolean>,
    inputValue: MutableState<Id>,
) {

    Box(modifier = Modifier.fillMaxWidth()) {
        Dragger(modifier = Modifier.align(Alignment.Center))
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderDefaults.Height)
            .padding(HeaderDefaults.PaddingValues)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.type_creation_new_type),
            style = typography.h3,
        )
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.type_creation_done),
                color = colorResource(id = if (nameValid.value) ColorTextActive else ColorTextDisabled),
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable(enabled = nameValid.value) {
                        vm.createType(inputValue.value)
                    },
                textAlign = TextAlign.End,
                style = HeaderDefaults.TextButtonStyle
            )
        }
    }

}

@Immutable
private object HeaderDefaults {
    val Height = 54.dp
    val PaddingValues = PaddingValues(start = 12.dp, top = 18.dp, end = 16.dp, bottom = 12.dp)
    val TextButtonStyle = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp
    )
    const val ColorTextActive = R.color.text_primary
    const val ColorTextDisabled = R.color.text_secondary
}