package com.fnostv.android4;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.fnostv.android4.config.ProfileStore;
import com.fnostv.android4.config.ProfileValidation;
import com.fnostv.android4.config.ProfileValidator;
import com.fnostv.android4.config.ServerProfile;
import com.fnostv.android4.media.MediaIndexStore;
import com.fnostv.android4.media.MediaLibrary;
import com.fnostv.android4.media.MediaLibraryCategory;
import com.fnostv.android4.media.MediaLibraryClassifier;
import com.fnostv.android4.media.MediaLibraryScanner;
import com.fnostv.android4.media.MediaLibraryStore;
import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosRpcClient;
import com.fnostv.android4.net.FnosRpcException;
import com.fnostv.android4.net.FnosSession;
import com.fnostv.android4.net.FnosSessionStore;
import com.fnostv.android4.ui.NativeSettingsView;
import com.fnostv.android4.ui.SettingsForm;
import com.fnostv.android4.util.Constants;
import com.fnostv.android4.util.Logger;

import java.util.ArrayList;
import java.util.List;

public final class SettingsActivity extends Activity implements SettingsForm.Listener, NativeSettingsView.Listener {
    private static final String[] CATEGORY_LABELS = {"全部", "电影", "电视节目", "其他"};
    private static final String[] CATEGORY_VALUES = {
            MediaLibraryCategory.ALL,
            MediaLibraryCategory.MOVIE,
            MediaLibraryCategory.TV,
            MediaLibraryCategory.OTHER
    };

    private ProfileStore store;
    private FnosSessionStore sessionStore;
    private MediaLibraryStore mediaLibraryStore;
    private MediaIndexStore mediaIndexStore;
    private SettingsForm settingsForm;
    private NativeSettingsView nativeSettingsView;
    private boolean accountEditorOpen;
    private boolean scanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        store = new ProfileStore(this);
        sessionStore = new FnosSessionStore(this);
        mediaLibraryStore = new MediaLibraryStore(this);
        mediaIndexStore = new MediaIndexStore(this);
        settingsForm = new SettingsForm(this, this);
        nativeSettingsView = new NativeSettingsView(this, this);

