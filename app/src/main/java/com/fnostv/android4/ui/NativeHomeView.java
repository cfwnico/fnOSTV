package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

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
    public static final String ACTION_USER_PASSWORD = "settings-password";
    public static final String ACTION_USER_PREFERENCE = "settings-preference";
    public static final String ACTION_USER_APPEARANCE = "settings-appearance";
    public static final String ACTION_HELP = "help";
    public static final String ACTION_ABOUT = "about";
    public static final String ACTION_LOGOUT = "logout";

    private final Context context;
    private final Listener listener;
    private final PosterLoader posterLoader = new PosterLoader();
    private FrameLayout view;
    private View firstNav;
    private LinearLayout userMenuView;
    private TextView favoriteCountView;
    private TextView libraryCountView;
    private TextView allCountView;
    private TextView movieCountView;
    private TextView tvCountView;
    private TextView otherCountView;
    private HomeMediaCard heroCard;
    private HomeMediaCard recentCard;
    private HomeMediaCard favoriteCard;
    private LinearLayout posterSectionContainer;
    private List<HomePosterSection> posterSections = new ArrayList<HomePosterSection>();
    private String posterBaseUrl = "";
    private String username = "--";
    private String sourceName = "--";
    private boolean admin;

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
        userMenuView = userMenu();
        userMenuView.setVisibility(View.GONE);
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(dp(208), dp(320), Gravity.TOP | Gravity.RIGHT);
        menuParams.topMargin = dp(62);
        menuParams.rightMargin = dp(90);
        view.addView(userMenuView, menuParams);
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

    public void updateCounts(int favoriteCount, int libraryCount, int allCount, int movieCount, int tvCount, int otherCount, int recentCount) {
        setCount(favoriteCountView, favoriteCount);
        setCount(libraryCountView, libraryCount);
        setCount(allCountView, allCount);
        setCount(movieCountView, movieCount);
        setCount(tvCountView, tvCount);
        setCount(otherCountView, otherCount);
        if (heroCard != null) {
            heroCard.setSummary("影视大全\n" + libraryCount + " 个媒体库");
        }
        if (recentCard != null) {
            recentCard.setSummary(recentCount == 0 ? "继续观看\n暂无最近播放" : "继续观看\n" + recentCount + " 个项目");
        }
        if (favoriteCard != null) {
            favoriteCard.setSummary(favoriteCount == 0 ? "收藏\n快速访问" : "收藏\n" + favoriteCount + " 个项目");
        }
    }

    public void setPosterBaseUrl(String baseUrl) {
        posterBaseUrl = baseUrl == null ? "" : baseUrl;
        refreshPosterCards();
    }

    public void setPosterAuthorizationToken(String token) {
        posterLoader.setAuthorizationToken(token);
        refreshPosterCards();
    }

    public void updatePosterCards(HomePosterSlots slots) {
        if (heroCard != null) {
            heroCard.setEntry(slots == null ? null : slots.media);
        }
        if (recentCard != null) {
            recentCard.setEntry(slots == null ? null : slots.recent);
        }
        if (favoriteCard != null) {
            favoriteCard.setEntry(slots == null ? null : slots.favorite);
        }
    }

    public void updatePosterSections(List<HomePosterSection> sections) {
        posterSections = sections == null ? new ArrayList<HomePosterSection>() : new ArrayList<HomePosterSection>(sections);
        renderPosterSections();
    }

    public void updateUser(String username, String sourceName, boolean admin) {
        this.username = emptyToDefault(username, "--");
        this.sourceName = emptyToDefault(sourceName, this.username);
        this.admin = admin;
        refreshUserMenu();
    }

    public void hideUserMenu() {
        if (userMenuView != null) {
            userMenuView.setVisibility(View.GONE);
        }
    }

    private View sidebar() {
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(FnosTheme.COLOR_SIDEBAR);

        LinearLayout sidebar = new LinearLayout(context);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setPadding(dp(18), dp(HomeSidebarLayout.PADDING_TOP), dp(18), dp(HomeSidebarLayout.PADDING_BOTTOM));
        sidebar.setBackgroundColor(FnosTheme.COLOR_SIDEBAR);

        LinearLayout brand = new LinearLayout(context);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.addView(new FnosLogoMarkView(context), new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView logo = new TextView(context);
        logo.setText("fnOSTV");
        logo.setTextColor(FnosTheme.COLOR_TEXT);
        logo.setTextSize(18);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.setPadding(dp(8), 0, 0, 0);
        brand.addView(logo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));
        sidebar.addView(brand, navParams(0, HomeSidebarLayout.BRAND_BOTTOM));

        firstNav = navItem(FnosSidebarIconView.TYPE_HOME, "首页", ACTION_HOME, null);
        sidebar.addView(firstNav, navParams(0, HomeSidebarLayout.ROW_BOTTOM));
        favoriteCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_FAVORITE, "收藏", ACTION_FAVORITES, favoriteCountView), navParams(0, HomeSidebarLayout.GROUP_BOTTOM));

        sidebar.addView(section("媒体库"), sectionParams(0, HomeSidebarLayout.ROW_BOTTOM));
        libraryCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_LIBRARY, "影视大全", ACTION_MEDIA, libraryCountView), navParams(0, HomeSidebarLayout.GROUP_BOTTOM));

        sidebar.addView(section("分类"), sectionParams(0, HomeSidebarLayout.ROW_BOTTOM));
        allCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_ALL, "全部", ACTION_ALL, allCountView), navParams(0, HomeSidebarLayout.ROW_BOTTOM));
        movieCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_MOVIE, "电影", ACTION_MOVIES, movieCountView), navParams(0, HomeSidebarLayout.ROW_BOTTOM));
        tvCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_TV, "电视节目", ACTION_TV, tvCountView), navParams(0, HomeSidebarLayout.ROW_BOTTOM));
        otherCountView = countView();
        sidebar.addView(navItem(FnosSidebarIconView.TYPE_OTHER, "其他", ACTION_OTHER, otherCountView), navParams(0, 0));

        scroll.addView(sidebar, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private LinearLayout content() {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(42), dp(24), dp(34), dp(30));
        content.setBackgroundColor(FnosTheme.COLOR_APP_BG);

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText("首页");
        title.setTextColor(FnosTheme.COLOR_TEXT);
        title.setTextSize(22);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(iconButton(FnosActionIconButton.TYPE_SEARCH, ACTION_SEARCH), iconParams());
        top.addView(iconButton(FnosActionIconButton.TYPE_USER, ACTION_USER), iconParams());
        top.addView(iconButton(FnosActionIconButton.TYPE_SETTINGS, ACTION_SETTINGS), iconParams());
        content.addView(top, rowParams(0, 30));

        ScrollView scroll = new ScrollView(context);
        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        body.addView(sectionTitle("媒体库"), rowParams(0, 12));
        heroCard = mediaCard("影视大全\n1 个媒体库", ACTION_MEDIA, dp(390), dp(76), true);
        body.addView(heroCard, rowHeightParams(0, 28, 112));

        posterSectionContainer = new LinearLayout(context);
        posterSectionContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(posterSectionContainer, rowParams(0, 0));
        renderPosterSections();
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
                hideUserMenu();
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

    private HomeMediaCard mediaCard(String text, final String action, int width, int height, boolean wide) {
        HomeMediaCard card = new HomeMediaCard(context, text, wide);
        card.setMinimumWidth(width);
        card.setMinimumHeight(height);
        FocusStyler.applyCard(card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHomeAction(action);
            }
        });
        return card;
    }

    private void refreshPosterCards() {
        if (heroCard != null) {
            heroCard.reloadPoster();
        }
        if (recentCard != null) {
            recentCard.reloadPoster();
        }
        if (favoriteCard != null) {
            favoriteCard.reloadPoster();
        }
        renderPosterSections();
    }

    private void renderPosterSections() {
        if (posterSectionContainer == null) {
            return;
        }
        posterSectionContainer.removeAllViews();
        if (posterSections.size() == 0) {
            posterSectionContainer.addView(sectionLink("影视大全  ›", ACTION_MEDIA), rowParams(0, 12));
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(mediaCard("影视大全\n浏览媒体库", ACTION_MEDIA, dp(160), dp(235), false),
                    new LinearLayout.LayoutParams(dp(160), dp(235)));
            posterSectionContainer.addView(row, rowParams(0, 0));
            return;
        }
        for (int i = 0; i < posterSections.size(); i++) {
            HomePosterSection section = posterSections.get(i);
            posterSectionContainer.addView(sectionLink(section.title + "  ›", section.action), rowParams(i == 0 ? 0 : 18, 12));
            posterSectionContainer.addView(sectionRow(section), rowParams(0, 0));
        }
    }

    private View sectionRow(HomePosterSection section) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        List<FnosFileEntry> entries = section.visibleEntries();
        if (entries.size() == 0) {
            row.addView(mediaCard(section.title + "\n暂无内容", section.action, dp(160), dp(235), false),
                    new LinearLayout.LayoutParams(dp(160), dp(235)));
            return row;
        }
        for (int i = 0; i < entries.size(); i++) {
            HomeMediaCard card = mediaCard(entries.get(i).name, section.action, dp(160), dp(235), false);
            card.setEntry(entries.get(i));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(160), dp(235));
            if (i > 0) {
                params.leftMargin = dp(16);
            }
            row.addView(card, params);
        }
        if (section.hasMore) {
            LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(120), dp(235));
            moreParams.leftMargin = dp(16);
            row.addView(mediaCard("更多\n" + section.title, section.action, dp(120), dp(235), false), moreParams);
        }
        return row;
    }

    private View iconButton(String iconType, final String action) {
        FnosActionIconButton button = new FnosActionIconButton(context, iconType);
        FocusStyler.applyButton(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ACTION_USER.equals(action)) {
                    toggleUserMenu();
                } else {
                    hideUserMenu();
                    listener.onHomeAction(action);
                }
            }
        });
        return button;
    }

    private LinearLayout userMenu() {
        LinearLayout menu = new LinearLayout(context);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(0, dp(12), 0, dp(10));
        menu.setBackgroundDrawable(FnosTheme.stroked(FnosTheme.COLOR_CARD, FnosTheme.COLOR_STROKE, 8, context));
        refreshUserMenu(menu);
        return menu;
    }

    private void refreshUserMenu() {
        if (userMenuView != null) {
            refreshUserMenu(userMenuView);
        }
    }

    private void refreshUserMenu(LinearLayout menu) {
        menu.removeAllViews();
        menu.addView(userHeader(), rowParams(0, 10));
        menu.addView(menuItem("▢", "修改密码", ACTION_USER_PASSWORD), menuItemParams());
        menu.addView(menuItem("▷", "播放偏好设置", ACTION_USER_PREFERENCE), menuItemParams());
        menu.addView(menuItem("◉", "外观", ACTION_USER_APPEARANCE), menuItemParams());
        menu.addView(menuDivider(), dividerParams());
        menu.addView(menuItem("?", "帮助中心", ACTION_HELP), menuItemParams());
        menu.addView(menuItem("i", "关于飞牛影视", ACTION_ABOUT), menuItemParams());
        menu.addView(menuDivider(), dividerParams());
        menu.addView(menuItem("↪", "退出", ACTION_LOGOUT), menuItemParams());
    }

    private View userHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), 0, dp(14), 0);
        TextView avatar = new TextView(context);
        avatar.setText(initial(username));
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(18);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundDrawable(FnosTheme.rounded(0xFF7588D1, 22, context));
        header.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(10), 0, 0, 0);
        LinearLayout nameRow = new LinearLayout(context);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(context);
        name.setText(username);
        name.setTextColor(FnosTheme.COLOR_TEXT);
        name.setTextSize(14);
        name.setTypeface(null, 1);
        nameRow.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (admin) {
            TextView pill = new TextView(context);
            pill.setText("管理员");
            pill.setTextColor(FnosTheme.COLOR_PRIMARY);
            pill.setTextSize(11);
            pill.setGravity(Gravity.CENTER);
            pill.setBackgroundDrawable(FnosTheme.rounded(0xFF0D376F, 4, context));
            nameRow.addView(pill, new LinearLayout.LayoutParams(dp(42), dp(20)));
        }
        texts.addView(nameRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView source = new TextView(context);
        source.setText(sourceName);
        source.setTextColor(FnosTheme.COLOR_TEXT_MUTED);
        source.setTextSize(12);
        texts.addView(source);
        header.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return header;
    }

    private View menuItem(String icon, String label, final String action) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(12), 0);
        row.setFocusable(true);
        row.setClickable(true);
        FocusStyler.apply(row, FnosTheme.COLOR_CARD, FnosTheme.COLOR_CARD_FOCUSED, 4);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideUserMenu();
                listener.onHomeAction(action);
            }
        });
        TextView iconView = new TextView(context);
        iconView.setText(icon);
        iconView.setTextColor(FnosTheme.COLOR_TEXT);
        iconView.setTextSize(15);
        iconView.setGravity(Gravity.CENTER);
        row.addView(iconView, new LinearLayout.LayoutParams(dp(22), ViewGroup.LayoutParams.MATCH_PARENT));
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(FnosTheme.COLOR_TEXT);
        text.setTextSize(14);
        text.setPadding(dp(8), 0, 0, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private View menuDivider() {
        View divider = new View(context);
        divider.setBackgroundColor(FnosTheme.COLOR_STROKE);
        return divider;
    }

    private void toggleUserMenu() {
        if (userMenuView == null) {
            return;
        }
        userMenuView.setVisibility(userMenuView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        if (userMenuView.getVisibility() == View.VISIBLE) {
            userMenuView.requestFocus();
        }
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
                dp(HomeSidebarLayout.ROW_HEIGHT));
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams sectionParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(HomeSidebarLayout.SECTION_HEIGHT));
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

    private LinearLayout.LayoutParams rowHeightParams(int top, int bottom, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(height));
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams menuItemParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
    }

    private LinearLayout.LayoutParams dividerParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.topMargin = dp(8);
        params.bottomMargin = dp(8);
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

    private String initial(String value) {
        String text = emptyToDefault(value, "?");
        return text.substring(0, 1).toUpperCase();
    }

    private String emptyToDefault(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }

    private final class HomeMediaCard extends FrameLayout {
        private final ImageView image;
        private final TextView fallback;
        private final TextView caption;
        private final boolean wide;
        private String summary;
        private FnosFileEntry entry;

        HomeMediaCard(Context context, String summary, boolean wide) {
            super(context);
            this.summary = summary;
            this.wide = wide;
            setPadding(dp(0), dp(0), dp(0), dp(0));
            setFocusable(true);
            setClickable(true);

            image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setVisibility(View.GONE);
            addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            fallback = new TextView(context);
            fallback.setTextColor(Color.WHITE);
            fallback.setTextSize(wide ? 16 : 15);
            fallback.setGravity(wide ? Gravity.CENTER : (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
            fallback.setPadding(dp(14), dp(12), dp(14), dp(14));
            addView(fallback, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            caption = new TextView(context);
            caption.setTextColor(Color.WHITE);
            caption.setTextSize(wide ? 14 : 15);
            caption.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            caption.setPadding(dp(12), dp(8), dp(12), dp(12));
            caption.setBackgroundColor(0x66000000);
            caption.setVisibility(View.GONE);
            addView(caption, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    wide ? dp(46) : dp(64),
                    Gravity.BOTTOM));
            renderText();
        }

        void setSummary(String summary) {
            this.summary = summary == null ? "" : summary;
            renderText();
        }

        void setEntry(FnosFileEntry entry) {
            this.entry = entry;
            reloadPoster();
        }

        void reloadPoster() {
            renderText();
            posterLoader.load(posterBaseUrl, entry, this, image, fallback, caption);
        }

        private void renderText() {
            fallback.setText(fallbackText());
            caption.setText(captionText());
            caption.setVisibility(View.GONE);
        }

        private String fallbackText() {
            if (entry != null && entry.name.length() > 0) {
                return entry.posterPath.length() > 0 ? summary : entry.name + "\n" + summary;
            }
            return summary;
        }

        private String captionText() {
            if (entry != null && entry.name.length() > 0) {
                return entry.name;
            }
            return summary;
        }
    }
}
