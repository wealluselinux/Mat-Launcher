package com.matlauncher.app.util

import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** Compose needs a lifecycle and a saved-state owner on its root view. Services have neither. */
private class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun create() {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    fun resume() { registry.currentState = Lifecycle.State.RESUMED }
    fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
}

/** Puts a Compose UI in a system overlay window (used by the accessibility service). */
class OverlayHost(private val ctx: Context, private val wm: WindowManager) {
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    val showing: Boolean get() = view != null

    fun show(params: WindowManager.LayoutParams, content: @Composable () -> Unit) {
        if (view != null) return
        val o = OverlayOwner()
        o.create()
        val v = ComposeView(ctx).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent(content)
        }
        try {
            wm.addView(v, params)
        } catch (e: Exception) {
            o.destroy()
            return
        }
        o.resume()
        view = v
        owner = o
    }

    fun hide() {
        val v = view ?: return
        owner?.destroy()
        try { wm.removeView(v) } catch (e: Exception) { /* already gone */ }
        try { v.disposeComposition() } catch (e: Exception) { /* ignore */ }
        view = null
        owner = null
    }
}
