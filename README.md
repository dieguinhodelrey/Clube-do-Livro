# Uni4Read Frontend

## Executar

O frontend é uma página estática. Sirva esta pasta por HTTP para que as chamadas `fetch` funcionem:

```powershell
python -m http.server 5500
```

Abra `http://localhost:5500`. O backend deve estar em execução em `http://localhost:8080`.

Para usar outra URL de API, defina `window.UNI4READ_API_URL` antes de carregar `script.js` em `index.html`.

## Integração

O feed, criação de livros, atividades, alteração de status, avaliação, exclusão e reações são persistidos na API. O frontend não usa `localStorage` como fonte de dados.

A identidade atual é temporariamente o usuário seed Alice (`10000000-0000-0000-0000-000000000001`). O próximo incremento deve substituir essa constante por login e token JWT.
# Clube-do-Livro
# Clube-do-Livro
