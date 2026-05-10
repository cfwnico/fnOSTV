package com.fnostv.android4.ui;

import android.view.View;
import android.widget.TextView;

public final class FocusStyler {
    private FocusStyler() {
    }

    public static void applyNav(final TextView view) {
        apply(view, FnosTheme.COLOR_CARD, FnosTheme.COLOR_PRIMARY, 6);
    }

    public static void applyCard(final View view) {
        apply(view, FnosTheme.COLOR_CARD, FnosTheme.COLOR_CARD_FOCUSED, 8);
    }

    public static void applyButton(final View view) {
        apply(view, FnosTheme.COLOR_CARD, FnosTheme.COLOR_PRIMARY, 8);
    }

    public static void apply(final View view, final int normalColor, final int focusedColor, final int radiusDp) {
        view.setFocusable(true);
        view.setBackgroundDrawable(FnosTheme.rounded(normalColor, radiusDp, view.getContext()));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundDrawable(FnosTheme.rounded(hasFocus ? focusedColor : normalColor, radiusDp, v.getContext()));
            }
        });
    }
}
