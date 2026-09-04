package com.beenthere.app.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.beenthere.app.data.AppLanguage
import java.util.Locale

/**
 * La lingua e' una scelta manuale dell'utente, non quella di sistema:
 * LocaleManager e' API 33+, e minSdk qui e' 26.
 *
 * Invece di sovrascrivere LocalContext (che romperebbe chi si aspetta il
 * Context dell'Activity, per esempio ModalBottomSheet), si fornisce soltanto
 * un oggetto Resources con il locale scelto. Le traduzioni restano in
 * res/values e res/values-en, e si leggono con [appString].
 */
val LocalAppResources: ProvidableCompositionLocal<Resources> =
    compositionLocalOf { error("LocalAppResources non fornito") }

@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val resources = remember(language, context) {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language.tag))
        }
        context.createConfigurationContext(configuration).resources
    }
    CompositionLocalProvider(LocalAppResources provides resources, content = content)
}

@Composable
@ReadOnlyComposable
fun appString(@StringRes id: Int): String = LocalAppResources.current.getString(id)

@Composable
@ReadOnlyComposable
fun appString(@StringRes id: Int, vararg formatArgs: Any): String =
    LocalAppResources.current.getString(id, *formatArgs)
