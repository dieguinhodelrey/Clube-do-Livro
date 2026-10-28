const ATIVIDADES_PADRAO = [
    {
        id: "seed-1",
        usuario: "Diego",
        avatar: "📚",
        status: "lendo",
        livro: "Como se tornar um Divo",
        autor: "Diego Del Rey",
        nota: null
    },
    {
        id: "seed-2",
        usuario: "Cristiana",
        avatar: "🌻",
        status: "lido",
        livro: "violet bent backwards over the grass",
        autor: "Lana Del Rey",
        nota: 5
    },
    {
        id: "seed-3",
        usuario: "Leonardo",
        avatar: "🐻",
        status: "lendo",
        livro: "O Avesso da Pele",
        autor: "Jeferson Tenório",
        nota: null
    }
];

const CHAVE_STORAGE = "uni4read-atividades";

function carregarAtividades() {
    try {
        const salvo = localStorage.getItem(CHAVE_STORAGE);
        if (salvo) {
            return JSON.parse(salvo);
        }
    } catch (erro) {
        console.error("Não foi possível carregar os dados salvos:", erro);
    }
    return ATIVIDADES_PADRAO;
}

function salvarAtividades() {
    try {
        localStorage.setItem(CHAVE_STORAGE, JSON.stringify(atividades));
    } catch (erro) {
        console.error("Não foi possível salvar os dados:", erro);
    }
}

const atividades = carregarAtividades();

const feed = document.querySelector(".feed");
const pagina = (location.pathname.split("/").pop() || "feed.html").toLowerCase();

function normalizarStatus(status) {
    const valor = String(status || "").toLowerCase().trim();
    if (["lido", "lidos", "read", "finished", "concluido", "concluído"].includes(valor)) return "lido";
    if (["lendo", "reading", "in progress", "em andamento"].includes(valor)) return "lendo";
    return valor;
}
const CHAVE_AMIGOS = "uni4read-amigos";
let amigos = [];
try { amigos = JSON.parse(localStorage.getItem(CHAVE_AMIGOS) || "[]"); } catch (erro) { amigos = []; }

const CORES_CAPA = [
    "#29479f", "#3f7d55", "#a4c6f3", "#d97757",
    "#7c5cbf", "#c14e6b", "#3f9188", "#b98a2f"
];

function corParaTexto(texto) {
    let hash = 0;
    for (let i = 0; i < texto.length; i++) {
        hash = texto.charCodeAt(i) + ((hash << 5) - hash);
    }
    const indice = Math.abs(hash) % CORES_CAPA.length;
    return CORES_CAPA[indice];
}

function gerarCapa(livro, autor) {
    const cor = corParaTexto(livro + autor);

    const capa = document.createElement("div");
    capa.className = "book-cover";
    capa.style.background = cor;

    const tituloEl = document.createElement("span");
    tituloEl.className = "cover-title";
    tituloEl.textContent = livro;

    const autorEl = document.createElement("span");
    autorEl.className = "cover-author";
    autorEl.textContent = autor;

    capa.appendChild(tituloEl);
    capa.appendChild(autorEl);

    return capa;
}

function criarCard(atividade) {

    const card = document.createElement("article");

    card.classList.add("feed-card");
    card.dataset.id = atividade.id;

    if (normalizarStatus(atividade.status) === "lido") {
        card.classList.add("read-card");
    }
    const avaliacaoHTML = (normalizarStatus(atividade.status) === "lido" && atividade.nota)
        ? `<div class="rating-display">
               <span class="stars">${"★".repeat(atividade.nota)}${"☆".repeat(5 - atividade.nota)}</span>
               <span>${atividade.nota}/5</span>
           </div>`
        : "";

    card.innerHTML = `
        <div class="card-header">

            <div class="user-info">

                <div class="avatar">
                    ${atividade.avatar}
                </div>

                <div>
                    <strong>${atividade.usuario}</strong>

                    <span>
                        ${normalizarStatus(atividade.status) === "lendo"
                            ? "está lendo"
                            : "leu"}
                    </span>
                </div>

            </div>

            <div class="card-action-wrapper">
                <button class="card-action">
                    ⋮
                </button>

                <div class="card-menu">
                    <button type="button" class="card-menu-item" data-action="alterar-status">
                        Alterar status
                    </button>
                    <button type="button" class="card-menu-item danger" data-action="excluir">
                        Excluir livro
                    </button>
                </div>
            </div>

        </div>


        <div class="book-content">

            <div class="book-cover-slot"></div>

            <div class="book-info">

                <span class="status-badge ${
                    normalizarStatus(atividade.status) === "lendo"
                        ? "reading"
                        : "read"
                }">

                    ${
                        normalizarStatus(atividade.status) === "lendo"
                            ? "LENDO"
                            : "LIDO"
                    }

                </span>

                <h2>
                    ${atividade.livro}
                </h2>

                <p>
                    ${atividade.autor}
                </p>

                ${avaliacaoHTML}

            </div>

        </div>


        <div class="card-footer">

            <button class="reaction-button">
                👍
            </button>

            <button class="reaction-button">
                👎
            </button>

        </div>
    `;

    const capaSlot = card.querySelector(".book-cover-slot");
    const capa = gerarCapa(atividade.livro, atividade.autor);
    capaSlot.replaceWith(capa);

    return card;
}

