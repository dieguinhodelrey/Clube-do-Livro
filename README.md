# Uni4Read Frontend

## Executar

O frontend é uma página estática. Sirva esta pasta por HTTP para que as chamadas `fetch` funcionem:

```powershell
python -m http.server 5500
```

Abra `http://localhost:5500` (ou `http://127.0.0.1:5500`). O backend deve estar em execução em `http://localhost:8080`.

Não abra o HTML diretamente pelo Explorer (`file://`), pois o navegador não envia uma origem HTTP permitida para cookies e CORS. Se estiver usando outra porta ou origem, defina `FRONTEND_ORIGINS` no backend, por exemplo: `FRONTEND_ORIGINS=http://localhost:5500,http://localhost:3000`.

Para usar outra URL de API, defina `window.UNI4READ_API_URL` antes de carregar `script.js` em `index.html`.

## Integração

O feed, criação de livros, atividades, alteração de status, avaliação, exclusão e reações são persistidos na API. O frontend não usa `localStorage` como fonte de dados.

O frontend usa login e cadastro com JWT. O access token fica apenas em memória e o refresh token é mantido pelo backend em cookie `HttpOnly`; ao recarregar uma página protegida, a sessão é restaurada automaticamente.

Para testar rapidamente, os usuários seed são `alice@example.com` e `bruno@example.com`, ambos com a senha `password`.
# Clube-do-Livro
# Clube-do-Livro
