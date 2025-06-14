package com.gondroid.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Observes a [Flow] of one-time events and handles them safely within a composable.
 *
 * This is useful for events like showing a Toast, navigating to another screen,
 * or displaying a SnackBar—actions that should occur only once and not be repeated
 * after a configuration change (e.g., screen rotation).
 *
 * @param flow The [Flow] of events emitted from the ViewModel.
 * @param key1 An optional key to trigger recomposition when it changes (used with LaunchedEffect).
 * @param key2 An optional second key for recomposition (can be null).
 * @param onEvent A lambda to handle each emitted event.
 *
 * @see androidx.lifecycle.repeatOnLifecycle
 * @see kotlinx.coroutines.Dispatchers.Main.immediate
 *
 * Example usage:
 * ```
 * ObserveAsEvents(viewModel.uiEventFlow) { event ->
 *     when (event) {
 *         is UiEvent.Toast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
 *         is UiEvent.Navigate -> navController.navigate(event.route)
 *     }
 * }
 * ```
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner.lifecycle, key1, key2) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}