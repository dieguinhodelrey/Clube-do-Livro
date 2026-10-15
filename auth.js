const API_BASE_URL = window.UNI4READ_API_URL || "http://localhost:8080/api";

let accessToken = null;
let currentUser = null;
let refreshPromise = null;

function setSession(session) {
    accessToken = session.accessToken || accessToken;
    currentUser = session.user;
}

function clearSession() {
    accessToken = null;
    currentUser = null;
}

async function parseError(response) {
    let message = `Erro ${response.status}`;
    try {
        const body = await response.json();
        message = body.message || body.error || message;
    } catch {
        // Respostas sem JSON mantêm a mensagem baseada no status.
    }
    return message;
}

async function refreshSession() {
    if (!refreshPromise) {
        refreshPromise = fetch(`${API_BASE_URL}/auth/refresh`, {
            method: "POST",
            credentials: "include"
        }).then(async response => {
            if (!response.ok) throw new Error(await parseError(response));
            const session = await response.json();
            setSession(session);
            return session;
        }).finally(() => {
            refreshPromise = null;
        });
    }
    return refreshPromise;
}

async function apiRequest(path, options = {}, canRefresh = true) {
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers,
        credentials: "include"
    });

    if (response.status === 401 && canRefresh) {
        try {
            await refreshSession();
            return apiRequest(path, options, false);
        } catch {
            clearSession();
            if (!window.location.pathname.endsWith("login.html")) {
                window.location.href = "login.html";
            }
        }
    }

    if (!response.ok) throw new Error(await parseError(response));
    if (response.status === 204) return null;
    return response.json();
}

async function requireAuth() {
    if (currentUser) return currentUser;
    try {
        const session = await refreshSession();
        return session.user;
    } catch {
        clearSession();
        window.location.href = "login.html";
        throw new Error("Sessão expirada");
    }
}

async function login(email, password) {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ email, password })
    });
    if (!response.ok) throw new Error(await parseError(response));
    const session = await response.json();
    setSession(session);
    return session.user;
}

async function register(name, email, password) {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ name, email, password })
    });
    if (!response.ok) throw new Error(await parseError(response));
    const session = await response.json();
    setSession(session);
    return session.user;
}

async function logout() {
    try {
        await fetch(`${API_BASE_URL}/auth/logout`, {
            method: "POST",
            credentials: "include",
            headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
        });
    } finally {
        clearSession();
        window.location.href = "login.html";
    }
}

window.uni4readAuth = {
    apiRequest,
    login,
    register,
    logout,
    requireAuth,
    get currentUser() {
        return currentUser;
    }
};
