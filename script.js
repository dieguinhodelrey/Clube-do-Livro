const feed = document.querySelector(".feed");
const activities = [];
const pendingDeletes = new Set();
const pendingReactions = new Set();
const operationError = document.createElement("p");
operationError.className = "auth-error";
operationError.setAttribute("role", "alert");
feed.before(operationError);

function renderOperationError(error) {
    operationError.textContent = error.message;
}

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
    feed.innerHTML = `<div class="empty-state" role="status"><p>Carregando ${currentPage.title.toLowerCase()}...</p></div>`;
}

function renderError(error, retry = carregarFeed) {
    feed.innerHTML = `<div class="empty-state" role="alert"><h2>Não foi possível carregar ${currentPage.title}</h2><p>${escapeHtml(error.message)}</p><button type="button" class="btn-primary retry-button">Tentar novamente</button></div>`;
    feed.querySelector(".retry-button").addEventListener("click", () => retry());
}

function renderEmpty() {
    const title = currentPage.status === "READ" ? "Você ainda não tem leituras lidas"
        : currentPage.status === "READING" ? "Você ainda não tem leituras em andamento"
        : "O Feed ainda está vazio";
    feed.innerHTML = `<div class="empty-state"><h2>${title}</h2><p>Use Adicionar livro para registrar uma leitura.</p></div>`;
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
    const reactionDisabled = pendingReactions.has(activity.id) ? "disabled" : "";

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
                    <button type="button" class="card-menu-item danger" data-action="excluir">Excluir leitura</button>
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
            <button class="reaction-button ${likeSelected}" data-reaction="LIKE" type="button" aria-label="Curtir" ${reactionDisabled}>👍 <span>${activity.likes}</span></button>
            <button class="reaction-button ${dislikeSelected}" data-reaction="DISLIKE" type="button" aria-label="Não curtir" ${reactionDisabled}>👎 <span>${activity.dislikes}</span></button>
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

async function carregarFeed(status = currentPage.status) {
    renderLoading();
    try {
        const params = new URLSearchParams();
        if (status) params.set("status", status);
        const endpoint = status ? "/feed/mine" : "/feed";
        const response = await apiRequest(`${endpoint}?${params}`);
        activities.splice(0, activities.length, ...response);
        renderFeed();
    } catch (error) {
        renderError(error, () => carregarFeed(status));
    }
}

async function alternarReacao(activityId, type) {
    if (pendingReactions.has(activityId)) return;
    pendingReactions.add(activityId);
    operationError.textContent = "";
    const buttons = () => feed.querySelectorAll(`[data-id="${activityId}"] .reaction-button`);
    buttons().forEach(button => { button.disabled = true; });
    try {
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
    } finally {
        pendingReactions.delete(activityId);
        buttons().forEach(button => { button.disabled = false; });
    }
}

const addBookButton = document.querySelector(".add-book-button");
const addBookOverlay = document.getElementById("addBookOverlay");
const addBookClose = document.getElementById("addBookClose");
const addBookCancel = document.getElementById("addBookCancel");
const addBookForm = document.getElementById("addBookForm");
const addBookSubmit = addBookForm.querySelector('button[type="submit"]');
const addBookError = document.createElement("p");
addBookError.className = "auth-error";
addBookError.setAttribute("role", "alert");
addBookForm.prepend(addBookError);
document.getElementById("fieldTitulo").maxLength = 255;
document.getElementById("fieldAutor").maxLength = 255;
let pendingAdd = false;

function abrirModal() {
    addBookError.textContent = "";
    addBookOverlay.classList.add("open");
    document.getElementById("fieldTitulo").focus();
}

function fecharModal() {
    if (pendingAdd) return;
    addBookOverlay.classList.remove("open");
    addBookForm.reset();
    addBookButton.focus();
}

addBookButton.addEventListener("click", abrirModal);
addBookClose.addEventListener("click", fecharModal);
addBookCancel.addEventListener("click", fecharModal);
addBookOverlay.addEventListener("click", event => {
    if (event.target === addBookOverlay) fecharModal();
});

addBookForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (pendingAdd) return;
    addBookError.textContent = "";
    const title = document.getElementById("fieldTitulo").value.trim();
    const author = document.getElementById("fieldAutor").value.trim();
    const status = { lendo: "READING", lido: "READ" }[document.getElementById("fieldStatus").value];
    if (!title || !author || title.length > 255 || author.length > 255 || !status) {
        addBookError.textContent = "Informe título e autor com até 255 caracteres e escolha Lendo ou Lido.";
        return;
    }
    if (!addBookForm.reportValidity()) return;

    pendingAdd = true;
    addBookForm.querySelectorAll("input, select, button").forEach(control => { control.disabled = true; });
    addBookClose.disabled = true;
    addBookSubmit.textContent = "Adicionando...";
    let saved = false;
    try {
        const book = await apiRequest("/books", {
            method: "POST",
            body: JSON.stringify({ title, author })
        });
        await apiRequest("/feed", {
            method: "POST",
            body: JSON.stringify({ bookId: book.id, status })
        });
        saved = true;
    } catch (error) {
        addBookError.textContent = error.message;
    } finally {
        pendingAdd = false;
        addBookForm.querySelectorAll("input, select, button").forEach(control => { control.disabled = false; });
        addBookClose.disabled = false;
        addBookSubmit.textContent = "Adicionar";
    }
    if (saved) {
        fecharModal();
        await carregarFeed();
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
const clearRatingButton = document.createElement("button");
clearRatingButton.type = "button";
clearRatingButton.className = "btn-secondary";
clearRatingButton.textContent = "Sem avaliação";
ratingSection.appendChild(clearRatingButton);
const statusError = document.createElement("p");
statusError.className = "auth-error";
statusError.setAttribute("role", "alert");
statusOverlay.querySelector(".status-options").before(statusError);

let idAtividadeEmEdicao = null;
let statusSelecionado = null;
let notaSelecionada = null;
let statusOriginal = null;
let pendingStatus = false;

function atualizarVisualStatus() {
    statusLendoBtn.classList.toggle("selected", statusSelecionado === "READING");
    statusLidoBtn.classList.toggle("selected", statusSelecionado === "READ");
    ratingSection.classList.toggle("visible", statusSelecionado === "READ");
    statusLendoBtn.disabled = statusOriginal === "READ" || pendingStatus;
}

function atualizarVisualEstrelas() {
    starPicker.querySelectorAll(".star-button").forEach(button => {
        const value = Number(button.dataset.value);
        button.classList.toggle("active", notaSelecionada !== null && value <= notaSelecionada);
        button.setAttribute("aria-label", `${value} ${value === 1 ? "estrela" : "estrelas"}`);
        button.setAttribute("aria-pressed", String(notaSelecionada === value));
    });
    clearRatingButton.setAttribute("aria-pressed", String(notaSelecionada === null));
}

function abrirModalStatus(id) {
    const activity = activities.find(item => item.id === id);
    if (!activity) return;
    idAtividadeEmEdicao = id;
    statusOriginal = activity.status;
    statusSelecionado = activity.status;
    notaSelecionada = activity.rating || null;
    statusError.textContent = "";
    atualizarVisualStatus();
    atualizarVisualEstrelas();
    statusOverlay.classList.add("open");
}

function fecharModalStatus() {
    if (pendingStatus) return;
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

clearRatingButton.addEventListener("click", () => {
    notaSelecionada = null;
    atualizarVisualEstrelas();
});

statusClose.addEventListener("click", fecharModalStatus);
statusCancel.addEventListener("click", fecharModalStatus);
statusOverlay.addEventListener("click", event => {
    if (event.target === statusOverlay) fecharModalStatus();
});

statusConfirm.addEventListener("click", async () => {
    if (pendingStatus || !idAtividadeEmEdicao) return;
    statusError.textContent = "";
    if (!["READING", "READ"].includes(statusSelecionado)
            || (statusSelecionado === "READ" && notaSelecionada !== null
                && (!Number.isInteger(notaSelecionada) || notaSelecionada < 1 || notaSelecionada > 5))) {
        statusError.textContent = "Escolha um status e uma nota inteira de 1 a 5, ou deixe sem avaliação.";
        return;
    }
    pendingStatus = true;
    statusOverlay.querySelectorAll("button").forEach(button => { button.disabled = true; });
    statusConfirm.textContent = "Salvando...";
    let saved = false;
    try {
        await apiRequest(`/feed/${idAtividadeEmEdicao}`, {
            method: "PATCH",
            body: JSON.stringify({
            status: statusSelecionado,
            rating: statusSelecionado === "READ" ? notaSelecionada : null
            })
        });
        saved = true;
    } catch (error) {
        statusError.textContent = error.message;
    } finally {
        pendingStatus = false;
        statusOverlay.querySelectorAll("button").forEach(button => { button.disabled = false; });
        statusConfirm.textContent = "Salvar";
        atualizarVisualStatus();
    }
    if (saved) {
        fecharModalStatus();
        await carregarFeed();
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
            renderOperationError(error);
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
        if (pendingDeletes.has(id)) return;
        const activity = activities.find(item => item.id === id);
        if (!window.confirm(`Excluir sua leitura de "${activity.book.title}"? O livro continuará no catálogo.`)) return;
        pendingDeletes.add(id);
        menuItem.disabled = true;
        operationError.textContent = "";
        try {
            await apiRequest(`/feed/${id}`, { method: "DELETE" });
            await carregarFeed();
        } catch (error) {
            renderOperationError(error);
        } finally {
            pendingDeletes.delete(id);
            menuItem.disabled = false;
        }
    }
});

document.addEventListener("click", event => {
    if (!event.target.closest(".card-menu") && !event.target.closest(".card-action")) {
        fecharTodosOsMenus();
    }
});

const pages = {
    "feed.html": { title: "Feed", status: null },
    "lidos.html": { title: "Lidos", status: "READ" },
    "lendo.html": { title: "Lendo", status: "READING" }
};

const currentPage = pages[window.location.pathname.split("/").pop()] || pages["feed.html"];
document.querySelector(".page-header h1").textContent = currentPage.title;
document.title = `Uni4Read - ${currentPage.title}`;
document.querySelectorAll(".sidebar-item").forEach(item => {
    item.classList.toggle("active", pages[item.getAttribute("href")] === currentPage);
});

const logoutButton = document.getElementById("logoutButton");
logoutButton.addEventListener("click", async () => {
    logoutButton.disabled = true;
    try {
        await window.uni4readAuth.logout();
    } catch (error) {
        renderOperationError(error);
    } finally {
        logoutButton.disabled = false;
    }
});

async function iniciarPagina() {
    addBookButton.disabled = true;
    renderLoading();
    try {
        const user = await window.uni4readAuth.requireAuth();
        document.getElementById("profileName").textContent = user.name;
        addBookButton.disabled = false;
        await carregarFeed();
    } catch (error) {
        console.error("Não foi possível iniciar a sessão", error);
        renderError(error, iniciarPagina);
    }
}

iniciarPagina();
