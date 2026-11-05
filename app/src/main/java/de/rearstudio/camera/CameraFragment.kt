package de.rearstudio.camera

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import de.rearstudio.R
import de.rearstudio.core.BackScreen
import de.rearstudio.core.KeeperService
import de.rearstudio.core.LendReason
import de.rearstudio.core.RearCommands
import de.rearstudio.core.RearOwner
import de.rearstudio.core.RearState
import de.rearstudio.rear.RearHostActivity
import de.rearstudio.ui.AskNotificationsOnce
import de.rearstudio.ui.MainSwitchBar
import de.rearstudio.ui.Refreshable
import de.rearstudio.ui.needShizuku
import de.rearstudio.ui.shizukuMissing
import de.rearstudio.ui.snackbar
import de.rearstudio.wallpaper.PreviewBackdrop
import de.rearstudio.wallpaper.WallpaperSettings
import com.google.android.material.snackbar.Snackbar

/**
 * The Camera tab: the switch for swipe left, a preview of the swipe over your wallpaper, Open
 * camera on back screen (Close camera while it's up), and a note while the wallpaper is off; the
 * ways out are in its help. There are no options. Open camera works while the switch is off,
 * like Battery's Play.
 */
class CameraFragment : Fragment(R.layout.fragment_camera), Refreshable {

    private lateinit var mainSwitch: MainSwitchBar
    private lateinit var backdrop: PreviewBackdrop
    private lateinit var hint: SwipeHint
    private lateinit var openButton: Button
    private lateinit var wallpaperCard: View

    private val ctx: Context get() = requireContext()

    // Refusing leaves this tab and Xiaomi's back strip to close the camera.
    private val askNotifications = AskNotificationsOnce(this, CameraSettings, R.string.camera_notifications_denied)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainSwitch = view.findViewById(R.id.mainSwitch)
        hint = view.findViewById(R.id.swipeHint)
        openButton = view.findViewById(R.id.openButton)
        wallpaperCard = view.findViewById(R.id.wallpaperCard)
        // Nothing is drawn in the clock's colours here, so there's nothing to match.
        backdrop = PreviewBackdrop(
            view.findViewById(R.id.previewFrame), view.findViewById(R.id.preview), view.findViewById(R.id.clock), {},
            view.findViewById(R.id.glow),
        )

        mainSwitch.onCheckedChange = ::switched
        openButton.setOnClickListener { openOrClose() }
        view.findViewById<Button>(R.id.wallpaperButton).setOnClickListener { turnOnWallpaper() }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) reloadPreview()
        hint.playing = !isHidden
    }

    override fun onPause() {
        super.onPause()
        hint.playing = false
    }

    /** Another tab is showing, or this one again. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        hint.playing = !hidden && isResumed
        if (!hidden) reloadPreview()
    }

    override fun onDestroyView() {
        backdrop.release()
        super.onDestroyView()
    }

    override fun refresh() {
        if (view == null) return
        val on = CameraSettings.isEnabled(ctx)
        val wallpaperOn = WallpaperSettings.isEnabled(ctx)
        val open = cameraIsUp()
        mainSwitch.isChecked = on
        mainSwitch.setSummary(
            when {
                !on -> R.string.state_off
                open -> R.string.camera_on_open
                !wallpaperOn -> R.string.camera_on_no_wallpaper
                shizukuMissing -> R.string.state_waiting_shizuku
                else -> R.string.camera_on_ready
            }
        )
        openButton.setText(if (open) R.string.close_camera else R.string.open_camera)
        wallpaperCard.visibility = if (wallpaperOn) View.GONE else View.VISIBLE
        backdrop.refresh()
    }

    private fun cameraIsUp() = RearState.lendFor(ctx, LendReason.CAMERA) != null

    private fun switched(on: Boolean) {
        CameraSettings.setEnabled(ctx, on)
        BackScreen.log(if (on) "Swipe left for Xiaomi Camera on" else "Swipe left for Xiaomi Camera off")
        // The back screen reads swipes only while a section uses them; nothing new to see there.
        RearHostActivity.swipesChanged()
        if (on) askNotifications.ask()
        refresh()
    }

    /** Opens Xiaomi Camera on the back screen, or closes it if it's up. */
    private fun openOrClose() {
        if (cameraIsUp()) {
            KeeperService.start(ctx, KeeperService.ACTION_CLOSE_CAMERA, why = "the Camera tab")
            return
        }
        when {
            !RearCommands.isReady() -> needShizuku()
            RearState.owner(ctx) is RearOwner.Lent -> snackbar(R.string.play_lent, Snackbar.LENGTH_LONG)
            else -> {
                KeeperService.start(ctx, KeeperService.ACTION_OPEN_CAMERA, why = "the Camera tab")
                // Off, it still opens from here, but a swipe won't.
                if (!CameraSettings.isEnabled(ctx)) snackbar(R.string.camera_opened_switch_off, Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun turnOnWallpaper() {
        if (!RearCommands.isReady()) return needShizuku()
        BackScreen.log("Wallpaper on, from the Camera tab")
        KeeperService.setWallpaper(ctx, true)
        refresh()
    }

    /** Shown again: the Wallpaper tab may have changed what's under it. */
    private fun reloadPreview() {
        backdrop.reload()
        refresh()
    }
}
