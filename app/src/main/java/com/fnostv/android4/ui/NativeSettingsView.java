package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.fnostv.android4.media.MediaLibrary;
import com.fnostv.android4.net.FnosSettingsSummary;

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

        void onSettingsAction(String message);
    }

    private static final String PAGE_PASSWORD = "password";
    private static final String PAGE_PREFERENCE = "preference";
    private static final String PAGE_APPEARANCE = "appearance";
    private static final String PAGE_LIBRARY = "library";
    private static final String PAGE_USERS = "users";
    private static final String PAGE_SERVER = "server";
    private static final String PAGE_TASK = "task";

    private final Context context;
    private final Listener listener;
    private LinearLayout sidebar;
    private LinearLayout content;
    private TextView titleView;
    private TextView statusView;
    private TextView versionView;
    private List<MediaLibrary> libraries;
    private FnosSettingsSummary summary = FnosSettingsSummary.empty();
    private String status = "";
    private String page = PAGE_LIBRARY;

    public NativeSettingsView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setInitialPage(String page) {
        if (isKnownPage(page)) {
            this.page = page;
        }
    }

    public View create(List<MediaLibrary> libraries, String status, FnosSettingsSummary summary) {
        this.libraries = libraries;
        this.status = status == null ? "" : status;
        if (summary != null) {
            this.summary = summary;
        }

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(FnosTheme.COLOR_APP_BG);

        LinearLayout shell = new LinearLayout(context);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        sidebar = new LinearLayout(context);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setPadding(dp(20), dp(22), dp(20), dp(18));
        sidebar.setBackgroundColor(FnosTheme.COLOR_SIDEBAR);
        shell.addView(sidebar, new LinearLayout.LayoutParams(dp(252), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout main = new LinearLayout(context);
        main.setOrientation(LinearLayout.VERTICAL);
        shell.addView(main, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        titleView = text("", 20, FnosTheme.COLOR_TEXT);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setPadding(dp(42), 0, dp(28), 0);
        main.addView(titleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        View divider = new View(context);
        divider.setBackgroundColor(FnosTheme.COLOR_STROKE);
        main.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        ScrollView scroll = new ScrollView(context);
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(42), dp(24), dp(42), dp(42));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        main.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        render();
        return root;
    }

    public void refresh(List<MediaLibrary> libraries, String status, FnosSettingsSummary summary) {
        this.libraries = libraries;
        this.status = status == null ? "" : status;
        if (summary != null) {
            this.summary = summary;
        }
        render();
    }

    private void render() {
        if (sidebar == null || content == null) {
            return;
        }
        renderSidebar();
        content.removeAllViews();
        titleView.setText(titleForPage());
        if (status.length() > 0) {
            statusView = muted(status, 13);
            content.addView(statusView, rowParams(0, 16));
        }
        if (PAGE_PASSWORD.equals(page)) {
            renderPassword();
        } else if (PAGE_PREFERENCE.equals(page)) {
            renderPreference();
        } else if (PAGE_APPEARANCE.equals(page)) {
            renderAppearance();
        } else if (PAGE_LIBRARY.equals(page)) {
            renderLibrary();
        } else if (PAGE_USERS.equals(page)) {
            renderUsers();
        } else if (PAGE_SERVER.equals(page)) {
            renderServer();
        } else if (PAGE_TASK.equals(page)) {
            renderTask();
        }
    }

    private boolean isKnownPage(String value) {
        return PAGE_PASSWORD.equals(value)
                || PAGE_PREFERENCE.equals(value)
                || PAGE_APPEARANCE.equals(value)
                || PAGE_LIBRARY.equals(value)
                || PAGE_USERS.equals(value)
                || PAGE_SERVER.equals(value)
                || PAGE_TASK.equals(value);
    }

    private void renderSidebar() {
        sidebar.removeAllViews();
        LinearLayout brand = new LinearLayout(context);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("←", 28, FnosTheme.COLOR_TEXT);
        back.setGravity(Gravity.CENTER);
        back.setFocusable(true);
        back.setClickable(true);
        FocusStyler.apply(back, FnosTheme.COLOR_SIDEBAR, FnosTheme.COLOR_CARD, 6);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onCloseSettings();
            }
        });
        brand.addView(back, new LinearLayout.LayoutParams(dp(38), dp(42)));
        brand.addView(new FnosLogoMarkView(context), new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView logo = text("飞牛影视", 17, FnosTheme.COLOR_TEXT);
        logo.setPadding(dp(6), 0, 0, 0);
        brand.addView(logo, new LinearLayout.LayoutParams(0, dp(42), 1));
        sidebar.addView(brand, rowParams(0, 28));

        sidebar.addView(section("帐号"), rowParams(0, 8));
        sidebar.addView(tab("▢", "修改密码", PAGE_PASSWORD), rowParams(0, 8));
        sidebar.addView(tab("▷", "播放偏好", PAGE_PREFERENCE), rowParams(0, 8));
        sidebar.addView(tab("◉", "外观", PAGE_APPEARANCE), rowParams(0, 30));

        sidebar.addView(section("影视服务器"), rowParams(0, 8));
        sidebar.addView(tab("□", "媒体库", PAGE_LIBRARY), rowParams(0, 8));
        sidebar.addView(tab("◎", "用户", PAGE_USERS), rowParams(0, 8));
        sidebar.addView(tab("⚙", "设置", PAGE_SERVER), rowParams(0, 8));
        sidebar.addView(tab("▣", "任务计划", PAGE_TASK), rowParams(0, 0));

        SpaceView spacer = new SpaceView(context);
        sidebar.addView(spacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        versionView = muted("版本号 " + summary.version + "（服务版本 " + summary.serviceVersion + "）", 12);
        sidebar.addView(versionView, rowParams(0, 0));
    }

    private View tab(String icon, String label, final String targetPage) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(12), 0);
        row.setFocusable(true);
        row.setClickable(true);
        boolean active = targetPage.equals(page);
        row.setBackgroundDrawable(FnosTheme.rounded(active ? FnosTheme.COLOR_CARD : FnosTheme.COLOR_SIDEBAR, 8, context));
        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundDrawable(FnosTheme.rounded(hasFocus || targetPage.equals(page)
                        ? FnosTheme.COLOR_CARD : FnosTheme.COLOR_SIDEBAR, 8, context));
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                page = targetPage;
                render();
            }
        });

        TextView iconView = text(icon, 18, active ? FnosTheme.COLOR_PRIMARY : FnosTheme.COLOR_TEXT);
        iconView.setGravity(Gravity.CENTER);
        row.addView(iconView, new LinearLayout.LayoutParams(dp(24), dp(40)));
        TextView text = text(label, 15, active ? FnosTheme.COLOR_PRIMARY : FnosTheme.COLOR_TEXT);
        text.setPadding(dp(8), 0, 0, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private void renderPassword() {
        LinearLayout user = new LinearLayout(context);
        user.setGravity(Gravity.CENTER_VERTICAL);
        TextView avatar = avatar(summary.userInitial, dp(72));
        user.addView(avatar, new LinearLayout.LayoutParams(dp(72), dp(72)));
        TextView name = text(summary.username, 28, FnosTheme.COLOR_TEXT);
        name.setPadding(dp(14), 0, 0, 0);
        user.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        content.addView(user, rowParams(12, 34));

        content.addView(label("新密码"), rowParams(0, 8));
        content.addView(passwordInput("请输入密码"), fixedWidthParams(400, 38, 0, 28));
        content.addView(label("确认密码"), rowParams(0, 8));
        content.addView(passwordInput("再次输入密码"), fixedWidthParams(400, 38, 0, 36));
        Button save = button("保存修改", true, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSettingsAction("修改密码接口已识别为 /user/passwd，正式保存前需要再次确认。");
            }
        });
        content.addView(save, fixedWidthParams(96, 42, 0, 0));
    }

    private void renderPreference() {
        content.addView(sectionTitle("首选字幕语言"), rowParams(0, 10));
        content.addView(muted("优先下载及使用首选语言的字幕，播放时可切换至其他语言。", 14), rowParams(0, 14));
        content.addView(valueBox("中文简体", false), fixedWidthParams(720, 38, 0, 0));
    }

    private void renderAppearance() {
        content.addView(sectionTitle("主题模式"), rowParams(0, 16));
        LinearLayout themes = new LinearLayout(context);
        themes.setOrientation(LinearLayout.HORIZONTAL);
        themes.addView(themeCard("跟随系统", false, true), new LinearLayout.LayoutParams(dp(160), dp(180)));
        LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(dp(160), dp(180));
        themeParams.leftMargin = dp(20);
        themes.addView(themeCard("浅色", false, false), themeParams);
        LinearLayout.LayoutParams darkParams = new LinearLayout.LayoutParams(dp(160), dp(180));
        darkParams.leftMargin = dp(20);
        themes.addView(themeCard("深色", true, false), darkParams);
        content.addView(themes, rowParams(0, 42));

        content.addView(sectionTitle("卡片样式"), rowParams(0, 16));
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(16));
        panel.setBackgroundDrawable(FnosTheme.stroked(FnosTheme.COLOR_CARD, FnosTheme.COLOR_STROKE, 12, context));
        panel.addView(posterPreview(), centeredParams(170, 252, 0, 24));
        panel.addView(divider(), rowParams(0, 18));
        panel.addView(settingSwitch("评分", true), rowParams(0, 12));
        panel.addView(settingSwitch("“已观看” 标记", false), rowParams(0, 12));
        panel.addView(settingSwitch("分辨率", true), rowParams(0, 0));
        content.addView(panel, fixedWidthParams(520, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0));
    }

    private void renderLibrary() {
        LinearLayout actions = new LinearLayout(context);
        actions.addView(button("新增媒体库", true, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onAddLibrary();
            }
        }), fixedWidthParams(112, 38, 0, 0));
        actions.addView(button("排序", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSettingsAction("排序功能已展示，后续会接入拖拽顺序保存。");
            }
        }), fixedWidthParams(82, 38, 16, 0));
        actions.addView(button("扫描媒体库文件", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onScanLibraries();
            }
        }), fixedWidthParams(142, 38, 16, 0));
        content.addView(actions, rowParams(0, 36));
        content.addView(tableHeader(new String[]{"媒体库", "媒体文件夹", "类型", "文件最近更新", "操作"}), rowParams(0, 0));
        if (libraries != null) {
            for (int i = 0; i < libraries.size(); i++) {
                content.addView(libraryRow(libraries.get(i)), rowParams(0, 0));
            }
        }
    }

    private void renderUsers() {
        LinearLayout actions = new LinearLayout(context);
        actions.addView(button("＋ 新增用户", true, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSettingsAction("新增用户入口已复刻，写入接口为 /manager/user/create。");
            }
        }), fixedWidthParams(112, 38, 0, 0));
        actions.addView(button("⚙ 新用户默认权限设置", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSettingsAction("默认权限接口已识别为 /manager/template/permission。");
            }
        }), fixedWidthParams(184, 38, 16, 0));
        content.addView(actions, rowParams(0, 36));
        content.addView(tableHeader(new String[]{"用户名", "可访问媒体库", "NAS 账号登录", "最近访问", "操作"}), rowParams(0, 0));
        for (int i = 0; i < summary.userRows.size(); i++) {
            content.addView(userRow(summary.userRows.get(i)), rowParams(0, 0));
        }
    }

    private void renderServer() {
        content.addView(serverField("服务器名称", "用于在网络中识别您的影视服务器。", summary.serverName), rowParams(0, 26));
        content.addView(serverField("首选语言", "作为语言相关设置项的默认选项，如字幕语言。可在每个媒体库设置中单独修改。", summary.languageLabel), rowParams(0, 26));
        content.addView(settingSwitch("启用 GPU 加速转码", summary.gpuAccelerationEnabled), rowParams(0, 12));
        content.addView(valueBox(summary.preferredGpuLabel, true), fixedWidthParams(700, 38, 20, 16));
        content.addView(settingSwitch("GPU 不支持解码影片格式时，自动切换为 CPU 解码", summary.cpuFallbackEnabled), rowParams(0, 26));
        content.addView(sectionTitle("直连播放"), rowParams(0, 12));
        content.addView(settingSwitch("网盘优先直连播放", summary.directLinkEnabled), rowParams(0, 12));
        content.addView(serverField("允许直连播放的用户", "包括网盘和 STRM 文件。请避免从多个 IP 频繁播放，以免触发网盘风控。", summary.directLinkAllowedLabel), rowParams(0, 26));
        content.addView(serverField("媒体元数据保存位置", "指定刮削产生的媒体元数据和图像的存储位置。", "默认位置  存储空间 1"), rowParams(0, 26));
        content.addView(serverField("播放临时文件缓存位置", "所选位置的硬盘性能将影响播放时的流畅性。", "默认位置  存储空间 1"), rowParams(0, 26));
        content.addView(settingSwitch("实时监控媒体文件更新", summary.fileMonitorEnabled), rowParams(0, 26));
        content.addView(button("重建索引", false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSettingsAction("重建索引接口已识别为 /search/rebuild，暂未自动执行。");
            }
        }), fixedWidthParams(110, 38, 0, 0));
    }

    private void renderTask() {
        content.addView(sectionTitle("定时执行任务"), rowParams(0, 10));
        content.addView(muted("每天凌晨自动执行任务，以提升日常使用影视的体验。", 14), rowParams(0, 26));
        for (int i = 0; i < summary.taskRows.size(); i++) {
            FnosSettingsSummary.TaskRow row = summary.taskRows.get(i);
            content.addView(taskRow(row.label, row.statusLabel), rowParams(0, 12));
        }
        if (summary.taskRows.size() == 0) {
            content.addView(muted("暂无任务计划", 15), rowParams(0, 0));
        }
    }

    private View themeCard(String label, boolean active, boolean split) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundDrawable(FnosTheme.stroked(active ? FnosTheme.COLOR_CARD : FnosTheme.COLOR_APP_BG, FnosTheme.COLOR_STROKE, 12, context));
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSettingsAction("主题模式已复刻展示，Android 4 端固定深色模式。");
            }
        });

        LinearLayout preview = new LinearLayout(context);
        preview.setPadding(dp(24), dp(20), dp(24), dp(18));
        preview.setBackgroundColor(active ? 0xFF11161C : Color.WHITE);
        LinearLayout left = previewColumn(active ? 0xFF2A323B : 0xFFE1E7F0);
        preview.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        rightParams.leftMargin = dp(16);
        preview.addView(previewColumn(split ? 0xFF28313A : (active ? 0xFF2A323B : 0xFFE1E7F0)), rightParams);
        card.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView name = text((active ? "●  " : "○  ") + label, 15, FnosTheme.COLOR_TEXT);
        name.setPadding(dp(24), 0, 0, 0);
        name.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        return card;
    }

    private LinearLayout previewColumn(int color) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView image = new TextView(context);
        image.setBackgroundDrawable(FnosTheme.rounded(color, 8, context));
        column.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));
        TextView line = new TextView(context);
        line.setBackgroundDrawable(FnosTheme.rounded(color, 4, context));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(16));
        params.topMargin = dp(10);
        column.addView(line, params);
        return column;
    }

    private View posterPreview() {
        FrameLayout poster = new FrameLayout(context);
        poster.setBackgroundDrawable(FnosTheme.rounded(0xFF6BA6D4, 6, context));
        TextView title = text("TRIM", 30, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        poster.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView score = text("9.2", 16, 0xFFFFE66D);
        score.setGravity(Gravity.CENTER);
        score.setBackgroundDrawable(FnosTheme.rounded(0xCC1B2029, 6, context));
        FrameLayout.LayoutParams scoreParams = new FrameLayout.LayoutParams(dp(48), dp(28), Gravity.TOP | Gravity.LEFT);
        scoreParams.leftMargin = dp(10);
        scoreParams.topMargin = dp(10);
        poster.addView(score, scoreParams);
        TextView badge = text("4K 1080", 11, Color.WHITE);
        badge.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        badge.setPadding(0, 0, dp(8), dp(8));
        poster.addView(badge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return poster;
    }

    private View libraryRow(final MediaLibrary library) {
        LinearLayout row = tableRow();
        row.addView(cell(library.name, true), weightParams(1.3f));
        row.addView(cell(pathsText(library), false), weightParams(2.2f));
        row.addView(cell(library.categoryLabel(), false), weightParams(1f));
        row.addView(cell(updatedAt(library.updatedAt), false), weightParams(1.2f));
        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(smallButton("编辑", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEditLibrary(library);
            }
        }));
        actions.addView(smallButton("删除", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteLibrary(library);
            }
        }));
        row.addView(actions, weightParams(1f));
        return row;
    }

    private View userRow(FnosSettingsSummary.UserRow user) {
        LinearLayout row = tableRow();
        LinearLayout identity = new LinearLayout(context);
        identity.setGravity(Gravity.CENTER_VERTICAL);
        identity.addView(avatar(user.username.substring(0, 1), dp(34)), new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView name = text(user.username + "  " + user.roleLabel, 14, FnosTheme.COLOR_TEXT);
        name.setPadding(dp(10), 0, 0, 0);
        identity.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(identity, weightParams(1.3f));
        row.addView(cell(user.permissionLabel, false), weightParams(1.4f));
        row.addView(cell(user.sourceName, false), weightParams(1.4f));
        row.addView(cell(user.lastLoginDate, false), weightParams(1.2f));
        row.addView(cell("✎", false), weightParams(0.7f));
        return row;
    }

    private View taskRow(String name, String statusLabel) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(14), dp(18), dp(14));
        row.setBackgroundDrawable(FnosTheme.stroked(FnosTheme.COLOR_CARD, FnosTheme.COLOR_STROKE, 8, context));
        row.addView(text(name, 16, FnosTheme.COLOR_TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(valuePill(statusLabel), new LinearLayout.LayoutParams(dp(88), dp(32)));
        return row;
    }

    private View serverField(String title, String desc, String value) {
        LinearLayout group = new LinearLayout(context);
        group.setOrientation(LinearLayout.VERTICAL);
        group.addView(sectionTitle(title), rowParams(0, 8));
        group.addView(muted(desc, 13), rowParams(0, 10));
        group.addView(valueBox(value, false), fixedWidthParams(720, 38, 0, 0));
        return group;
    }

    private View settingSwitch(String label, boolean checked) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView text = text(label, 16, FnosTheme.COLOR_TEXT);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(context);
        toggle.setChecked(checked);
        toggle.setFocusable(true);
        row.addView(toggle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View tableHeader(String[] labels) {
        LinearLayout row = tableRow();
        row.setBackgroundColor(FnosTheme.COLOR_APP_BG);
        float[] weights = weightsFor(labels.length);
        for (int i = 0; i < labels.length; i++) {
            row.addView(cell(labels[i], true), weightParams(weights[i]));
        }
        return row;
    }

    private LinearLayout tableRow() {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(10), 0);
        row.setMinimumHeight(dp(72));
        return row;
    }

    private TextView cell(String text, boolean strong) {
        TextView view = text(text, 14, strong ? FnosTheme.COLOR_TEXT : FnosTheme.COLOR_TEXT_MUTED);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setSingleLine(false);
        return view;
    }

    private float[] weightsFor(int count) {
        if (count == 5) {
            return new float[]{1.3f, 2.2f, 1f, 1.2f, 1f};
        }
        return new float[]{1f, 1f, 1f, 1f, 1f};
    }

    private TextView valueBox(String value, boolean dropdown) {
        TextView view = text(value + (dropdown ? "   ˅" : ""), 15, FnosTheme.COLOR_TEXT);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setBackgroundDrawable(FnosTheme.stroked(FnosTheme.COLOR_CARD, FnosTheme.COLOR_STROKE, 6, context));
        return view;
    }

    private TextView valuePill(String value) {
        TextView view = text(value, 13, FnosTheme.COLOR_TEXT);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundDrawable(FnosTheme.rounded(0xFF22364D, 16, context));
        return view;
    }

    private EditText passwordInput(String hint) {
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setTextColor(FnosTheme.COLOR_TEXT);
        input.setHintTextColor(FnosTheme.COLOR_TEXT_DIM);
        input.setBackgroundDrawable(FnosTheme.stroked(FnosTheme.COLOR_CARD, FnosTheme.COLOR_STROKE, 6, context));
        input.setPadding(dp(12), 0, dp(12), 0);
        return input;
    }

    private Button button(String text, boolean primary, View.OnClickListener clickListener) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(FnosTheme.COLOR_TEXT);
        FocusStyler.apply(button, primary ? FnosTheme.COLOR_PRIMARY : FnosTheme.COLOR_CARD, FnosTheme.COLOR_CARD_FOCUSED, 8);
        button.setOnClickListener(clickListener);
        return button;
    }

    private Button smallButton(String text, View.OnClickListener clickListener) {
        Button button = button(text, false, clickListener);
        button.setTextSize(12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(62), dp(34));
        params.leftMargin = dp(6);
        button.setLayoutParams(params);
        return button;
    }

    private TextView avatar(String text, int size) {
        TextView avatar = text(text, 18, Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundDrawable(FnosTheme.rounded(0xFF7588D1, size / 2, context));
        return avatar;
    }

    private TextView section(String text) {
        TextView view = muted(text, 13);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = text(text, 20, FnosTheme.COLOR_TEXT);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTypeface(null, 1);
        return view;
    }

    private TextView label(String text) {
        TextView view = text(text, 16, FnosTheme.COLOR_TEXT);
        view.setTypeface(null, 1);
        return view;
    }

    private TextView muted(String text, int size) {
        return text(text, size, FnosTheme.COLOR_TEXT_MUTED);
    }

    private TextView text(String text, int size, int color) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private View divider() {
        View view = new View(context);
        view.setBackgroundColor(FnosTheme.COLOR_STROKE);
        return view;
    }

    private String titleForPage() {
        if (PAGE_PASSWORD.equals(page)) {
            return "帐号 - 修改密码";
        }
        if (PAGE_PREFERENCE.equals(page)) {
            return "帐号 - 播放偏好";
        }
        if (PAGE_APPEARANCE.equals(page)) {
            return "帐号 - 外观";
        }
        if (PAGE_LIBRARY.equals(page)) {
            return "影视服务器 - 媒体库";
        }
        if (PAGE_USERS.equals(page)) {
            return "影视服务器 - 用户";
        }
        if (PAGE_SERVER.equals(page)) {
            return "影视服务器 - 设置";
        }
        return "影视服务器 - 任务计划";
    }

    private String pathsText(MediaLibrary library) {
        if (library.paths.size() == 0) {
            return "未设置目录";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < library.paths.size(); i++) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(library.paths.get(i));
        }
        return builder.toString();
    }

    private String updatedAt(long value) {
        if (value <= 0) {
            return "--";
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(value));
    }

    private LinearLayout.LayoutParams rowParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams fixedWidthParams(int width, int height, int left, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(width), height < 0 ? height : dp(height));
        params.leftMargin = dp(left);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams centeredParams(int width, int height, int top, int bottom) {
        LinearLayout.LayoutParams params = fixedWidthParams(width, height, 0, bottom);
        params.topMargin = dp(top);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private LinearLayout.LayoutParams weightParams(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    private int dp(int value) {
        return FnosTheme.dp(context, value);
    }

    private static final class SpaceView extends View {
        SpaceView(Context context) {
            super(context);
        }
    }
}
