# Uni4Read Backend

## Executar

No Windows, dentro desta pasta:

```powershell
./mvnw.cmd spring-boot:run
```

No macOS, é necessário ter o Java 17 ou superior instalado. Se o comando `brew` não for encontrado, instale primeiro o [Homebrew](https://brew.sh/):

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Ao final da instalação, siga a instrução exibida pelo instalador para adicionar o Homebrew ao `PATH`. Feche e abra o Terminal novamente e confirme:

```bash
brew --version
```

Com o Homebrew disponível, instale o Java 17:

```bash
brew install openjdk@17
sudo ln -sfn "$(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

Se não quiser usar o Homebrew, baixe e instale o JDK 17 pelo [site oficial do Adoptium](https://adoptium.net/temurin/releases/?version=17), escolhendo macOS e a arquitetura do seu Mac (`aarch64` para Apple Silicon ou `x64` para Intel).

Confirme a versão do Java e, na primeira execução, dê permissão ao Maven Wrapper:

```bash
java -version
chmod +x mvnw
```

Depois, dentro da pasta `backend`, suba a aplicação com:

```bash
./mvnw spring-boot:run
```

Se preferir definir um segredo JWT próprio durante o desenvolvimento, use uma variável de ambiente antes de iniciar a aplicação:

```bash
export JWT_SECRET='uma-chave-com-pelo-menos-32-caracteres'
./mvnw spring-boot:run
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

Os enums enviados pela API são `READING`, `READ`, `LIKE` e `DISLIKE`.

### Autenticação

- `POST /api/auth/register`: cria uma conta e inicia a sessão.
- `POST /api/auth/login`: autentica e retorna um access token.
- `POST /api/auth/refresh`: rotaciona o refresh token `HttpOnly`.
- `GET /api/auth/me`: retorna o usuário autenticado.
- `POST /api/auth/logout`: revoga a sessão atual.

As rotas do feed usam o usuário do token JWT e não aceitam mais `userId` enviado pelo cliente. Em desenvolvimento, os usuários seed são `alice@example.com` e `bruno@example.com`, ambos com a senha `password`. Defina `JWT_SECRET` com pelo menos 32 bytes fora do ambiente de desenvolvimento.

## Testes

```powershell
./mvnw.cmd test
```
# Clube-do-Livro
# Clube-do-Livro
