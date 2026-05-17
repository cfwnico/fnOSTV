package com.fnostv.android4.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.MediaDetailInfo;

public final class NativeMediaDetailView {
    public interface Listener {
        void onDetailPlayRequested(MediaDetailState state);

        void onDetailFavoriteToggled(MediaDetailState state);

        void onDetailBackRequested();

        void onDetailSourceSelected(MediaDetailState state, int sourceIndex);
    }

    private final Context context;
    private final Listener listener;
    private final PosterLoader posterLoader = new PosterLoader();
    private LinearLayout view;
    private FrameLayout posterFrame;
    private ImageView posterImage;
    private TextView posterFallback;
    private TextView titleView;
    private TextView metaView;
    private TextView overviewView;
    private TextView childrenView;
    private TextView detailStatusView;
    private TextView pathView;
    private TextView statusView;
    private TextView sourceView;
    private TextView playButton;
    private TextView sourceButton;
    private TextView favoriteButton;
    private TextView backButton;
    private MediaDetailState state;
    private String posterBaseUrl = "";

    public NativeMediaDetailView(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View create() {
        view = new LinearLayout(context);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setPadding(dp(46), dp(42), dp(46), dp(38));
        view.setBackgroundColor(FnosTheme.COLOR_APP_BG);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setVisibility(View.GONE);
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return handleKey(keyCode, event);
            }
        });

        view.addView(posterPanel(), new LinearLayout.LayoutParams(dp(330), dp(460)));
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        detailParams.leftMargin = dp(38);
        view.addView(detailPanel(), detailParams);
        return view;
    }

    public void show(MediaDetailState state, String posterBaseUrl) {
        this.state = state;
        this.posterBaseUrl = posterBaseUrl == null ? "" : posterBaseUrl;
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        render();
        if (playButton != null) {
            playButton.requestFocus();
        }
    }

    public void update(MediaDetailState state) {
        this.state = state;
        render();
    }

    public void hide() {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public boolean isVisible() {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return isVisible() && handleKey(keyCode, event);
    }

    public void setPosterAuthorizationToken(String token) {
        posterLoader.setAuthorizationToken(token);
        render();
    }

    private FrameLayout posterPanel() {
        posterFrame = new FrameLayout(context);
        posterFrame.setBackgroundDrawable(FnosTheme.rounded(FnosTheme.COLOR_CARD, 8, context));

        posterImage = new ImageView(context);
        posterImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        posterImage.setVisibility(View.GONE);
        posterFrame.addView(posterImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        posterFallback = new TextView(context);
        posterFallback.setTextColor(FnosTheme.COLOR_TEXT);
        posterFallback.setTextSize(22);
        posterFallback.setGravity(Gravity.CENTER);
        posterFallback.setPadding(dp(20), dp(20), dp(20), dp(20));
        posterFrame.addView(posterFallback, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return posterFrame;
    }

    private LinearLayout detailPanel() {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(30), dp(28), dp(30), dp(28));
        panel.setBackgroundDrawable(FnosTheme.rounded(FnosTheme.COLOR_PANEL, 8, context));

        TextView section = label("影视详情", 14, FnosTheme.COLOR_TEXT_MUTED);
        panel.addView(section, rowParams(0, 10));

        titleView = label("", 28, Color.WHITE);
        titleView.setSingleLine(false);
        panel.addView(titleView, rowParams(0, 12));

        metaView = label("", 15, FnosTheme.COLOR_TEXT_MUTED);
        panel.addView(metaView, rowParams(0, 10));

        overviewView = label("", 15, FnosTheme.COLOR_TEXT);
        overviewView.setSingleLine(false);
        panel.addView(overviewView, rowParams(0, 10));

        childrenView = label("", 14, FnosTheme.COLOR_TEXT_MUTED);
        panel.addView(childrenView, rowParams(0, 10));

        detailStatusView = label("", 13, FnosTheme.COLOR_TEXT_DIM);
        panel.addView(detailStatusView, rowParams(0, 10));

        pathView = label("", 13, FnosTheme.COLOR_TEXT_DIM);
        pathView.setSingleLine(false);
        panel.addView(pathView, rowParams(0, 24));

        sourceView = label("", 16, FnosTheme.COLOR_TEXT);
        panel.addView(sourceView, rowParams(0, 10));

        statusView = label("", 14, FnosTheme.COLOR_TEXT_MUTED);
        panel.addView(statusView, rowParams(0, 24));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.VERTICAL);
        playButton = actionButton("播放");
        sourceButton = actionButton("播放源");
        favoriteButton = actionButton("收藏");
        backButton = actionButton("返回");
        actions.addView(actionRow(playButton, sourceButton), actionRowParams(0));
        actions.addView(actionRow(favoriteButton, backButton), actionRowParams(8));
        panel.addView(actions, rowParams(0, 0));

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDetailPlayRequested(state);
            }
        });
        sourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleSource();
            }
        });
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDetailFavoriteToggled(state);
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDetailBackRequested();
            }
        });
        return panel;
    }

    private void render() {
        if (state == null || state.entry == null || titleView == null) {
            return;
        }
        FnosFileEntry entry = state.entry;
        MediaDetailInfo info = state.detailInfo == null ? MediaDetailInfo.empty() : state.detailInfo;
        String title = info.title.length() > 0 ? info.title : entry.name;
        titleView.setText(title.length() == 0 ? "未命名视频" : title);
        metaView.setText(metaText(entry, info));
        overviewView.setText(info.overview);
        overviewView.setVisibility(info.overview.length() > 0 ? View.VISIBLE : View.GONE);
        childrenView.setText(childrenText(state.detailChildren().size()));
        childrenView.setVisibility(state.detailChildren().size() > 0 ? View.VISIBLE : View.GONE);
        detailStatusView.setText(detailStatusText(info));
        detailStatusView.setVisibility(detailStatusView.getText().length() > 0 ? View.VISIBLE : View.GONE);
        pathView.setText(entry.path);
        sourceView.setText("播放源 " + state.sourceLabel());
        statusView.setText(statusText());
        favoriteButton.setText(state.favorite ? "取消收藏" : "收藏");
        sourceButton.setEnabled(state.sources().size() > 1);
        sourceButton.setText(state.sources().size() > 1 ? "播放源" : "单源");
        posterFallback.setText(FileBrowserLabels.posterPlaceholder(entry, state.favorite));
        posterLoader.load(posterBaseUrl, entry, posterFrame, posterImage, posterFallback);
    }

    private String metaText(FnosFileEntry entry, MediaDetailInfo info) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, info.year);
        appendPart(builder, info.rating.length() > 0 ? "评分 " + info.rating : "");
        appendPart(builder, info.category);
        appendPart(builder, info.durationLabel);
        appendPart(builder, info.sourceLabel);
        appendPart(builder, entry.formatLabel());
        appendPart(builder, sizeLabel(entry.size));
        appendPart(builder, state.favorite ? "已收藏" : "未收藏");
        return builder.toString();
    }

    private String childrenText(int count) {
        return count > 0 ? "剧集/版本 " + count + " 项" : "";
    }

    private String detailStatusText(MediaDetailInfo info) {
        if (state.loadingDetail) {
            return "详情加载中";
        }
        if (state.detailError.length() > 0) {
            return state.detailError;
        }
        if (!info.isEmpty()) {
            return "详情信息已同步";
        }
        return "";
    }

    private void appendPart(StringBuilder builder, String value) {
        if (value == null || value.length() == 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" · ");
        }
        builder.append(value);
    }

    private String statusText() {
        if (state.loadingSources) {
            return "播放源准备中";
        }
        if (state.errorMessage.length() > 0) {
            return state.errorMessage;
        }
        if (state.sources().size() == 0) {
            return "可直接尝试播放，失败时会进入外部播放器兜底";
        }
        return "确认键播放，左右键切换操作，菜单键收藏";
    }

    private boolean handleKey(int keyCode, KeyEvent event) {
        if (event == null || event.getAction() != KeyEvent.ACTION_DOWN || state == null) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            listener.onDetailBackRequested();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            listener.onDetailFavoriteToggled(state);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
            listener.onDetailPlayRequested(state);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            cycleSource(1);
            return state.sources().size() > 1;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            cycleSource(-1);
            return state.sources().size() > 1;
        }
        return false;
    }

    private void cycleSource() {
        cycleSource(1);
    }

    private void cycleSource(int direction) {
        if (state == null || state.sources().size() <= 1) {
            return;
        }
        int next = DetailSourceNavigation.move(state.selectedSourceIndex(), state.sources().size(), direction);
        listener.onDetailSourceSelected(state, next);
    }

    private TextView label(String text, int size, int color) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setGravity(Gravity.LEFT);
        return label;
    }

    private TextView actionButton(String text) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextColor(FnosTheme.COLOR_TEXT);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        FocusStyler.applyButton(button);
        return button;
    }

    private LinearLayout actionRow(TextView first, TextView second) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(first, actionParams(false));
        row.addView(second, actionParams(true));
        return row;
    }

    private String sizeLabel(long size) {
        if (size <= 0) {
            return "未知大小";
        }
        long mb = size / (1024L * 1024L);
        if (mb < 1024) {
            return mb + " MB";
        }
        return (mb / 1024) + " GB";
    }

    private LinearLayout.LayoutParams rowParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams actionParams(boolean lastInRow) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
        if (!lastInRow) {
            params.rightMargin = dp(12);
        }
        return params;
    }

    private LinearLayout.LayoutParams actionRowParams(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(top);
        return params;
    }

    private int dp(int value) {
        return FnosTheme.dp(context, value);
    }
}
