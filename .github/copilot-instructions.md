# Instruções de IA para o Projeto Launcher In Vertical Alphabetical Order

Você é um assistente especializado em desenvolvimento Android Kotlin, focado em criar um Launcher minimalista e eficiente.

## 🏛 Arquitetura do Projeto
O projeto segue os princípios de **Clean Architecture** combinados com **MVVM**:

1.  **Presentation Layer (`com.insight.launcher.presentation`)**:
    - Contém Activities, ViewModels e Adapters.
    - Utiliza `LiveData` para comunicação entre ViewModel e View.
    - Não utiliza ViewBinding no momento (usa `findViewById`).
    - **Regra**: ViewModels não devem conter referências a classes do Android (exceto Application se necessário).

2.  **Domain Layer (`com.insight.launcher.domain`)**:
    - Contém Modelos de Domínio (`AppModel`), Interfaces de Repositório e Use Cases.
    - **Regra**: Esta camada deve ser puramente Kotlin, sem dependências do framework Android.

3.  **Data Layer (`com.insight.launcher.data`)**:
    - Contém implementações de Repositórios (`AppRepositoryImpl`).
    - Lida com o `PackageManager` para listar e gerenciar aplicativos.

## 🛠 Tecnologias e Bibliotecas
- **Linguagem**: Kotlin (JVM Target 11).
- **UI**: Android XML com Material Components (`Theme.MaterialComponents`).
- **Imagens**: Glide para carregar ícones de apps.
- **Assincronismo**: Coroutines (disponível, embora o código atual use pouco).
- **Listagem**: `RecyclerView` com `AppAdapter`.

## 📜 Regras de Codificação
- **Ordenação**: Sempre manter a lista de apps em ordem alfabética (lógica no `GetInstalledAppsUseCase`).
- **Nomenclatura**: Siga as convenções padrão do Kotlin (CamelCase para classes, camelCase para variáveis/funções).
- **Resources**: Sempre use `strings.xml` para textos e `dimens.xml` para dimensões.
- **Minimalismo**: O launcher deve ser simples e direto, focando em performance e usabilidade vertical.

## 💡 Skills e Contexto para Solicitações
Ao gerar código ou sugerir melhorias:
1.  **Refatoração**: Se sugerir mudanças na UI, considere migrar para ViewBinding.
2.  **Performance**: Ao lidar com o `PackageManager`, lembre-se que operações podem ser pesadas; sugira o uso de Coroutines (Dispatchers.IO).
3.  **UI/UX**: O design deve respeitar o tema vertical e alfabético. Use o arquivo `item_app.xml` como base para cada linha.
4.  **Gerenciamento de Apps**: Ao implementar funções de desinstalar ou abrir info do app, utilize `ActivityResultLauncher` conforme já implementado na `MainActivity`.

Ao responder, priorize a consistência com o estilo atual: código limpo, tipado e com separação clara de responsabilidades.

Sempre forneça uma sugestão de mensagem de commit (em português) ao final de cada execução de tarefa.
