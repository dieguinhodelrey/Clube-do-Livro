const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

function client(fetch) {
    const window = { location: { hostname: "localhost", protocol: "http:", pathname: "/lendo.html", href: "" } };
    const context = vm.createContext({ window, fetch, Headers });
    vm.runInContext(fs.readFileSync(path.join(__dirname, "..", "auth.js"), "utf8"), context);
    return { auth: window.uni4readAuth, window, request: vm.runInContext("apiRequest", context) };
}

function response(status, body) {
    return { ok: status >= 200 && status < 300, status, json: async () => body };
}

const session = { accessToken: "test-token", user: { id: "alice", name: "Alice" } };

test("restores the session through the refresh cookie", async () => {
    const app = client(async (url, options) => {
        assert.ok(url.endsWith("/auth/refresh"));
        assert.equal(options.credentials, "include");
        return response(200, session);
    });
    assert.equal(await app.auth.requireAuth(), session.user);
    assert.equal(app.auth.currentUser, session.user);
    assert.equal(app.window.location.href, "");
});

test("redirects to login when the refresh session is invalid", async () => {
    const app = client(async () => response(401, { message: "Sessao expirada" }));
    await assert.rejects(app.auth.requireAuth(), /Sessao expirada/);
    assert.equal(app.window.location.href, "login.html");
    assert.equal(app.auth.currentUser, null);
});

test("reports network failures without treating them as an expired session", async () => {
    const app = client(async () => { throw new Error("Offline"); });
    await assert.rejects(app.auth.requireAuth(), /Offline/);
    assert.equal(app.window.location.href, "");
});

test("renews once and retries an unauthorized request", async () => {
    let requests = 0;
    const app = client(async (url, options) => {
        if (url.endsWith("/auth/refresh")) return response(200, session);
        requests++;
        if (requests === 1) return response(401, {});
        assert.equal(options.headers.get("Authorization"), "Bearer test-token");
        assert.equal(options.credentials, "include");
        return response(200, []);
    });
    assert.deepEqual(await app.request("/feed"), []);
    assert.equal(requests, 2);
});

test("does not retry indefinitely when the renewed token is rejected", async () => {
    let requests = 0;
    const app = client(async url => {
        requests++;
        return url.endsWith("/auth/refresh") ? response(200, session) : response(401, { message: "Denied" });
    });
    await assert.rejects(app.request("/feed"), /Denied/);
    assert.equal(requests, 3);
    assert.equal(app.window.location.href, "login.html");
    assert.equal(app.auth.currentUser, null);
});

test("failed logout remains visible and does not pretend the session was revoked", async () => {
    const app = client(async url => url.endsWith("/auth/logout")
        ? response(500, { message: "Logout failed" }) : response(200, session));
    await app.auth.requireAuth();
    await assert.rejects(app.auth.logout(), /Logout failed/);
    assert.equal(app.auth.currentUser, session.user);
    assert.equal(app.window.location.href, "");
});

test("successful logout clears the session and returns to login", async () => {
    const app = client(async url => url.endsWith("/auth/logout") ? response(204) : response(200, session));
    await app.auth.requireAuth();
    await app.auth.logout();
    assert.equal(app.auth.currentUser, null);
    assert.equal(app.window.location.href, "login.html");
});

test("all protected pages contain the complete interface and load auth before the feed script", () => {
    for (const name of ["index.html", "feed.html", "lendo.html", "lidos.html"]) {
        const html = fs.readFileSync(path.join(__dirname, "..", name), "utf8");
        for (const id of ["logoutButton", "profileName", "statusClose", "statusLendo", "statusConfirm"]) {
            assert.ok(html.includes(`id="${id}"`), `${name}: missing ${id}`);
        }
        assert.ok(html.indexOf('src="auth.js"') < html.indexOf('src="script.js"'));
        assert.ok(html.trim().endsWith("</html>"), `${name}: incomplete HTML`);
    }
});

test("protected pages expose accessible MVP navigation and use existing images", () => {
    for (const name of ["index.html", "feed.html", "lendo.html", "lidos.html"]) {
        const root = path.join(__dirname, "..");
        const html = fs.readFileSync(path.join(root, name), "utf8");
        assert.ok(!html.includes('href="amigos.html"'), `${name}: friends are outside the MVP`);
        for (const label of ["Feed", "Lendo", "Lidos", "Adicionar livro"]) {
            assert.ok(html.includes(`aria-label="${label}"`), `${name}: missing accessible ${label}`);
        }
        for (const image of html.matchAll(/<img[^>]+src="([^"]+)"/g)) {
            assert.ok(fs.existsSync(path.join(root, image[1])), `${name}: missing image ${image[1]}`);
        }
    }
});
