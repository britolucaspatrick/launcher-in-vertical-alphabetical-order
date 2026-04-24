# Configuração de IA para o Projeto

Este projeto está configurado com diretrizes para que assistentes de IA (GitHub Copilot, Gemini, Claude) entendam a arquitetura e as regras de negócio.

## 🤖 Como usar com cada IA

### 1. GitHub Copilot
O Copilot no VS Code ou Android Studio lê automaticamente o arquivo `.github/copilot-instructions.md`. Nenhuma ação adicional é necessária.

### 2. Gemini / Claude (Web ou API)
Se você estiver usando as versões web ou chat dessas IAs, copie o conteúdo abaixo e cole no início da sua conversa ou configure como "Instruções Personalizadas" (Custom Instructions):

---
**CONTEÚDO PARA COPIAR:**
"Atue como um desenvolvedor Android Sênior trabalhando no projeto 'Launcher Vertical'.
Arquitetura: Clean Architecture + MVVM.
Camadas: Data (repositórios), Domain (use cases/modelos), Presentation (Activities/ViewModels/XML).
Tecnologias: Kotlin, Material Components, Glide, RecyclerView.
Regra de Ouro: Manter a ordem alfabética vertical e separar lógica de sistema (PackageManager) em repositórios.
Sempre prefira manter a consistência com o código existente em `MainActivity.kt` e `AppRepositoryImpl.kt`.
Ao final de cada execução, forneça sempre uma sugestão de mensagem de commit (em inglês) que resuma as alterações feitas."
---

### 3. Cursor (IDE)
O arquivo `.cursorrules` na raiz já contém todas as instruções necessárias para o Cursor.

## 📂 Estrutura de Arquivos de Instrução
- `.github/copilot-instructions.md`: Instruções específicas para GitHub Copilot.
- `.cursorrules`: Regras para IDEs baseadas em IA (Cursor, Windsurf, etc).
- `AI_INSTRUCTIONS.md`: Este guia.

## 🎯 Objetivo das Instruções
Garantir que a IA:
1. Respeite a separação de camadas (Data/Domain/Presentation).
2. Não misture lógica de negócio com a View.
3. Utilize as dependências corretas definidas em `libs.versions.toml`.
4. Mantenha o design minimalista e alfabético do launcher.
