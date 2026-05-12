package com.fnostv.android4.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fnostv.android4.media.MediaLibrary;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class NativeSettingsView {
    public interface Listener {
        void onCloseSettings();

        void onEditAccount();

        void onAddLibrary();

        void onEditLibrary(MediaLibrary library);

        void onDeleteLibrary(MediaLibrary library);

        void onScanLibraries();
    }

    private final Context context;
    private final Listener listener;
    private LinearLayout list;
    private TextView statusView;
    private List<MediaLibrary> libraries;

    public NativeSettingsView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create(List<MediaLibrary> libraries, String status) {
        this.libraries = libraries;
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(FnosTheme.COLOR_APP_BG);

        LinearLayout shell = new LinearLayout(context);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        shell.addView(sidebar(), new LinearLayout.LayoutParams(dp(250), ViewGroup.LayoutParams.MATCH_PARENT));
        shell.addView(content(status), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        refresh(libraries, status);
        return root;
    }

    public void refresh(List<MediaLibrary> libraries, String status) {
        this.libraries = libraries;
        if (statusView != null) {
            statusView.setText(status == null ? "" : status);
        }
        if (list == null) {
            return;
        }
        list.removeAllViews();
        if (libraries == null || libraries.size() == 0) {
            TextView empty = muted("暂无媒体库，添加一个目录后即可扫描生成首页内容。", 15);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));
            return;
        }
        for (int i = 0; i < libraries.size(); i++) {
            list.addView(libraryRow(libraries.get(i)), rowParams(0, 12));
        }
    }

    private View sidebar() {
        LinearLayout sidebar = new LinearLayout(context);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setPadding(dp(22), dp(28), dp(18), dp(20));
        sidebar.setBackgroundColor(FnosTheme.COLOR_SIDEBAR);

        TextView title = new TextView(context);
        title.setText("fnOSTV");
        title.setTextColor(FnosTheme.COLOR_TEXT);
        title.setTextSize(22);
        sidebar.addView(title, rowParams(0, 34));

        sidebar.addView(tab("账号连接", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEditAccount();
            }
        }), rowParams(0, 10));
        sidebar.addView(tab("媒体库", true, null), rowParams(0, 10));
        sidebar.addView(tab("外观", false, null), rowParams(0, 10));
        sidebar.addView(tab("服务", false, null), rowParams(0, 10));
        return sidebar;
    }

    private View content(String status) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(40), dp(26), dp(40), dp(28));

        LinearLayout top = new LinearLayout(context);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText("媒体库");
        title.setTextColor(FnosTheme.COLOR_TEXT);
        title.setTextSize(24);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(button("添加媒体库", true, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onAddLibrary();
            }
        }), actionParams());
        top.addView(button("扫描媒体库", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onScanLibraries();
            }
        }), actionParams());
        top.addView(button("返回", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onCloseSettings();
            }
        }), actionParams());
        content.addView(top, rowParams(0, 16));

        statusView = muted(status == null ? "" : status, 14);
        content.addView(statusView, rowParams(0, 16));

        ScrollView scrollView = new ScrollView(context);
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));
        return content;
    }

    private View libraryRow(final MediaLibrary library) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(14), dp(14), dp(14));
        row.setBackgroundDrawable(FnosTheme.stroked(FnosTheme.COLOR_CARD, FnosTheme.COLOR_STROKE, 8, context));
        row.setFocusable(true);
        FocusStyler.applyCard(row);

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(context);
        name.setText(library.name);
        name.setTextColor(FnosTheme.COLOR_TEXT);
        name.setTextSize(18);
        info.addView(name, rowParams(0, 4));
        TextView paths = muted(pathsText(library), 14);
        info.addView(paths, rowParams(0, 4));
        TextView meta = muted(library.categoryLabel() + "  ·  " + updatedAt(library.updatedAt), 13);
        info.addView(meta, rowParams(0, 0));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        row.addView(button("编辑", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEditLibrary(library);
            }
        }), compactActionParams());
        row.addView(button("删除", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteLibrary(library);
            }
        }), compactActionParams());
        return row;
    }

    private View tab(String text, boolean active, View.OnClickListener clickListener) {
        TextView tab = new TextView(context);
        tab.setText(text);
        tab.setTextColor(active ? FnosTheme.COLOR_TEXT : FnosTheme.COLOR_TEXT_MUTED);
        tab.setTextSize(16);
        tab.setGravity(Gravity.CENTER_VERTICAL);
        tab.setPadding(dp(14), 0, dp(14), 0);
        tab.setFocusable(true);
        tab.setClickable(clickListener != null);
        tab.setBackgroundDrawable(FnosTheme.rounded(active ? FnosTheme.COLOR_CARD : FnosTheme.COLOR_SIDEBAR, 6, context));
        if (clickListener != null) {
            tab.setOnClickListener(clickListener);
        }
        return tab;
    }

    private Button button(String text, boolean primary, View.OnClickListener clickListener) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(FnosTheme.COLOR_TEXT);
        button.setMinHeight(dp(42));
        FocusStyler.apply(button, primary ? FnosTheme.COLOR_PRIMARY : FnosTheme.COLOR_CARD, FnosTheme.COLOR_CARD_FOCUSED, 8);
        button.setOnClickListener(clickListener);
        return button;
    }

    private TextView muted(String text, int size) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        view.setTextSize(size);
        return view;
    }

    private String pathsText(MediaLibrary library) {
        if (library.paths.size() == 0) {
            return "未设置目录";
        }
        StringBuilder builder = new StringBuilder();
        int max = Math.min(3, library.paths.size());
        for (int i = 0; i < max; i++) {
            if (builder.length() > 0) {
                builder.append("  /  ");
            }
            builder.append(library.paths.get(i));
        }
        if (library.paths.size() > max) {
            builder.append("  等 ").append(library.paths.size()).append(" 个目录");
        }
        return builder.toString();
    }

    private String updatedAt(long value) {
        if (value <= 0) {
            return "尚未扫描";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(value));
    }

    private LinearLayout.LayoutParams rowParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams actionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(118), dp(44));
        params.leftMargin = dp(10);
        return params;
    }

    private LinearLayout.LayoutParams compactActionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(82), dp(42));
        params.leftMargin = dp(10);
        return params;
    }

    private int dp(int value) {
        return FnosTheme.dp(context, value);
    }
}
