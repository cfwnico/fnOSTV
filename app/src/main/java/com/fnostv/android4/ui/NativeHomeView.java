package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class NativeHomeView {
    public interface Listener {
        void onHomeAction(String action);
    }

    public static final String ACTION_HOME = "home";
    public static final String ACTION_FAVORITES = "favorites";
    public static final String ACTION_FILES = "files";
    public static final String ACTION_MEDIA = "media";
    public static final String ACTION_ALL = "all";
    public static final String ACTION_MOVIES = "movies";
    public static final String ACTION_TV = "tv";
    public static final String ACTION_OTHER = "other";
    public static final String ACTION_SEARCH = "search";
    public static final String ACTION_USER = "user";
    public static final String ACTION_SETTINGS = "settings";
    public static final String ACTION_RECENT = "recent";

    private final Context context;
    private final Listener listener;
    private FrameLayout view;
    private View firstNav;
    private TextView favoriteCountView;
    private TextView libraryCountView;
    private TextView allCountView;
    private TextView movieCountView;
    private TextView tvCountView;
    private TextView otherCountView;
    private TextView heroCard;
    private TextView recentCard;

    public NativeHomeView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new FrameLayout(context);
        view.setBackgroundColor(FnosTheme.COLOR_APP_BG);
        view.setVisibility(View.GONE);

        LinearLayout shell = new LinearLayout(context);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        view.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        shell.addView(sidebar(), new LinearLayout.LayoutParams(dp(250), ViewGroup.LayoutParams.MATCH_PARENT));
        shell.addView(content(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return view;
    }

    public View getView() {
        return view;
    }

    public void show() {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        if (firstNav != null) {
            firstNav.requestFocus();
        }
    }

    public void hide() {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public void updateCounts(int favoriteCount, int libraryCount, int allCount, int movieCount, int tvCount, int otherCount) {
        setCount(favoriteCountView, favoriteCount);
        setCount(libraryCountView, libraryCount);
        setCount(allCountView, allCount);
        setCount(movieCountView, movieCount);
        setCount(tvCountView, tvCount);
        setCount(otherCountView, otherCount);
        if (heroCard != null) {
            heroCard.setText("影视大全\n" + libraryCount + " 个媒体库");
        }
        if (recentCard != null) {
            recentCard.setText(allCount == 0 ? "继续观看\n暂无最近播放" : "继续观看\n" + allCount + " 个项目");
        }
    }

    private LinearLayout sidebar() {
        LinearLayout sidebar = new LinearLayout(context);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setPadding(dp(18), dp(26), dp(18), dp(20));
        sidebar.setBackgroundColor(FnosTheme.COLOR_SIDEBAR);

        TextView logo = new TextView(context);
        logo.setText("fnOSTV");
        logo.setTextColor(FnosTheme.COLOR_TEXT);
        logo.setTextSize(20);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        sidebar.addView(logo, navParams(0, 28));

        firstNav = navItem(FnosSidebarIconView.TYPE_HOME, "首页", ACTION_HOME, null);
        sidebar.addView(firstNav, navParams(0, 8));
        favoriteCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_FAVORITE, "收藏", ACTION_FAVORITES, favoriteCountView), navParams(0, 30));

        sidebar.addView(section("媒体库"), navParams(0, 8));
        libraryCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_LIBRARY, "影视大全", ACTION_MEDIA, libraryCountView), navParams(0, 28));

        sidebar.addView(section("分类"), navParams(0, 8));
        allCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_ALL, "全部", ACTION_ALL, allCountView), navParams(0, 8));
        movieCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_MOVIE, "电影", ACTION_MOVIES, movieCountView), navParams(0, 8));
        tvCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_TV, "电视节目", ACTION_TV, tvCountView), navParams(0, 8));
        otherCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_OTHER, "其他", ACTION_OTHER, otherCountView), navParams(0, 8));
        return sidebar;
    }

    private LinearLayout content() {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(42), dp(22), dp(34), dp(30));
        content.setBackgroundColor(FnosTheme.COLOR_APP_BG);

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText("首页");
        title.setTextColor(FnosTheme.COLOR_TEXT);
        title.setTextSize(21);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(iconButton(FnosActionIconButton.TYPE_SEARCH, ACTION_SEARCH), iconParams());
        top.addView(iconButton(FnosActionIconButton.TYPE_USER, ACTION_USER), iconParams());
        top.addView(iconButton(FnosActionIconButton.TYPE_SETTINGS, ACTION_SETTINGS), iconParams());
        content.addView(top, rowParams(0, 28));

        content.addView(sectionTitle("媒体库"), rowParams(0, 12));
        heroCard = mediaCard("影视大全\n1 个媒体库", ACTION_MEDIA, dp(242), dp(136));
        content.addView(heroCard, rowParams(0, 28));

        content.addView(sectionLink("影视大全  ›", ACTION_MEDIA), rowParams(0, 12));
        LinearLayout cards = new LinearLayout(context);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        recentCard = mediaCard("继续观看\n暂无最近播放", ACTION_RECENT, dp(160), dp(235));
        cards.addView(recentCard, new LinearLayout.LayoutParams(dp(160), dp(235)));
        TextView favoriteCard = mediaCard("收藏\n快速访问", ACTION_FAVORITES, dp(160), dp(235));
        LinearLayout.LayoutParams favoriteParams = new LinearLayout.LayoutParams(dp(160), dp(235));
        favoriteParams.leftMargin = dp(16);
        cards.addView(favoriteCard, favoriteParams);
        content.addView(cards, rowParams(0, 0));
        return content;
    }

    private View navItem(String iconType, String label, final String action, TextView countView) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(10), 0);
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackgroundDrawable(FnosTheme.rounded(FnosTheme.COLOR_SIDEBAR, 6, context));
        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundDrawable(FnosTheme.rounded(hasFocus ? FnosTheme.COLOR_CARD : FnosTheme.COLOR_SIDEBAR, 6, context));
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHomeAction(action);
            }
        });

        FnosSidebarIconView iconView = new FnosSidebarIconView(context, iconType);
        row.addView(iconView, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(FnosTheme.COLOR_TEXT);
        text.setTextSize(15);
        text.setPadding(dp(6), 0, 0, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (countView != null) {
            row.addView(countView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return row;
    }

    private TextView section(String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        view.setTextSize(13);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(4), 0, 0, 0);
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(FnosTheme.COLOR_TEXT);
        view.setTextSize(18);
        return view;
    }

    private TextView sectionLink(String text, final String action) {
        TextView view = sectionTitle(text);
        view.setFocusable(true);
        view.setClickable(true);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHomeAction(action);
            }
        });
        return view;
    }

    private TextView mediaCard(String text, final String action, int width, int height) {
        TextView card = new TextView(context);
        card.setText(text);
        card.setTextColor(Color.WHITE);
        card.setTextSize(16);
        card.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(14));
        FocusStyler.applyCard(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHomeAction(action);
            }
        });
        return card;
    }

    private View iconButton(String iconType, final String action) {
        FnosActionIconButton button = new FnosActionIconButton(context, iconType);
        FocusStyler.applyButton(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHomeAction(action);
            }
        });
        return button;
    }

    private TextView countView() {
        TextView view = new TextView(context);
        view.setText("0");
        view.setTextColor(FnosTheme.COLOR_TEXT);
        view.setTextSize(13);
        return view;
    }

    private void setCount(TextView view, int count) {
        if (view != null) {
            view.setText(String.valueOf(Math.max(0, count)));
        }
    }

    private LinearLayout.LayoutParams navParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40));
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams rowParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams iconParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(36), dp(36));
        params.leftMargin = dp(10);
        return params;
    }

    private int dp(int value) {
        return FnosTheme.dp(context, value);
    }
}
