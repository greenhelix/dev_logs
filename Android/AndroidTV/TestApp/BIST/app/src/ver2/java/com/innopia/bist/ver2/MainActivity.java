package com.innopia.bist.ver2;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
//import com.innopia.bist.R;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // DrawerLayout 및 ActionBarDrawerToggle 설정
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // NavigationView 아이템 선택 리스너
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // 메뉴 아이템 클릭 이벤트 처리
                drawerLayout.closeDrawers();
                return true;
            }
        });

        // RecyclerView 설정
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4)); // 3열 그리드

        // 샘플 데이터 생성
        List<CardItem> cardItems = new ArrayList<>();
        cardItems.add(new CardItem("Test A"));
        cardItems.add(new CardItem("Test B"));
        cardItems.add(new CardItem("Test C"));
        cardItems.add(new CardItem("Test D"));
        cardItems.add(new CardItem("Test E"));
        cardItems.add(new CardItem("Test F"));
        cardItems.add(new CardItem("Test G"));
        cardItems.add(new CardItem("Test A"));
        cardItems.add(new CardItem("Test B"));
        cardItems.add(new CardItem("Test C"));
        cardItems.add(new CardItem("Test D"));
        cardItems.add(new CardItem("Test E"));
        cardItems.add(new CardItem("Test F"));
        cardItems.add(new CardItem("Test G"));
        cardItems.add(new CardItem("Test A"));
        cardItems.add(new CardItem("Test B"));
        cardItems.add(new CardItem("Test C"));
        cardItems.add(new CardItem("Test D"));
        cardItems.add(new CardItem("Test E"));
        cardItems.add(new CardItem("Test F"));
        cardItems.add(new CardItem("Test G"));
        cardItems.add(new CardItem("Test A"));
        cardItems.add(new CardItem("Test B"));
        cardItems.add(new CardItem("Test C"));
        cardItems.add(new CardItem("Test D"));
        cardItems.add(new CardItem("Test E"));
        cardItems.add(new CardItem("Test F"));
        cardItems.add(new CardItem("Test G"));

        // 어댑터 설정
        cardAdapter = new CardAdapter(cardItems);
        recyclerView.setAdapter(cardAdapter);
    }
}
