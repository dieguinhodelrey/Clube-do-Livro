# Uni4Read Backend

## Executar

No Windows, dentro desta pasta:

```powershell
.\mvnw.cmd spring-boot:run
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

Sirva o frontend por HTTP na porta 5500. `FRONTEND_ORIGINS` configura as origens permitidas, separadas por vírgula; o padrão é `http://localhost:5500,http://127.0.0.1:5500`. Use o mesmo hostname no frontend e na API para preservar os cookies da sessão. O passo a passo está em [Como executar na wiki](https://github.com/dieguinhodelrey/Clube-do-Livro/wiki/Como-executar).

## Documentação

O contrato é documentado no [Guia da API na wiki](https://github.com/dieguinhodelrey/Clube-do-Livro/wiki/Guia-da-API), com rotas, autenticação, pedidos, respostas e erros. Feed e leituras usam a identidade do token, não um `userId` escolhido pelo cliente.

O [escopo](https://github.com/dieguinhodelrey/Clube-do-Livro/wiki/Escopo-da-entrega), os [requisitos e regras](https://github.com/dieguinhodelrey/Clube-do-Livro/wiki/Requisitos-e-regras) e as [histórias de usuário](https://github.com/dieguinhodelrey/Clube-do-Livro/wiki/Hist%C3%B3rias-de-usu%C3%A1rio) ficam na wiki oficial, sem cópias paralelas nesta pasta.

## Testes

Os testes cobrem autenticação e cookies, catálogo, Feed, listas pessoais, conclusão e avaliação, exclusão e reações com duas contas. Na configuração padrão, usam H2 no processo de testes, separado da aplicação em execução. As contas de teste de autenticação são criadas e removidas em cada cenário; os demais dados de integração são desfeitos por transações.

```powershell
.\mvnw.cmd test
```
