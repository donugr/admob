package id.donugr.admob.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class SystemUiHelper {
    private SystemUiHelper() {
    }

    public static void releaseForAdInteraction(Activity activity) {
        if (activity == null) {
            return;
        }

        Window window = activity.getWindow();
        if (window == null) {
            return;
        }

        View decorView = window.getDecorView();
        if (decorView == null) {
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(window, true);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decorView);
        if (controller != null) {
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
            controller.show(WindowInsetsCompat.Type.systemBars());
        }

        int visibility = decorView.getSystemUiVisibility();
        visibility &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        visibility &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        visibility &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
        visibility &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        visibility &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;
        decorView.setSystemUiVisibility(visibility);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        }
    }
}
