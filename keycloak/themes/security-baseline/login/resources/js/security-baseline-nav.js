(function () {
  const frontendUrl = 'http://localhost:5173';
  const labels = {
    en: 'Back to Security Baseline',
    es: 'Volver a Security Baseline'
  };

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

  function sanitizePrompt(url) {
    if (url.searchParams.get('prompt') === 'create') {
      url.searchParams.delete('prompt');
    }
    return url;
  }

  function decorateLoginLinks() {
    const candidates = document.querySelectorAll(
      '#kc-back-to-login, .back-link, a[href*="login-actions/authenticate"]'
    );
    candidates.forEach((anchor) => {
      try {
        const url = sanitizePrompt(new URL(anchor.href, window.location.origin));
        anchor.href = url.toString();
      } catch (_error) {
      }
    });
  }

  function decorateGoogleLogin() {
    const candidates = document.querySelectorAll(
      '#kc-social-providers a[id*="google"], #kc-social-providers a[href*="/google"], #kc-social-providers a[href*="google"]'
    );
    candidates.forEach((anchor) => {
      try {
        const url = sanitizePrompt(new URL(anchor.href, window.location.origin));
        if (!url.searchParams.has('prompt')) {
          url.searchParams.set('prompt', 'select_account');
        }
        anchor.href = url.toString();
      } catch (_error) {
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      applyIntentClasses();
      addBackLink();
      decorateLoginLinks();
      decorateGoogleLogin();
    }, { once: true });
  } else {
    applyIntentClasses();
    addBackLink();
    decorateLoginLinks();
    decorateGoogleLogin();
  }
})();
