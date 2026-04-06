// /static/js/auth.js
(function (global) {
  const ACCESS_KEY = "accessToken";
  const REFRESH_KEY = "refreshToken";
  const NICKNAME_KEY = "userNickname";
  let refreshPromise = null;

  function getAccessToken() { return localStorage.getItem(ACCESS_KEY); }
  function getRefreshToken() { return localStorage.getItem(REFRESH_KEY); }
  function getNickname() { return localStorage.getItem(NICKNAME_KEY); }

  function emitAuthChange() {
    global.dispatchEvent(new CustomEvent("auth:changed", {
      detail: {
        loggedIn: isLoggedIn(),
        nickname: getNickname(),
      },
    }));
  }

  function setTokens(accessToken, refreshToken, nickname) {
    if (accessToken) localStorage.setItem(ACCESS_KEY, accessToken);
    if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
    if (nickname !== undefined) {
      if (nickname === null || nickname === "") localStorage.removeItem(NICKNAME_KEY);
      else localStorage.setItem(NICKNAME_KEY, nickname);
    }
    updateHeaderAuthUI();
    emitAuthChange();
  }

  function clearTokens() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(NICKNAME_KEY);
    updateHeaderAuthUI();
    emitAuthChange();
  }

  async function refreshAccessToken() {
    if (refreshPromise) {
      return refreshPromise;
    }

    const refreshToken = getRefreshToken();
    if (!refreshToken) throw new Error("No refresh token");

    refreshPromise = (async function () {
      const res = await fetch("/auth/refresh", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });

      if (!res.ok) {
        clearTokens();
        throw new Error("Refresh failed");
      }

      const data = await res.json();
      if (!data.refreshToken) {
        clearTokens();
        throw new Error("Refresh rotation failed: no refreshToken returned");
      }

      setTokens(data.accessToken, data.refreshToken, data.nickname);
      return data.accessToken;
    })();

    try {
      return await refreshPromise;
    } finally {
      refreshPromise = null;
    }
  }

  // 자동 Authorization + 401이면 refresh 후 1회 재시도
  async function authFetch(url, options = {}) {
    const opts = { ...options };
    opts.headers = { ...(options.headers || {}) };

    const accessToken = getAccessToken();
    if (accessToken) opts.headers["Authorization"] = "Bearer " + accessToken;

    let res = await fetch(url, opts);

    if (res.status === 401 && getRefreshToken()) {
      try {
        const newAccess = await refreshAccessToken();
        opts.headers["Authorization"] = "Bearer " + newAccess;
        res = await fetch(url, opts);
      } catch (error) {
        return res;
      }
    }

    return res;
  }

  function isLoggedIn() {
    return !!getAccessToken() && !!getRefreshToken();
  }

  function getLangFromUrl() {
    const u = new URL(window.location.href);
    return u.searchParams.get("lang") || document.documentElement.lang || "en";
  }

  function goToLogin() {
    const lang = getLangFromUrl();
    if (global.PageSkeleton && typeof global.PageSkeleton.show === "function") {
      global.PageSkeleton.show();
    }
    window.location.href = "/auth/login?lang=" + encodeURIComponent(lang);
  }
  
  function goToSignup() {
    const lang = getLangFromUrl();
    if (global.PageSkeleton && typeof global.PageSkeleton.show === "function") {
      global.PageSkeleton.show();
    }
    window.location.href = "/auth/signup?lang=" + encodeURIComponent(lang);
  }

  function isProtectedRoute() {
    const pathname = window.location.pathname.replace(/\/+$/, "") || "/";
    return pathname === "/me" || pathname.startsWith("/me/");
  }

  function sendLogoutRequest(refreshToken) {
    const payload = JSON.stringify({ refreshToken });

    if (navigator.sendBeacon) {
      const body = new Blob([payload], { type: "application/json" });
      if (navigator.sendBeacon("/auth/logout", body)) {
        return;
      }
    }

    fetch("/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: payload,
      keepalive: true,
    }).catch(() => {});
  }

  function finalizeLogoutNavigation() {
    if (isProtectedRoute()) {
      if (global.PageSkeleton && typeof global.PageSkeleton.show === "function") {
        global.PageSkeleton.show();
      }
      const lang = getLangFromUrl();
      window.location.replace("/?lang=" + encodeURIComponent(lang));
    }
  }

  function logout() {
    const refreshToken = getRefreshToken();

    clearTokens();

    if (refreshToken) {
      sendLogoutRequest(refreshToken);
    }

    finalizeLogoutNavigation();
  }

  // 헤더 UI 업데이트 (header.html에 넣을 id를 기준으로 동작)
  function updateHeaderAuthUI() {
    const box = document.getElementById("auth-box");
    if (!box) return;

    const loginBtn = box.querySelector('[data-auth="login"]');
    const signupBtn = box.querySelector('[data-auth="signup"]'); // ✅ 추가
    const logoutBtn = box.querySelector('[data-auth="logout"]');
    const statusText = box.querySelector('[data-auth="status"]');
    const guestActions = box.querySelector('[data-auth="guest-actions"]');
    const userPanel = box.querySelector('[data-auth="user-panel"]');

    if (isLoggedIn()) {
      const currentNickname = getNickname() || "User";
      if (statusText) statusText.textContent = currentNickname;
      if (guestActions) guestActions.style.display = "none";
      if (userPanel) userPanel.hidden = false;

      if (loginBtn) loginBtn.style.display = "none";
      if (signupBtn) signupBtn.style.display = "none";
      if (logoutBtn) logoutBtn.style.display = "inline-flex";
    } else {
      if (statusText) statusText.textContent = "";
      if (guestActions) guestActions.style.display = "inline-flex";
      if (userPanel) userPanel.hidden = true;
      if (loginBtn) loginBtn.style.display = "inline-flex";
      if (signupBtn) signupBtn.style.display = "inline-flex";
      if (logoutBtn) logoutBtn.style.display = "none";
    }

    if (loginBtn) loginBtn.onclick = () => Auth.goToLogin();
    if (signupBtn) signupBtn.onclick = () => Auth.goToSignup();
    if (logoutBtn) logoutBtn.onclick = () => Auth.logout();
  }

  // 전역 노출
  global.Auth = {
    getAccessToken,
    getRefreshToken,
    setTokens,
    clearTokens,
    refreshAccessToken,
    authFetch,
    isLoggedIn,
    logout,
    goToLogin,
    goToSignup,
    updateHeaderAuthUI,
  };

  document.addEventListener("DOMContentLoaded", updateHeaderAuthUI);
})(window);
