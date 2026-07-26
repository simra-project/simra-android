package de.tuberlin.mcc.simra.app.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import de.tuberlin.mcc.simra.app.R;

/**
 * Edge-to-edge friendly inset handling for targetSdk 35+/36+: content draws behind the system
 * bars (no letterboxing), while bottom-anchored controls and toolbars are shifted clear of them.
 */
public final class SystemBarInsets {

    private SystemBarInsets() {
    }

    public static void install(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content == null || Boolean.TRUE.equals(content.getTag(R.id.tag_system_bar_insets_installed))) {
            return;
        }
        content.setTag(R.id.tag_system_bar_insets_installed, Boolean.TRUE);

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // tappableElement covers 3-button nav; on some devices larger than systemBars bottom.
            Insets tappable = windowInsets.getInsets(WindowInsetsCompat.Type.tappableElement());
            int bottomInset = Math.max(bars.bottom, tappable.bottom);
            if (v instanceof ViewGroup) {
                applyToHierarchy((ViewGroup) v, bars.top, bottomInset);
            }
            // Pass insets through so NavigationView can still pad its header.
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private static void applyToHierarchy(ViewGroup root, int topInset, int bottomInset) {
        int toolbarSpacePx = root.getResources().getDimensionPixelSize(R.dimen.toolbar_space);
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (shouldOffsetTop(child) || isToolbarClearanceMargin(child, toolbarSpacePx)) {
                addExtraTopMargin(child, topInset);
            }
            if (isBottomAnchored(child)) {
                addExtraBottomMargin(child, bottomInset);
            }
            if (child instanceof ViewGroup) {
                applyToHierarchy((ViewGroup) child, topInset, bottomInset);
            }
        }
    }

    private static boolean shouldOffsetTop(View view) {
        if (view.getId() == R.id.top_bar) {
            return true;
        }
        // Toolbar included as a direct screen chrome (not already under top_bar).
        return view instanceof Toolbar && !hasAncestorWithId(view, R.id.top_bar);
    }

    /**
     * Screens place content under the floating toolbar with a fixed {@code toolbar_space}
     * top margin. When the toolbar moves down for the status bar, that content must move too.
     */
    private static boolean isToolbarClearanceMargin(View view, int toolbarSpacePx) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (!(lp instanceof ViewGroup.MarginLayoutParams)) {
            return false;
        }
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
        Object baseTag = view.getTag(R.id.tag_base_top_margin);
        int baseMargin = baseTag instanceof Integer ? (Integer) baseTag : mlp.topMargin;
        return baseMargin == toolbarSpacePx;
    }

    private static boolean hasAncestorWithId(View view, int ancestorId) {
        Object parent = view.getParent();
        while (parent instanceof View) {
            View parentView = (View) parent;
            if (parentView.getId() == ancestorId) {
                return true;
            }
            parent = parentView.getParent();
        }
        return false;
    }

    private static boolean isBottomAnchored(View view) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof RelativeLayout.LayoutParams) {
            int[] rules = ((RelativeLayout.LayoutParams) lp).getRules();
            return rules[RelativeLayout.ALIGN_PARENT_BOTTOM] == RelativeLayout.TRUE;
        }
        if (lp instanceof CoordinatorLayout.LayoutParams) {
            return isBottomGravity(((CoordinatorLayout.LayoutParams) lp).gravity);
        }
        if (lp instanceof FrameLayout.LayoutParams) {
            return isBottomGravity(((FrameLayout.LayoutParams) lp).gravity);
        }
        return false;
    }

    private static boolean isBottomGravity(int gravity) {
        if (gravity == Gravity.NO_GRAVITY) {
            return false;
        }
        int vertical = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        return vertical == Gravity.BOTTOM;
    }

    private static void addExtraTopMargin(View view, int extraTop) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (!(lp instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
        int base = readBase(view, R.id.tag_base_top_margin, mlp.topMargin);
        int target = base + extraTop;
        if (mlp.topMargin != target) {
            mlp.topMargin = target;
            view.setLayoutParams(mlp);
        }
    }

    private static void addExtraBottomMargin(View view, int extraBottom) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (!(lp instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
        int base = readBase(view, R.id.tag_base_bottom_margin, mlp.bottomMargin);
        int target = base + extraBottom;
        if (mlp.bottomMargin != target) {
            mlp.bottomMargin = target;
            view.setLayoutParams(mlp);
        }
    }

    private static int readBase(View view, int tagId, int current) {
        Object base = view.getTag(tagId);
        if (base instanceof Integer) {
            return (Integer) base;
        }
        view.setTag(tagId, current);
        return current;
    }
}
