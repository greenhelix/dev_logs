package com.innopia.bist.ver2.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.service.OsdOverlayService;
import com.innopia.bist.ver2.ui.fragment.MainFragment;
import com.innopia.bist.ver2.util.OsdManager;
import com.innopia.bist.ver2.util.SecretCodeDetector;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private SecretCodeDetector secretCodeDetector;
    private SwitchCompat osdSwitch; // ⭐ 스위치 위젯 직접 참조

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupSecretCodeDetector();
        setupOsdSwitch(); // ⭐ 스위치 설정 메서드 호출

        if (savedInstanceState == null) {
            loadMainFragment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ⭐ 앱으로 돌아올 때 실제 서비스 상태와 스위치 상태를 동기화
        syncOsdState();
    }

    private void setupSecretCodeDetector() {
        secretCodeDetector = new SecretCodeDetector(() -> runOnUiThread(() -> {
            Toast.makeText(this, "Secret code activated!", Toast.LENGTH_SHORT).show();
            toggleOsd();
        }));
    }

    /**
     * ⭐ OSD 스위치 설정 및 리스너 연결
     */
    private void setupOsdSwitch() {
        MenuItem osdMenuItem = navigationView.getMenu().findItem(R.id.nav_osd_toggle_switch);
        osdSwitch = (SwitchCompat) osdMenuItem.getActionView().findViewById(R.id.menu_switch);

        if (osdSwitch != null) {
            osdSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // 사용자가 직접 스위치를 눌렀을 때만 토글 실행
                if (buttonView.isPressed()) {
                    toggleOsd();
                }
            });
        }
    }

    /**
     * ⭐ 실제 서비스 상태와 스위치 UI를 동기화
     */
    private void syncOsdState() {
        boolean isRunning = OsdManager.isServiceRunning(this, OsdOverlayService.class);
        if (osdSwitch != null && osdSwitch.isChecked() != isRunning) {
            osdSwitch.setChecked(isRunning);
        }
    }

    /**
     * ⭐ OSD 서비스 토글 로직
     */
    private void toggleOsd() {
        if (!OsdManager.canDrawOverlays(this)) {
            startActivity(OsdManager.getOverlayPermissionIntent(this));
            Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_LONG).show();
            // 권한 요청 후 스위치 상태를 원래대로 되돌림
            new Handler(Looper.getMainLooper()).postDelayed(this::syncOsdState, 100);
            return;
        }

        boolean isRunning = OsdManager.isServiceRunning(this, OsdOverlayService.class);

        if (isRunning) {
            OsdManager.stopOsdService(this);
            Toast.makeText(this, "OSD Disabled", Toast.LENGTH_SHORT).show();
        } else {
            OsdManager.startOsdService(this);
            Toast.makeText(this, "OSD Enabled", Toast.LENGTH_SHORT).show();
        }

        // 서비스 상태가 반영될 때까지 약간의 딜레이 후 UI 업데이트
        new Handler(Looper.getMainLooper()).postDelayed(this::syncOsdState, 500);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {

            secretCodeDetector.onKeyPressed(keyCode);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void loadMainFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, MainFragment.newInstance())
                .commit();
        navigationView.setCheckedItem(R.id.nav_home);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            loadMainFragment();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show();
        }
        // 스위치가 있는 메뉴는 onNavigationItemSelected에서 처리하지 않음

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