function renderFeed() {
    if (!feed) return;
    feed.innerHTML = "";

    if (pagina === "amigos.html") {
        renderAmigos();
        salvarAtividades();
        return;
    }

    // Cada arquivo tem sua própria página: feed.html, lidos.html ou lendo.html.
    // Normalizamos o status para evitar diferenças de maiúsculas/minúsculas ou valores antigos.
    const filtradas = atividades.filter(atividade => {
        const status = normalizarStatus(atividade.status);
        if (pagina === "lidos.html") return status === "lido";
        if (pagina === "lendo.html") return status === "lendo";
        return true; // Feed mostra todas as atividades.
    });

    if (filtradas.length === 0) {
        const mensagem = pagina === "lidos.html"
            ? ["Nenhum livro lido ainda", "Os livros que você marcar como lidos aparecerão aqui."]
            : pagina === "lendo.html"
                ? ["Você não está lendo nenhum livro", "Adicione um livro e marque o status como Lendo."]
                : ["Seu feed está vazio", "Adicione um livro para começar."];
        feed.innerHTML = `<div class="empty-state"><h2>${mensagem[0]}</h2><p>${mensagem[1]}</p></div>`;
    } else {
        filtradas.forEach(atividade => feed.appendChild(criarCard(atividade)));
    }
    salvarAtividades();
}

function salvarAmigos() {
    try { localStorage.setItem(CHAVE_AMIGOS, JSON.stringify(amigos)); } catch (erro) { console.error("Não foi possível salvar os amigos:", erro); }
}

function renderAmigos() {
    if (!feed) return;
    feed.innerHTML = `
      <form class="friend-add-form" id="friendAddForm">
        <label class="field"><span>Adicionar amigo</span><input id="friendName" type="text" placeholder="Nome ou @usuário" required maxlength="60"></label>
        <button class="btn-primary" type="submit">Adicionar</button>
      </form>
      <div class="friends-list" id="friendsList"></div>`;
    const list = document.getElementById("friendsList");
    if (!amigos.length) {
      list.innerHTML = '<div class="empty-state"><h2>Você ainda não tem amigos</h2><p>Adicione pessoas para montar sua lista de amigos.</p></div>';
    } else {
      amigos.forEach((nome, indice) => {
        const linha = document.createElement("div");
        linha.className = "friend-row";
        const info = document.createElement("div");
        info.className = "friend-info";
        const avatar = document.createElement("span");
        avatar.className = "avatar";
        avatar.textContent = "👤";
        const nomeEl = document.createElement("strong");
        nomeEl.textContent = nome;
        info.append(avatar, nomeEl);
        const remover = document.createElement("button");
        remover.className = "btn-secondary friend-remove";
        remover.type = "button";
        remover.textContent = "Remover";
        remover.addEventListener("click", () => { amigos.splice(indice, 1); salvarAmigos(); renderAmigos(); });
        linha.append(info, remover);
        list.appendChild(linha);
      });
    }
    document.getElementById("friendAddForm").addEventListener("submit", event => {
      event.preventDefault();
      const campo = document.getElementById("friendName");
      const nome = campo.value.trim().replace(/^@/, "");
      if (!nome) return;
      if (amigos.some(amigo => amigo.toLocaleLowerCase() === nome.toLocaleLowerCase())) {
        alert("Esse amigo já está na sua lista.");
        return;
      }
      amigos.push(nome);
      salvarAmigos();
      renderAmigos();
    });
}

renderFeed();

document.addEventListener("click", function(event) {

    if (!event.target.classList.contains("reaction-button")) {
        return;
    }

    const card = event.target.closest(".feed-card");

    const buttons = card.querySelectorAll(".reaction-button");

    buttons.forEach(button => {
        button.classList.remove("selected");
    });

    event.target.classList.add("selected");
});

const addBookButton = document.querySelector(".add-book-button");
const addBookOverlay = document.getElementById("addBookOverlay");
const addBookClose = document.getElementById("addBookClose");
const addBookCancel = document.getElementById("addBookCancel");
const addBookForm = document.getElementById("addBookForm");

const AVATARES_PADRAO = ["📘", "📗", "📙", "📕", "🦉", "🐱", "🐼", "🦊"];

function avatarAleatorio() {
    const indice = Math.floor(Math.random() * AVATARES_PADRAO.length);
    return AVATARES_PADRAO[indice];
}

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

// Fecha ao clicar fora do card do modal
addBookOverlay.addEventListener("click", function(event) {
    if (event.target === addBookOverlay) {
        fecharModal();
    }
});

