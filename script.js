const feed = document.querySelector(".feed");
const activities = [];

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function statusLabel(status) {
    return status === "READ" ? "LIDO" : "LENDO";
}

function statusDescription(status) {
    return status === "READ" ? "leu" : "está lendo";
}

function statusClass(status) {
    return status === "READ" ? "read" : "reading";
}

function renderLoading() {
    feed.innerHTML = '<div class="empty-state"><p>Carregando feed...</p></div>';
}

function renderError(error) {
    feed.innerHTML = `<div class="empty-state"><h2>Não foi possível carregar o feed</h2><p>${escapeHtml(error.message)}</p></div>`;
}

function renderEmpty() {
    feed.innerHTML = '<div class="empty-state"><h2>Nenhuma atividade encontrada</h2><p>Adicione um livro para começar.</p></div>';
}

function gerarCapa(book) {
    const cover = document.createElement("div");
    cover.className = "book-cover generated-cover";
    cover.textContent = book.title;
    return cover;
}

function iniciaisDoNome(name) {
    const partes = String(name || "?").trim().split(/\s+/).filter(Boolean);
    if (partes.length === 0) return "?";
    if (partes.length === 1) return partes[0].charAt(0).toUpperCase();
    return `${partes[0].charAt(0)}${partes[1].charAt(0)}`.toUpperCase();
}

function criarCard(activity) {
    const card = document.createElement("article");
    card.className = `feed-card ${activity.status === "READ" ? "read-card" : ""}`;
    card.dataset.id = activity.id;
    const isOwner = activity.user.id === window.uni4readAuth.currentUser.id;

    const rating = activity.rating
        ? `<div class="rating-display"><span class="stars">${"★".repeat(activity.rating)}${"☆".repeat(5 - activity.rating)}</span><span>${activity.rating}/5</span></div>`
        : "";
    const likeSelected = activity.currentUserReaction === "LIKE" ? "selected" : "";
    const dislikeSelected = activity.currentUserReaction === "DISLIKE" ? "selected" : "";

    card.innerHTML = `
        <div class="card-header">
            <div class="user-info">
                <div class="avatar">${escapeHtml(iniciaisDoNome(activity.user.name))}</div>
                <div><strong>${escapeHtml(activity.user.name)}</strong><span>${statusDescription(activity.status)}</span></div>
            </div>
            ${isOwner ? `<div class="card-action-wrapper">
                <button class="card-action" type="button" aria-label="Ações">⋮</button>
                <div class="card-menu">
                    <button type="button" class="card-menu-item" data-action="alterar-status">Alterar status</button>
                    <button type="button" class="card-menu-item danger" data-action="excluir">Excluir livro</button>
                </div>
            </div>` : ""}
        </div>
        <div class="book-content">
            <div class="book-cover-slot"></div>
            <div class="book-info">
                <span class="status-badge ${statusClass(activity.status)}">${statusLabel(activity.status)}</span>
                <h2>${escapeHtml(activity.book.title)}</h2>
                <p>${escapeHtml(activity.book.author)}</p>
                ${rating}
            </div>
        </div>
        <div class="card-footer">
            <button class="reaction-button ${likeSelected}" data-reaction="LIKE" type="button" aria-label="Curtir">👍 <span>${activity.likes}</span></button>
            <button class="reaction-button ${dislikeSelected}" data-reaction="DISLIKE" type="button" aria-label="Não curtir">👎 <span>${activity.dislikes}</span></button>
        </div>
    `;

    card.querySelector(".book-cover-slot").replaceWith(gerarCapa(activity.book));
    return card;
}

function renderFeed() {
    feed.innerHTML = "";
    if (activities.length === 0) {
        renderEmpty();
        return;
    }
    activities.forEach(activity => feed.appendChild(criarCard(activity)));
}

async function carregarFeed(status = null) {
    renderLoading();
    try {
        const params = new URLSearchParams();
        if (status) params.set("status", status);
        const response = await apiRequest(`/feed?${params}`);
        activities.splice(0, activities.length, ...response);
        renderFeed();
    } catch (error) {
        renderError(error);
    }
}

async function alternarReacao(activityId, type) {
    const updated = await apiRequest(`/feed/${activityId}/reacao`, {
        method: "POST",
        body: JSON.stringify({ type })
    });
    const activity = activities.find(item => item.id === activityId);
    if (activity) {
        activity.likes = updated.likes;
        activity.dislikes = updated.dislikes;
        activity.currentUserReaction = updated.userReaction;
    }
    renderFeed();
}

const addBookButton = document.querySelector(".add-book-button");
const addBookOverlay = document.getElementById("addBookOverlay");
const addBookClose = document.getElementById("addBookClose");
const addBookCancel = document.getElementById("addBookCancel");
const addBookForm = document.getElementById("addBookForm");

function abrirModal() {
    addBookOverlay.classList.add("open");
    document.getElementById("fieldTitulo").focus();
}

function fecharModal() {
    addBookOverlay.classList.remove("open");
    addBookForm.reset();
}

addBookButton.addEventListener("click", abrirModal);
addBookClose.addEventListener("click", fecharModal);
addBookCancel.addEventListener("click", fecharModal);
addBookOverlay.addEventListener("click", event => {
    if (event.target === addBookOverlay) fecharModal();
});

