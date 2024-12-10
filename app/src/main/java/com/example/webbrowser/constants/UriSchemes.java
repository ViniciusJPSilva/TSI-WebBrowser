package com.example.webbrowser.constants;

/**
 * Classe utilitária que define constantes de esquemas de URI utilizados
 * para intents no Android. Estes esquemas são usados para identificar
 * ações específicas, como abrir uma URL, realizar chamadas telefônicas
 * ou enviar e-mails.
 *
 * <p>
 * Esta classe é final para evitar herança e possui um construtor privado
 * para impedir a criação de instâncias.
 * </p>
 *
 * <ul>
 *     <li>{@link #INTENT} - Prefixo para intents genéricos.</li>
 *     <li>{@link #TEL} - Prefixo para chamadas telefônicas.</li>
 *     <li>{@link #MAILTO} - Prefixo para envio de e-mails.</li>
 * </ul>
 *
 * @author Vinicius J P Silva
 */
public final class UriSchemes {

    public static final String INTENT = "intent:";
    public static final String TEL = "tel:";
    public static final String MAILTO = "mailto:";

    private UriSchemes() {}

}
