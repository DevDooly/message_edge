package com.devdooly.notificationedge;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 실제 계정이나 콘텐츠 없이 미디어 세션의 일시정지·자동 PiP를 재현한다. */
public class PipPlaybackFixtureActivity extends Activity {
    private MediaSession session;
    private TextView label;
    private boolean playing;
    private boolean autoPip;
    private int pipEntries;
    private Intent panelShortcut;
    private String publishedShortcutStatus = "not checked";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        label = new TextView(this);
        label.setTextColor(Color.WHITE);
        label.setBackgroundColor(Color.rgb(15, 32, 55));
        label.setTextSize(24);
        label.setGravity(android.view.Gravity.CENTER);
        label.setOnClickListener(view -> updatePlayback(!playing));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(label, new LinearLayout.LayoutParams(-1, 0, 1f));
        Button configure = new Button(this);
        configure.setText("Configure Slivue shortcut");
        configure.setOnClickListener(view -> {
            // 등록 단계는 재생 중 PiP 시험과 분리한다.
            updatePlayback(false);
            Intent request = new Intent(Intent.ACTION_CREATE_SHORTCUT)
                .setClassName("com.devdooly.notificationedge",
                    "com.devdooly.notificationedge.ui.OpenPanelActivity");
            startActivityForResult(request, 1);
        });
        content.addView(configure);
        Button launch = new Button(this);
        launch.setText("Run Slivue shortcut");
        launch.setOnClickListener(view -> {
            if (panelShortcut != null) startActivity(new Intent(panelShortcut));
        });
        content.addView(launch);
        Button published = new Button(this);
        published.setText("Run published Slivue app shortcut");
        published.setOnClickListener(view -> {
            try {
                LauncherApps launcher = getSystemService(LauncherApps.class);
                LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery()
                    .setPackage("com.devdooly.notificationedge")
                    .setShortcutIds(java.util.Collections.singletonList("slivue_open_panel"))
                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC);
                java.util.List<ShortcutInfo> found = launcher.getShortcuts(query, android.os.Process.myUserHandle());
                if (found == null || found.size() != 1) {
                    publishedShortcutStatus = "not found";
                } else {
                    publishedShortcutStatus = "found and launched";
                    launcher.startShortcut(found.get(0), null, null);
                }
            } catch (RuntimeException error) {
                publishedShortcutStatus = error.getClass().getSimpleName();
            }
            refreshLabel();
        });
        content.addView(published);
        setContentView(content);
        session = new MediaSession(this, "SlivuePiPFixture");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPause() { updatePlayback(false); }
            @Override public void onPlay() { updatePlayback(true); }
        }, new Handler(Looper.getMainLooper()));
        session.setActive(true);
        applyIntent(getIntent());
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            panelShortcut = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        }
        refreshLabel();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIntent(intent);
    }

    private void applyIntent(Intent intent) {
        pipEntries = 0;
        autoPip = intent.getBooleanExtra("auto_pip", true);
        updatePlayback(intent.getBooleanExtra("playing", true));
    }

    private void updatePlayback(boolean value) {
        playing = value;
        if (Build.VERSION.SDK_INT >= 31) {
            setPictureInPictureParams(new PictureInPictureParams.Builder()
                .setAutoEnterEnabled(autoPip && playing).build());
        }
        session.setPlaybackState(new PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE)
            .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, 0, playing ? 1f : 0f)
            .build());
        refreshLabel();
    }

    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (playing && (Build.VERSION.SDK_INT < 31 || !autoPip)) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }

    @Override public void onPictureInPictureModeChanged(boolean inPip, Configuration config) {
        super.onPictureInPictureModeChanged(inPip, config);
        if (inPip) pipEntries++;
        refreshLabel();
    }

    private void refreshLabel() {
        label.setText("Slivue PiP fixture\n" + (playing ? "PLAYING" : "PAUSED")
            + "\nPiP entries: " + pipEntries
            + "\nShortcut ready: " + (panelShortcut != null)
            + "\nPublished shortcut: " + publishedShortcutStatus);
    }

    @Override protected void onDestroy() {
        session.release();
        super.onDestroy();
    }
}
