package com.example.webbrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.webbrowser.database.FavoriteItemDatabase;
import com.example.webbrowser.models.FavoriteItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String  GOOGLE_QUERY = "https://www.google.com/search?q=";

    public WebView webView;
    private String currentSearch;
    private EditText searchEditText;

    private List<FavoriteItem> favoriteList;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        favoriteList = new ArrayList<>();

        webView = findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                checkFavorite();
            }
        });

        searchEditText = findViewById(R.id.searchText);
        searchEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (isEnterKeyPressed(actionId)) {
                search();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        ImageButton buttonSearch = findViewById(R.id.buttonSearch);
        ImageButton buttonBack =  findViewById(R.id.buttonBack);
        ImageButton buttonRefresh = findViewById(R.id.buttonRefresh);
        ImageButton favoritesButton = findViewById(R.id.buttonFavorites);

        buttonSearch.setOnClickListener(e -> search());
        buttonBack.setOnClickListener(e -> back());
        buttonBack.setOnLongClickListener(e -> { forward(); vibratePhone(); return true; });
        buttonRefresh.setOnClickListener(e -> refresh());
        favoritesButton.setOnClickListener(e -> favorites(favoritesButton));
        favoritesButton.setOnLongClickListener(e -> shortcutAddToFavorites());
        
        recoverFavorites();
        search();
    }

    @Override
    protected void onStop() {
        super.onStop();

        persistFavorites();
    }

    private void search() {
        currentSearch = uiGetSearchTextText();
        webView.loadUrl(isValidUrl(currentSearch) ? currentSearch : String.format("%s%s", GOOGLE_QUERY, currentSearch.replace(" ", "+")));
        currentSearch = webView.getUrl();
        uiSetSearchTextText(currentSearch);
    }

    private void refresh() {
        uiSetSearchTextText(currentSearch);
        webView.reload();
    }

    private void back() {
        if(webView.canGoBack())
            webView.goBack();
    }

    private void forward() {
        if(webView.canGoForward())
            webView.goForward();
    }

    private void favorites(ImageButton favoritesButton) {
        PopupMenu favoritesMenu = new PopupMenu(this, favoritesButton);
        refreshFavorites(favoritesMenu);
        favoritesMenu.getMenuInflater().inflate(R.menu.favorites_menu, favoritesMenu.getMenu());
        favoritesMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.addToFavoritesItem)
                addFavoriteToMenu(favoritesMenu, generateAndSaveWebsiteFavorite());
            else if(item.getItemId() == R.id.removeFromFavoritesItem)
                removeFavoriteFromMenu();
            else
                searchFavorite(item);

            return true;
        });
        favoritesMenu.show();
    }

    private void addFavoriteToMenu(PopupMenu menu, FavoriteItem item) {
        if(menu == null || item == null) return;
        menu.getMenu().add(0, favoriteList.size() + 1, favoriteList.size() + 1, item.getTitle());
        checkFavorite();
    }

    private void removeFavoriteFromMenu() {
        if(favoriteList.remove(new FavoriteItem(webView.getTitle(), webView.getUrl())))
            Toast.makeText(this, R.string.msg_remove_favorite, Toast.LENGTH_SHORT).show();
        checkFavorite();
    }

    private boolean shortcutAddToFavorites() {
        if(generateAndSaveWebsiteFavorite() != null) {
            Toast.makeText(this, R.string.msg_add_favorite, Toast.LENGTH_SHORT).show();
            checkFavorite();
        } else
            removeFavoriteFromMenu();
        return true;
    }

    private void recoverFavorites() {
        try (FavoriteItemDatabase favoriteItemDatabase = new FavoriteItemDatabase(this)) {
            favoriteList = favoriteItemDatabase.getAll();
        }
    }

    private void persistFavorites() {
        try (FavoriteItemDatabase favoriteItemDatabase = new FavoriteItemDatabase(this)) {
            favoriteItemDatabase.clear();
            favoriteItemDatabase.create();
            for(FavoriteItem item : favoriteList)
                favoriteItemDatabase.insert(item);
        }
    }

    private void checkFavorite() {
        ImageButton button = findViewById(R.id.buttonFavorites);
        button.setImageResource((favoriteList.contains(new FavoriteItem(webView.getTitle(), webView.getUrl()))) ? R.drawable.full_star_img : R.drawable.star_img);
    }

    private FavoriteItem generateAndSaveWebsiteFavorite() {
        FavoriteItem item = new FavoriteItem(webView.getTitle(), webView.getUrl());
        if(favoriteList.contains(item)) return null;
        favoriteList.add(item);
        return item;
    }

    private void searchFavorite(MenuItem item) {
        FavoriteItem favoriteItem = favoriteList.get(item.getItemId() - 1);
        uiSetSearchTextText(favoriteItem.getUrl());
        search();
    }

    private void refreshFavorites(PopupMenu menu) {
        for(FavoriteItem item : favoriteList)
            menu.getMenu().add(0, favoriteList.indexOf(item) + 1, favoriteList.indexOf(item) + 1, item.getTitle());
    }

    private boolean isValidUrl(String url) {
        return android.util.Patterns.WEB_URL.matcher(url).matches();
    }

    private boolean isEnterKeyPressed(int actionId) {
        return actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT;
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator())
            vibrator.vibrate(500);
    }

    private String uiGetSearchTextText() {
        return searchEditText.getText().toString();
    }

    private void uiSetSearchTextText(String text) {
        searchEditText.setText(text);
    }

}