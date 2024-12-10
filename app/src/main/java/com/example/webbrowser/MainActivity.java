package com.example.webbrowser;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.webbrowser.constants.UriSchemes;
import com.example.webbrowser.database.FavoriteItemDatabase;
import com.example.webbrowser.models.FavoriteItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe principal da aplicação Android que implementa um navegador WebView com suporte a favoritos.
 * Permite buscas na web, navegação entre páginas, gerenciamento de favoritos e tratamento de erros
 * durante o carregamento de páginas. Inclui suporte à interface moderna com Edge-to-Edge e ajustes
 * para a experiência do usuário.
 * </br></br>
 * Funcionalidades principais:
 * </br>- Busca de páginas na web através de uma barra de pesquisa.
 * </br>- Verificação e gerenciamento de favoritos com persistência local.
 * </br>- Tratamento de erros durante o carregamento de páginas, exibindo uma tela de erro quando necessário.
 * </br>- Navegação no histórico de páginas (voltar e avançar).
 * </br>- Atualização da página atual.
 * </br>- Suporte a interações com atalhos e feedback tátil.
 *
 * @author Vinicius J P Silva
 */
public class MainActivity extends AppCompatActivity {

    public static final String  GOOGLE_QUERY = "https://www.google.com/search?q=";
    public static final int VIBRATE_TIME = 500;

    public WebView webView;
    ConstraintLayout errorView;
    private String currentSearch;
    private EditText searchEditText;

    private boolean hasErrorOccurred = false;

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

        errorView = findViewById(R.id.errorWebViewLayout);

