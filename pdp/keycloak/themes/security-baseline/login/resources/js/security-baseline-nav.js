(function () {
    const frontendUrl = 'http://localhost:5173';

    // DEV y PROD comparten un único realm/cliente con dos backends distintos (uno por ambiente), así
    // que no hay una sola "backendUrl" fija que sirva para los tres contextos (local, DEV, PROD). En
    // vez de hardcodearla, se deriva del parámetro client_data que Keycloak ya adjunta a cada enlace
    // de esta página: viene del OAuth2AuthorizationRequest original y trae el redirect_uri exacto que
    // el BFF que inició este flujo le pasó a Keycloak.
    function currentBackendOrigin() {
        try {
            const raw = new URLSearchParams(window.location.search).get('client_data');
            if (!raw) return null;
            const base64 = raw.replace(/-/g, '+').replace(/_/g, '/').padEnd(raw.length + (4 - (raw.length % 4)) % 4, '=');
            const decoded = JSON.parse(atob(base64));
            return new URL(decoded.ru).origin;
        } catch (_error) {
            return null;
        }
    }

    const backendOrigin = currentBackendOrigin();
    const bffLoginUrl = backendOrigin ? backendOrigin + '/oauth2/authorization/keycloak' : null;
    const bffRegisterUrl = backendOrigin ? backendOrigin + '/oauth2/authorization/keycloak/register' : null;

    const labels = {
        en: 'Back to Security Baseline',
        es: 'Volver a Security Baseline'
    };

    // El tema solo tiene paleta clara. Keycloak puede activar pf-v5-theme-dark según el sistema
    // operativo del visitante; sin esto, decenas de variables PatternFly quedan sin sobrescribir y
    // se ven inconsistentes (bordes, estados de foco, textos de ayuda). Corre antes del primer
    // pintado porque el <script> vive en <head> y <html> ya existe cuando se parsea.
    document.documentElement.classList.remove('pf-v5-theme-dark');

    function label() {
        const language = document.documentElement.lang || 'en';
        return labels[language.startsWith('es') ? 'es' : 'en'];
    }

    function addBackLink() {
        if (document.querySelector('.sb-back-link')) return;
        const anchor = document.createElement('a');
        anchor.className = 'sb-back-link';
        anchor.href = frontendUrl;
        anchor.textContent = label();

        const header = document.querySelector('#kc-header-wrapper');
        if (header?.parentElement) {
            header.parentElement.insertBefore(anchor, header);
            return;
        }

        const title = document.querySelector('#kc-page-title');
        if (title?.parentElement) {
            title.parentElement.insertBefore(anchor, title);
        }
    }

    function isRegistrationView() {
        const path = window.location.pathname || '';
        if (path.includes('login-actions/registration')) {
            return true;
        }
        if (document.querySelector('#password-confirm, input[name="password-confirm"], input[name="password-new"]')) {
            return true;
        }
        const title = document.querySelector('#kc-page-title')?.textContent?.trim()?.toLowerCase() || '';
        return title === 'create account' || title === 'crea tu cuenta';
    }

    function applyIntentClasses() {
        document.documentElement.classList.toggle('sb-register-view', isRegistrationView());
    }

    // Las páginas de Keycloak (registro, "back to sign in") navegan entre sí sin volver a pasar por
    // el BFF, así que la intención LOGIN/REGISTER que guarda OidcFlowStateService en la sesión del
    // BFF queda desactualizada: si el usuario abre "Crear cuenta" y luego "Back to sign in" para
    // entrar con Google, el backend seguía viendo REGISTER y cerraba la sesión como si acabara de
    // registrarse. La corrección: estos enlaces vuelven a pasar siempre por el BFF, que reescribe la
    // intención correcta antes de reenviar a Keycloak.
    function redirectThroughBff(selector, href) {
        document.querySelectorAll(selector).forEach((anchor) => {
            anchor.href = href;
        });
    }

    function decorateFlowLinks() {
        // Sin client_data no hay forma segura de saber a qué backend volver; se deja el comportamiento
        // por defecto de Keycloak en vez de arriesgar un enlace roto.
        if (!bffLoginUrl || !bffRegisterUrl) return;
        redirectThroughBff('#kc-back-to-login, .back-link, a[href*="login-actions/authenticate"]', bffLoginUrl);
        redirectThroughBff('#kc-registration a, a[href*="login-actions/registration"]', bffRegisterUrl);
    }

    function decorateGoogleLogin() {
        const candidates = document.querySelectorAll(
            '#kc-social-providers a[id*="google"], #kc-social-providers a[href*="/google"], #kc-social-providers a[href*="google"]'
        );
        candidates.forEach((anchor) => {
            try {
                const url = new URL(anchor.href, window.location.origin);
                if (url.searchParams.get('prompt') === 'create') {
                    url.searchParams.delete('prompt');
                }
                if (!url.searchParams.has('prompt')) {
                    url.searchParams.set('prompt', 'select_account');
                }
                anchor.href = url.toString();
            } catch (_error) {
            }
        });
    }

    function init() {
        applyIntentClasses();
        addBackLink();
        decorateFlowLinks();
        decorateGoogleLogin();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init, {once: true});
    } else {
        init();
    }
})();
