package com.innopia.bist.ver2.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;
import com.innopia.bist.ver2.R;
import com.innopia.bist.ver2.ui.fragment.MainFragment;
import com.innopia.bist.ver2.util.OverscanHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // DrawerLayout 설정
        drawerLayout = findViewById(R.id.drawer_layout);
        OverscanHelper.applyOverscanMargins(this, drawerLayout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // NavigationView 설정
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                drawerLayout.closeDrawers();
                return true;
            }
        });

        // 초기 Fragment 로드 (MainFragment)
        if (savedInstanceState == null) {
            loadMainFragment();
        }
    }

    /**
     * MainFragment 로드
     */
    private void loadMainFragment() {
        Fragment fragment = MainFragment.newInstance();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        Log.d(TAG, "MainFragment loaded");
    }

    /**
     * 뒤로가기 처리
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called, backStackCount: "
                + getSupportFragmentManager().getBackStackEntryCount());

        // 1. Drawer가 열려있으면 닫기
        if (drawerLayout.isDrawerOpen(findViewById(R.id.nav_view))) {
            Log.d(TAG, "Closing drawer");
            drawerLayout.closeDrawers();
            return;
        }

        // 2. Fragment 스택이 있으면 뒤로가기
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            Log.d(TAG, "Popping back stack");
            getSupportFragmentManager().popBackStack();
            return;
        }

        // 3. MainFragment에서 Exit Dialog 표시
        Log.d(TAG, "Showing exit dialog");
        showExitDialog();
        super.onBackPressed();
    }

    /**
     * 종료 확인 Dialog
     */
    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit App")
                .setMessage("Do you want to exit the application?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "User chose to exit");
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "User chose to stay");
                        dialog.dismiss();
                    }
                })
                .setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Dialog 버튼에 포커스 설정
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
        }
    }
}