        webView = findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                turnWebViewVisible(!hasErrorOccurred);
                checkFavorite();
                hasErrorOccurred = false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (handleMailToLink(view, url)) return true;
                if (handleTelLink(view, url)) return true;
                return handleIntentLink(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                hasErrorOccurred = true;
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                hasErrorOccurred = true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                hasErrorOccurred = true;
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

    /**
     * Realiza a busca no WebView utilizando o texto informado pelo usuário. Caso o texto não seja uma URL válida,
     * utiliza o mecanismo de busca do Google para realizar a pesquisa.
     */
    private void search() {
        currentSearch = uiGetSearchTextText();
        webView.loadUrl(isValidUrl(currentSearch) ? currentSearch : String.format("%s%s", GOOGLE_QUERY, currentSearch.replace(" ", "+")));
        currentSearch = webView.getUrl();
        uiSetSearchTextText(currentSearch);
    }

    /**
     * Atualiza a página atual no WebView e redefine o texto de busca com a URL atual.
     */
    private void refresh() {
        uiSetSearchTextText(currentSearch);
        webView.reload();
    }

    /**
     * Navega para a página anterior no histórico do WebView, se disponível.
     */
    private void back() {
        if(webView.canGoBack())
            webView.goBack();
    }

    /**
     * Navega para a próxima página no histórico do WebView, se disponível.
     */
    private void forward() {
        if(webView.canGoForward())
            webView.goForward();
    }

    /**
     * Exibe um menu de favoritos, permitindo adicionar, remover ou selecionar favoritos.
     *
     * @param favoritesButton O botão que invocará o menu de favoritos.
     */
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

    /**
     * Adiciona um item ao menu de favoritos.
     *
     * @param menu O menu onde o item será adicionado.
     * @param item O item favorito a ser adicionado.
     */
    private void addFavoriteToMenu(PopupMenu menu, FavoriteItem item) {
        if(menu == null || item == null) return;
        menu.getMenu().add(0, favoriteList.size() + 1, favoriteList.size() + 1, item.getTitle());
        checkFavorite();
    }

    /**
     * Remove o favorito correspondente à página atual do WebView.
     */
    private void removeFavoriteFromMenu() {
        if(favoriteList.remove(new FavoriteItem(webView.getTitle(), webView.getUrl())))
            Toast.makeText(this, R.string.msg_remove_favorite, Toast.LENGTH_SHORT).show();
        checkFavorite();
    }

    /**
     * Atalho para adicionar ou remover a página atual como favorito.
     *
     * @return {@code true} se a operação foi realizada.
     */
    private boolean shortcutAddToFavorites() {
        if(generateAndSaveWebsiteFavorite() != null) {
            Toast.makeText(this, R.string.msg_add_favorite, Toast.LENGTH_SHORT).show();
            checkFavorite();
        } else
            removeFavoriteFromMenu();
        return true;
    }

    /**
     * Recupera a lista de favoritos do banco de dados local.
     */
    private void recoverFavorites() {
        try (FavoriteItemDatabase favoriteItemDatabase = new FavoriteItemDatabase(this)) {
            favoriteList = favoriteItemDatabase.getAll();
        }
    }

    /**
     * Salva a lista de favoritos no banco de dados local.
     */
    private void persistFavorites() {
        try (FavoriteItemDatabase favoriteItemDatabase = new FavoriteItemDatabase(this)) {
            favoriteItemDatabase.clear();
            favoriteItemDatabase.create();
            for(FavoriteItem item : favoriteList)
                favoriteItemDatabase.insert(item);
        }
    }

    /**
     * Atualiza o ícone do botão de favoritos com base na presença da página atual na lista de favoritos.
     */
    private void checkFavorite() {
        ImageButton button = findViewById(R.id.buttonFavorites);
        button.setImageResource((favoriteList.contains(new FavoriteItem(webView.getTitle(), webView.getUrl()))) ? R.drawable.full_star_img : R.drawable.star_img);
    }

    /**
     * Gera um item favorito com as informações da página atual e o adiciona à lista de favoritos.
     *
     * @return O item gerado ou {@code null} se já existir na lista.
     */
    private FavoriteItem generateAndSaveWebsiteFavorite() {
        FavoriteItem item = new FavoriteItem(webView.getTitle(), webView.getUrl());
        if(favoriteList.contains(item)) return null;
        favoriteList.add(item);
        return item;
    }

    /**
     * Realiza a busca com base no item selecionado no menu de favoritos.
     *
     * @param item O item selecionado.
     */
    private void searchFavorite(MenuItem item) {
        FavoriteItem favoriteItem = favoriteList.get(item.getItemId() - 1);
        uiSetSearchTextText(favoriteItem.getUrl());
        search();
    }

    /**
     * Atualiza o menu de favoritos com a lista de favoritos atual.
     *
     * @param menu O menu de favoritos a ser atualizado.
     */
    private void refreshFavorites(PopupMenu menu) {
        for(FavoriteItem item : favoriteList)
            menu.getMenu().add(0, favoriteList.indexOf(item) + 1, favoriteList.indexOf(item) + 1, item.getTitle());
    }

    /**
     * Verifica se a URL fornecida é válida.
     *
     * @param url A URL a ser validada.
     * @return {@code true} se a URL for válida, {@code false} caso contrário.
     */
    private boolean isValidUrl(String url) {
        return android.util.Patterns.WEB_URL.matcher(url).matches();
    }

    /**
     * Verifica se a tecla Enter foi pressionada com base na ação recebida.
     *
     * @param actionId O ID da ação do teclado.
     * @return {@code true} se a tecla Enter foi pressionada, {@code false} caso contrário.
     */
    private boolean isEnterKeyPressed(int actionId) {
        return actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT;
    }

    /**
     * Faz o dispositivo vibrar, se suportado.
     */
    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator())
            vibrator.vibrate(VIBRATE_TIME);
    }

    /**
     * Alterna a visibilidade do WebView e da visão de erro.
     *
     * @param isVisible {@code true} para tornar o WebView visível, {@code false} para exibir a view de erro.
     */
    private void turnWebViewVisible(boolean isVisible) {
        if(isVisible) {
            webView.setVisibility(View.VISIBLE);
            errorView.setVisibility(View.INVISIBLE);
        } else {
            webView.setVisibility(View.INVISIBLE);
            errorView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Manipula links do tipo "mailto:".
     * <p>
     * Este método trata URLs que iniciam com "mailto:" e tenta abrir o aplicativo de e-mail
     * padrão para compor um e-mail. Caso nenhum aplicativo de e-mail esteja disponível,
     * exibe uma mensagem de erro ao usuário.
     * </p>
     * @param view A WebView que disparou a solicitação.
     * @param url  A URL sendo carregada.
     * @return {@code true} se o link foi tratado, {@code false} caso contrário.
     */
    private boolean handleMailToLink(WebView view, String url) {
        if (url.startsWith(UriSchemes.MAILTO)) {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse(url));
            try {
                view.getContext().startActivity(emailIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.msg_handle_email, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    /**
     * Manipula links do tipo "tel:".
     * <p>
     * Este método trata URLs que iniciam com "tel:" e tenta abrir o discador do dispositivo
     * para iniciar uma chamada telefônica. Caso o discador não esteja disponível,
     * exibe uma mensagem de erro ao usuário.
     * </p>
     * @param view A WebView que disparou a solicitação.
     * @param url  A URL sendo carregada.
     * @return {@code true} se o link foi tratado, {@code false} caso contrário.
     */
    private boolean handleTelLink(WebView view, String url) {
        if (url.startsWith(UriSchemes.TEL)) {
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            phoneIntent.setData(Uri.parse(url));
            try {
                view.getContext().startActivity(phoneIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.msg_handle_tel, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    /**
     * Manipula links genéricos do tipo Intent.
     * <p>
     * Este método trata URLs que iniciam com "intent:" e exibe uma mensagem informativa
     * para o usuário. Não realiza nenhuma ação adicional, mas pode ser expandido
     * para tratar intents genéricos no futuro.
     * </p>
     * @param url  A URL sendo carregada.
     * @return {@code true} se o link foi tratado, {@code false} caso contrário.
     */
    private boolean handleIntentLink(String url) {
        if (url.startsWith(UriSchemes.INTENT)) {
            Toast.makeText(this, R.string.msg_handle_itent, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    /**
     * Obtém o texto atual inserido na barra de busca.
     *
     * @return O texto da barra de busca.
     */
    private String uiGetSearchTextText() {
        return searchEditText.getText().toString();
    }

    /**
     * Define o texto na barra de busca.
     *
     * @param text O texto a ser exibido na barra de busca.
     */
    private void uiSetSearchTextText(String text) {
        searchEditText.setText(text);
    }

}