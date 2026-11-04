# Nexus — assistente pessoal de IA (Android)

Projeto completo do app Nexus: interface estilo HUD, chat e voz em português,
IA que decide quando controlar dispositivos/música/abrir conteúdo (tool-use
real, não palavras-chave fixas), bolinha flutuante, rastreamento de mão pela
câmera, sons e efeitos visuais originais.

## Como instalar sem computador (só com o celular)

Você não precisa de PC nem do Claude Code. O GitHub compila o APK pra você
na nuvem, de graça. Passo a passo:

1. Crie uma conta grátis em **github.com** (pelo navegador do celular).
2. Toque em **New repository**, dê um nome (ex: `nexus-ai`) e marque como
   **Public** (necessário para o plano de Actions grátis funcionar sem
   limites). Crie o repositório.
3. Dentro do repositório, toque em **Add file → Upload files** e envie TODOS
   os arquivos e pastas deste projeto (mantendo a mesma estrutura de pastas).
   Dica: no navegador, você pode arrastar a pasta inteira ou selecionar tudo
   de uma vez.
4. Confirme o envio (**Commit changes**).
5. Vá na aba **Actions** do repositório. Um workflow chamado "Build Nexus
   APK" vai começar sozinho (ou toque em **Run workflow** se não começar).
6. Espere uns 3–6 minutos. Quando o círculo ficar verde, toque na execução
   concluída e desça até **Artifacts** → toque em **nexus-apk** para baixar
   um .zip.
7. Abra o .zip pelo gerenciador de arquivos do celular — dentro tem o
   `app-debug.apk`. Toque nele para instalar (o Android vai pedir para
   permitir instalação de fontes desconhecidas na primeira vez).

Pronto — o Nexus estará instalado no seu POCO C75.

## Antes de usar

- Abra **⚙ Configurações** dentro do app e cole sua chave de API da
  Anthropic (criada em console.anthropic.com). Sem ela, o chat funciona
  mas avisa que não há IA configurada — o uso da API é cobrado pela
  Anthropic, por token.
- Para transmitir a tela do Nexus na sua TV Philco (Roku), use o botão
  **📺 Transmitir p/ TV** — ele abre o espelhamento de tela nativo do
  Android (Smart View/Cast). A Roku não aceita instalar o Nexus nela
  diretamente, porque não é Android.

## O que está incluído

- Chat e voz (reconhecimento de fala + texto por voz) em português.
- IA com tool-use: decide sozinha quando controlar um dispositivo, tocar/
  pausar música ou abrir um site, em vez de regras fixas de palavra-chave.
- Dispositivos HTTP (tomadas/relés tipo Tasmota/ESPHome) — cadastre em
  **Dispositivos**.
- Controle de mídia (play/pause/parar/próxima/anterior) via tecla de mídia
  do Android — funciona com Spotify, YouTube Music etc.
- **Nexus Visão**: tela dedicada que usa a câmera traseira para detectar
  sua mão em tempo real e desenhar o esqueleto sobre a imagem (usa
  MediaPipe; baixa um modelo pequeno na primeira vez que você abre a tela).
- Bolinha flutuante (acesso rápido, igual ao seu app Otimizador).
- Orbe central com 4 estados visuais (parado/ouvindo/pensando/falando) e
  efeitos sonoros originais sintetizados (nenhum áudio de terceiros).
- Chave de API guardada criptografada no aparelho.

## O que ficou de fora desta versão (e por quê)

- **App nativo na TV Roku**: tecnicamente impossível — Roku não roda
  Android nem aceita instalar apps externos.
- **Nexus controlando outros jogos (VR/motion control em apps de terceiros)**:
  exigiria invadir a renderização/controles de outro app, o que o Android
  bloqueia por segurança (e seria o tipo de injeção que quebra anti-cheat).
- **PC Windows na nuvem, app de iPhone, integração com Roblox Studio,
  "hackear" quem invadir sua conta**: precisam de infraestrutura paga,
  conta de desenvolvedor Apple, ou são ilegais (invasão de sistema de
  terceiros), respectivamente.
- Reconhecimento de objetos (além da mão), gestos configuráveis (pinça,
  punho fechado etc.) e PIN de bloqueio no app ficam como próximos passos.

## Se o build do GitHub Actions falhar

A parte de rastreamento de mão usa a biblioteca MediaPipe, que muda nomes de
método entre versões — se o erro aparecer especificamente em
`VisionActivity.java` ou `VisionOverlayView.java`, volte nesta conversa e
cole o erro: ajusto rapidamente.