addBookForm.addEventListener("submit", async event => {
    event.preventDefault();
    const title = document.getElementById("fieldTitulo").value.trim();
    const author = document.getElementById("fieldAutor").value.trim();
    const status = document.getElementById("fieldStatus").value === "lido" ? "READ" : "READING";
    if (!title || !author) return;

    try {
        const book = await apiRequest("/books", {
            method: "POST",
            body: JSON.stringify({ title, author })
        });
        await apiRequest("/feed", {
            method: "POST",
            body: JSON.stringify({ bookId: book.id, status })
        });
        fecharModal();
        await carregarFeed();
    } catch (error) {
        renderError(error);
    }
});

const statusOverlay = document.getElementById("statusOverlay");
const statusClose = document.getElementById("statusClose");
const statusCancel = document.getElementById("statusCancel");
const statusConfirm = document.getElementById("statusConfirm");
const statusLendoBtn = document.getElementById("statusLendo");
const statusLidoBtn = document.getElementById("statusLido");
const ratingSection = document.getElementById("ratingSection");
const starPicker = document.getElementById("starPicker");

let idAtividadeEmEdicao = null;
let statusSelecionado = null;
let notaSelecionada = null;

function atualizarVisualStatus() {
    statusLendoBtn.classList.toggle("selected", statusSelecionado === "READING");
    statusLidoBtn.classList.toggle("selected", statusSelecionado === "READ");
    ratingSection.classList.toggle("visible", statusSelecionado === "READ");
    statusLendoBtn.disabled = statusSelecionado === "READ";
}

function atualizarVisualEstrelas() {
    starPicker.querySelectorAll(".star-button").forEach(button => {
        button.classList.toggle("active", notaSelecionada !== null && Number(button.dataset.value) <= notaSelecionada);
    });
}

function abrirModalStatus(id) {
    const activity = activities.find(item => item.id === id);
    if (!activity) return;
    idAtividadeEmEdicao = id;
    statusSelecionado = activity.status;
    notaSelecionada = activity.rating || null;
    atualizarVisualStatus();
    atualizarVisualEstrelas();
    statusOverlay.classList.add("open");
}

function fecharModalStatus() {
    statusOverlay.classList.remove("open");
    idAtividadeEmEdicao = null;
}

statusLendoBtn.addEventListener("click", () => {
    statusSelecionado = "READING";
    atualizarVisualStatus();
});

statusLidoBtn.addEventListener("click", () => {
    statusSelecionado = "READ";
    atualizarVisualStatus();
});

starPicker.addEventListener("click", event => {
    const button = event.target.closest(".star-button");
    if (!button) return;
    notaSelecionada = Number(button.dataset.value);
    atualizarVisualEstrelas();
});

statusClose.addEventListener("click", fecharModalStatus);
statusCancel.addEventListener("click", fecharModalStatus);
statusOverlay.addEventListener("click", event => {
    if (event.target === statusOverlay) fecharModalStatus();
});

statusConfirm.addEventListener("click", async () => {
    if (!idAtividadeEmEdicao) return;
    try {
        await apiRequest(`/feed/${idAtividadeEmEdicao}`, {
            method: "PATCH",
            body: JSON.stringify({
            status: statusSelecionado,
            rating: statusSelecionado === "READ" ? notaSelecionada : null
            })
        });
        fecharModalStatus();
        await carregarFeed();
    } catch (error) {
        renderError(error);
    }
});

function fecharTodosOsMenus() {
    document.querySelectorAll(".card-menu.open").forEach(menu => menu.classList.remove("open"));
}

feed.addEventListener("click", async event => {
    const actionButton = event.target.closest(".card-action");
    if (actionButton) {
        const menu = actionButton.nextElementSibling;
        const wasOpen = menu.classList.contains("open");
        fecharTodosOsMenus();
        if (!wasOpen) menu.classList.add("open");
        return;
    }

    const reactionButton = event.target.closest(".reaction-button");
    if (reactionButton) {
        try {
            await alternarReacao(reactionButton.closest(".feed-card").dataset.id, reactionButton.dataset.reaction);
        } catch (error) {
            renderError(error);
        }
        return;
    }

    const menuItem = event.target.closest(".card-menu-item");
    if (!menuItem) return;
    const id = menuItem.closest(".feed-card").dataset.id;
    fecharTodosOsMenus();

    if (menuItem.dataset.action === "alterar-status") {
        abrirModalStatus(id);
    } else if (menuItem.dataset.action === "excluir") {
        try {
            await apiRequest(`/feed/${id}`, { method: "DELETE" });
            await carregarFeed();
        } catch (error) {
            renderError(error);
        }
    }
});

document.addEventListener("click", event => {
    if (!event.target.closest(".card-menu") && !event.target.closest(".card-action")) {
        fecharTodosOsMenus();
    }
});

document.querySelectorAll(".sidebar-item").forEach(item => {
    item.addEventListener("click", event => {
        const href = item.getAttribute("href");
        if (href === "feed.html" || href === "lidos.html" || href === "lendo.html") {
            event.preventDefault();
            const status = href === "lidos.html" ? "READ" : href === "lendo.html" ? "READING" : null;
            carregarFeed(status);
        }
    });
});

document.getElementById("logoutButton").addEventListener("click", () => window.uni4readAuth.logout());

window.uni4readAuth.requireAuth().then(user => {
    document.getElementById("profileName").textContent = user.name;
    carregarFeed();
});