addBookForm.addEventListener("submit", function(event) {
    event.preventDefault();

    const titulo = document.getElementById("fieldTitulo").value.trim();
    const autor = document.getElementById("fieldAutor").value.trim();
    const status = document.getElementById("fieldStatus").value;

    if (!titulo || !autor) {
        return;
    }

    const novaAtividade = {
        id: "livro-" + Date.now(),
        usuario: "Você",
        avatar: avatarAleatorio(),
        status: status,
        livro: titulo,
        autor: autor,
        nota: null
    };

    atividades.unshift(novaAtividade);

    renderFeed();
    fecharModal();
});

document.addEventListener("click", function(event) {

    const actionButton = event.target.closest(".card-action");

    if (actionButton) {
        const menu = actionButton.nextElementSibling;
        const jaAberto = menu.classList.contains("open");

        fecharTodosOsMenus();

        if (!jaAberto) {
            menu.classList.add("open");
        }

        return;
    }

    if (!event.target.closest(".card-menu")) {
        fecharTodosOsMenus();
    }
});

function fecharTodosOsMenus() {
    document.querySelectorAll(".card-menu.open").forEach(menu => {
        menu.classList.remove("open");
    });
}

// Clique nos itens do menu (Excluir livro / Alterar status)
document.addEventListener("click", function(event) {

    const item = event.target.closest(".card-menu-item");

    if (!item) {
        return;
    }

    const card = item.closest(".feed-card");
    const id = card.dataset.id;

    fecharTodosOsMenus();

    if (item.dataset.action === "excluir") {
        excluirLivro(id);
    }

    if (item.dataset.action === "alterar-status") {
        abrirModalStatus(id);
    }
});

function excluirLivro(id) {
    const indice = atividades.findIndex(atividade => atividade.id === id);

    if (indice === -1) {
        return;
    }

    atividades.splice(indice, 1);
    renderFeed();
}

const statusOverlay = document.getElementById("statusOverlay");
const statusClose = document.getElementById("statusClose");
const statusCancel = document.getElementById("statusCancel");
const statusConfirm = document.getElementById("statusConfirm");
const statusLendoBtn = document.getElementById("statusLendo");
const statusLidoBtn = document.getElementById("statusLido");
const ratingSection = document.getElementById("ratingSection");
const starPicker = document.getElementById("starPicker");

let idLivroEmEdicao = null;
let statusSelecionado = null;
let notaSelecionada = null;

function abrirModalStatus(id) {
    const atividade = atividades.find(item => item.id === id);

    if (!atividade) {
        return;
    }

    idLivroEmEdicao = id;
    statusSelecionado = atividade.status;
    notaSelecionada = atividade.nota || null;

    atualizarVisualStatus();
    atualizarVisualEstrelas();
    atualizarVisibilidadeAvaliacao();

    statusOverlay.classList.add("open");
}

function fecharModalStatus() {
    statusOverlay.classList.remove("open");
    idLivroEmEdicao = null;
}

function atualizarVisualStatus() {
    statusLendoBtn.classList.toggle("selected", statusSelecionado === "lendo");
    statusLidoBtn.classList.toggle("selected", statusSelecionado === "lido");
}

function atualizarVisibilidadeAvaliacao() {
    ratingSection.classList.toggle("visible", statusSelecionado === "lido");
}

function atualizarVisualEstrelas() {
    const botoes = starPicker.querySelectorAll(".star-button");

    botoes.forEach(botao => {
        const valor = Number(botao.dataset.value);
        botao.classList.toggle("active", notaSelecionada !== null && valor <= notaSelecionada);
    });
}

statusLendoBtn.addEventListener("click", function() {
    statusSelecionado = "lendo";
    atualizarVisualStatus();
    atualizarVisibilidadeAvaliacao();
});

statusLidoBtn.addEventListener("click", function() {
    statusSelecionado = "lido";
    atualizarVisualStatus();
    atualizarVisibilidadeAvaliacao();
});

starPicker.addEventListener("click", function(event) {
    const botao = event.target.closest(".star-button");

    if (!botao) {
        return;
    }

    notaSelecionada = Number(botao.dataset.value);
    atualizarVisualEstrelas();
});

statusClose.addEventListener("click", fecharModalStatus);
statusCancel.addEventListener("click", fecharModalStatus);

statusOverlay.addEventListener("click", function(event) {
    if (event.target === statusOverlay) {
        fecharModalStatus();
    }
});

statusConfirm.addEventListener("click", function() {
    if (!idLivroEmEdicao) {
        return;
    }

    const atividade = atividades.find(item => item.id === idLivroEmEdicao);

    if (!atividade) {
        return;
    }

    atividade.status = normalizarStatus(statusSelecionado);

    atividade.nota = statusSelecionado === "lido" ? notaSelecionada : null;

    renderFeed();
    fecharModalStatus();
});