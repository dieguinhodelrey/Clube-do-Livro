# Uni4Read Backend

## Executar

No Windows, dentro desta pasta:

```powershell
./mvnw.cmd spring-boot:run
```

A API inicia em `http://localhost:8080`. O banco padrão é H2 em memória e é recriado a cada execução. Os dados seed incluem os usuários Alice e Bruno.

## Contrato atual

- `GET /api/feed?userId={uuid}&status=READING|READ`: lista atividades.
- `POST /api/feed`: cria atividade com `userId`, `bookId` e `status`.
- `PATCH /api/feed/{id}`: altera com `userId`, `status` e `rating` opcional de 1 a 5.
- `DELETE /api/feed/{id}?userId={uuid}`: exclui atividade do próprio usuário.
- `POST /api/feed/{id}/reacao`: alterna `LIKE` ou `DISLIKE` para um `userId`.
- `GET /api/books`: lista livros.
- `POST /api/books`: cria ou reutiliza livro por título e autor.

Os enums enviados pela API são `READING`, `READ`, `LIKE` e `DISLIKE`. A autenticação JWT ainda está pendente; por enquanto o frontend usa o UUID seed de Alice para demonstrar a integração.

## Testes

```powershell
./mvnw.cmd test
```
# Clube-do-Livro
# Clube-do-Livro