        String error = getIntent().getStringExtra(Constants.EXTRA_SETTINGS_ERROR_MESSAGE);
        if (error != null || !store.load().isReady()) {
            showAccountEditor(error);
        } else {
            showNativeSettings("管理本地媒体库，扫描后首页和分类页会使用本地索引。");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onCancelRequested();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveRequested(ServerProfile profile) {
        ProfileValidation validation = ProfileValidator.validate(profile);
        if (!validation.isValid()) {
            Toast.makeText(this, validation.getMessage(), Toast.LENGTH_SHORT).show();
            settingsForm.focusServerUrl();
            return;
        }
        store.save(profile);
        if (accountEditorOpen && profile.isReady()) {
            accountEditorOpen = false;
            showNativeSettings("账号连接已保存。");
            return;
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onCancelRequested() {
        if (accountEditorOpen && store.load().isReady()) {
            accountEditorOpen = false;
            showNativeSettings("");
            return;
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onCloseSettings() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onEditAccount() {
        showAccountEditor(null);
    }

    @Override
    public void onAddLibrary() {
        showLibraryEditor(null);
    }

    @Override
    public void onEditLibrary(MediaLibrary library) {
        showLibraryEditor(library);
    }

    @Override
    public void onDeleteLibrary(final MediaLibrary library) {
        if (library == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("删除媒体库")
                .setMessage("确定删除“" + library.name + "”？本地索引会在下次扫描后刷新。")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mediaLibraryStore.delete(library.id);
                        refreshNativeSettings("已删除媒体库：" + library.name);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onScanLibraries() {
        if (scanning) {
            return;
        }
        final List<MediaLibrary> libraries = mediaLibraryStore.listOrSeedDefault();
        if (!hasScanPath(libraries)) {
            Toast.makeText(this, "请先为媒体库添加至少一个目录路径", Toast.LENGTH_SHORT).show();
            return;
        }
        scanning = true;
        refreshNativeSettings("正在扫描媒体库...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ScanResult result = scanLibraries(libraries);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scanning = false;
                        if (result.success) {
                            refreshNativeSettings("扫描完成，共索引 " + result.count + " 个视频。");
                        } else {
                            refreshNativeSettings(result.message);
                        }
                    }
                });
            }
        }, "fnos-media-library-scan").start();
    }

    private void showAccountEditor(String errorMessage) {
        accountEditorOpen = true;
        setContentView(settingsForm.create(store.load(), errorMessage));
    }

    private void showNativeSettings(String status) {
        accountEditorOpen = false;
        setContentView(nativeSettingsView.create(mediaLibraryStore.listOrSeedDefault(), status));
    }

    private void refreshNativeSettings(String status) {
        nativeSettingsView.refresh(mediaLibraryStore.listOrSeedDefault(), status);
    }

    private void showLibraryEditor(final MediaLibrary existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        form.setPadding(padding, padding, padding, 0);

        final EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint("媒体库名称，例如 影视大全");
        nameInput.setText(existing == null ? "" : existing.name);
        form.addView(nameInput);

        final Spinner categoryInput = new Spinner(this);
        categoryInput.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORY_LABELS));
        categoryInput.setSelection(categoryIndex(existing == null ? MediaLibraryCategory.ALL : existing.category));
        form.addView(categoryInput);

        final EditText pathsInput = new EditText(this);
        pathsInput.setMinLines(3);
        pathsInput.setHint("目录路径，每行一个，例如 /video/Movies");
        pathsInput.setText(existing == null ? "" : joinPaths(existing.paths));
        form.addView(pathsInput);

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "添加媒体库" : "编辑媒体库")
                .setView(form)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<String> paths = parsePaths(pathsInput.getText().toString());
                        if (paths.size() == 0) {
                            Toast.makeText(SettingsActivity.this, "请填写至少一个目录路径", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String id = existing == null ? MediaLibraryStore.newId() : existing.id;
                        int order = existing == null ? mediaLibraryStore.list().size() : existing.sortOrder;
                        MediaLibrary library = new MediaLibrary(
                                id,
                                nameInput.getText().toString(),
                                CATEGORY_VALUES[categoryInput.getSelectedItemPosition()],
                                paths,
                                true,
                                order,
                                existing == null ? 0L : existing.updatedAt);
                        mediaLibraryStore.upsert(library);
                        refreshNativeSettings("已保存媒体库：" + library.name);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private ScanResult scanLibraries(List<MediaLibrary> libraries) {
        try {
            ServerProfile profile = store.load();
            FnosSession session = sessionStore.load();
            if (!profile.isReady() || !session.hasToken()) {
                return ScanResult.failure("登录会话已失效，请返回重新登录后再扫描。");
            }
            FnosRpcClient client = new FnosRpcClient(profile, sessionStore.getOrCreateDeviceId());
            MediaLibraryScanner scanner = new MediaLibraryScanner(client, session);
            List<FnosFileEntry> entries = scanner.scan(libraries);
            mediaIndexStore.replaceAll(entries);
            markLibrariesScanned(libraries);
            return ScanResult.success(entries.size());
        } catch (FnosRpcException ex) {
            Logger.w("Media library scan failed: " + ex.getMessage());
            return ScanResult.failure("媒体库扫描失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            Logger.w("Media library scan crashed: " + ex.getMessage());
            return ScanResult.failure("媒体库扫描异常：" + ex.getMessage());
        }
    }

    private void markLibrariesScanned(List<MediaLibrary> libraries) {
        List<MediaLibrary> updated = new ArrayList<MediaLibrary>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < libraries.size(); i++) {
            MediaLibrary library = libraries.get(i);
            updated.add(library.enabled ? library.withUpdatedAt(now) : library);
        }
        mediaLibraryStore.saveAll(updated);
    }

    private boolean hasScanPath(List<MediaLibrary> libraries) {
        for (int i = 0; i < libraries.size(); i++) {
            if (libraries.get(i).enabled && libraries.get(i).paths.size() > 0) {
                return true;
            }
        }
        return false;
    }

    private int categoryIndex(String category) {
        String value = MediaLibraryCategory.normalize(category);
        for (int i = 0; i < CATEGORY_VALUES.length; i++) {
            if (CATEGORY_VALUES[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private List<String> parsePaths(String raw) {
        List<String> paths = new ArrayList<String>();
        String[] parts = raw == null ? new String[0] : raw.split("[,\\r\\n]+");
        for (int i = 0; i < parts.length; i++) {
            String value = MediaLibraryClassifier.normalizePath(parts[i]);
            if (value.length() > 0 && !paths.contains(value)) {
                paths.add(value);
            }
        }
        return paths;
    }

    private String joinPaths(List<String> paths) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(paths.get(i));
        }
        return builder.toString();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class ScanResult {
        final boolean success;
        final int count;
        final String message;

        private ScanResult(boolean success, int count, String message) {
            this.success = success;
            this.count = count;
            this.message = message;
        }

        static ScanResult success(int count) {
            return new ScanResult(true, count, "");
        }

        static ScanResult failure(String message) {
            return new ScanResult(false, 0, message);
        }
    }
}
