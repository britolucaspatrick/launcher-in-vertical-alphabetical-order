# Launcher em Ordem Alfabética Vertical

Este é um launcher Android minimalista que organiza seus aplicativos em uma grade vertical por ordem alfabética. O projeto foi refatorado para seguir os princípios da **Clean Architecture**, garantindo manutenibilidade, testabilidade e separação de preocupações.

## 🚀 Tecnologias e Padrões Utilizados

- **Kotlin**: Linguagem principal.
- **Clean Architecture**: Divisão do projeto em camadas (Domain, Data, Presentation).
- **MVVM (Model-View-ViewModel)**: Padrão de arquitetura de UI.
- **Jetpack Lifecycle (ViewModel & LiveData)**: Gerenciamento de estado da UI consciente do ciclo de vida.
- **Kotlin Coroutines**: Para operações assíncronas (carregamento de apps).
- **Glide**: Carregamento de imagens de fundo aleatórias (via Picsum).
- **Material Design**: Componentes de interface.

---

## 🏗️ Estrutura do Projeto (Clean Architecture)

O projeto está organizado nos seguintes pacotes dentro de `com.insight.launcher`:

### 1. Camada de Domínio (`domain`)
A camada mais interna, contendo a lógica de negócio pura e independente de frameworks Android.
- **`model/`**: Contém a entidade `AppModel`, que representa as informações básicas de um aplicativo.
- **`repository/`**: Define a interface `AppRepository`, estabelecendo o contrato para obtenção de dados.
- **`usecase/`**: Contém o `GetInstalledAppsUseCase`, que encapsula a regra de negócio de buscar e ordenar os aplicativos instalados.

### 2. Camada de Dados (`data`)
Responsável pela implementação dos repositórios definidos no domínio.
- **`repository/`**: `AppRepositoryImpl` utiliza o `PackageManager` do Android para buscar a lista real de aplicativos instalados no sistema.

### 3. Camada de Apresentação (`presentation`)
Responsável pela interface do usuário e gerenciamento de estado.
- **`model/`**: `AppUiModel` contém os dados formatados para exibição (incluindo o ícone `Drawable`).
- **`MainViewModel`**: Observa o caso de uso e expõe os dados para a `MainActivity` via `LiveData`.
- **`MainActivity`**: Atua como a View, reagindo às mudanças no `ViewModel` e gerenciando a interação com o usuário.

---

## 🔑 Configuração de Assinatura (Signing)

O projeto está configurado para assinatura automática. Para gerar o APK assinado ou rodar em modo release:

1. Crie um arquivo `keystore.jks` usando o comando:
   ```bash
   keytool -genkey -v -keystore keystore.jks -alias launcher_key -keyalg RSA -keysize 2048 -validity 10000
   ```
2. No seu arquivo `gradle.properties`, adicione as seguintes chaves:
   ```properties
   RELEASE_STORE_FILE=keystore.jks
   RELEASE_STORE_PASSWORD=sua_senha
   RELEASE_KEY_ALIAS=launcher_key
   RELEASE_KEY_PASSWORD=sua_senha_da_chave
   ```

---

## 🛠️ Como Funciona o Launcher

1. **Listagem**: Ao abrir, o launcher busca todos os apps com categoria `LAUNCHER` instalados no dispositivo.
2. **Ordem Alfabética**: Os apps são exibidos em uma grade (Grid) organizada verticalmente por nome.
3. **Personalização**: É possível alterar o tamanho e o estilo da fonte (Negrito, Itálico) através do menu de configurações (clique longo em qualquer app).
4. **Papel de Parede**: O fundo do launcher muda diariamente buscando uma imagem aleatória da natureza.
5. **Gestão**: Clique longo em um app permite abrir as informações do sistema ou desinstalá-lo (se permitido).

---

## 🧪 Próximos Passos (Melhorias)
- [ ] Implementação de Injeção de Dependência (Hilt/Koin).
- [ ] Adição de Testes Unitários para a camada de Domínio.
- [ ] Implementação de uma barra de busca para filtrar apps.
- [ ] Suporte a pacotes de ícones customizados.
