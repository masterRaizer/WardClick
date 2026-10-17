package com.wardclick.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding as composePadding

/**
 * Позволяет вызывать padding(innerPadding) без явного Modifier. (используется в NavHost).
 */
internal fun padding(values: PaddingValues): Modifier = Modifier.composePadding(values)
