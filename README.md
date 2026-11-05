# Painel de Gravação (Fabric 1.21.1)

Um painel com botões no estilo do Minecraft para controlar o jogo durante a gravação,
sem aparecer nada no chat.

**Só quem controla precisa instalar.** O mod roda no servidor do seu mundo
(quando você abre em LAN) e o seu amigo entra com o **Minecraft normal, sem mod nenhum**.
Também não precisa ligar "Permitir comandos".

## Como abrir

- **ESC**: botão **Painel** no canto superior esquerdo do menu de pausa
- **Tecla K** (dá pra trocar em *Opções > Controles > Painel de Gravação*)
- **/painel** no chat (comando local, não vai para o servidor)

## O que tem

Escolha o alvo no botão do topo (**Você**, cada jogador online ou **Todos**) e clique na ação:

| Jogador | | Mundo |
|---|---|---|
| Curar | Voar on/off (sobrevivência) | Dia / Tarde / Noite |
| Restaurar (vida, fome, efeitos, fogo) | Deus on/off (imortal) | Tempo limpo |
| Quase morto (meio coração) | Puxar p/ mim | Chuva / Tempestade |
| Matar (clica 2x para confirmar) | Ir até ele | |
| Saciar / Fome total / Meia fome | Sobrevivência / Criativo / Espectador | |
| Tirar efeitos | | |

O painel **não pausa o jogo**, então o seu amigo continua jogando enquanto ele está aberto.

## Instalação (só no seu PC)

1. Instale o [Fabric Loader](https://fabricmc.net/use/installer/) para o **Minecraft 1.21.1**
2. Coloque na pasta `.minecraft/mods`:
   - `painel-1.0.0.jar`
   - [Fabric API](https://modrinth.com/mod/fabric-api) para 1.21.1
3. Abra o jogo pelo perfil **Fabric 1.21.1**, crie o mundo e abra em LAN
4. Seu amigo entra pelo Minecraft 1.21.1 normal

> Com servidor dedicado (Fabric), coloque o mod + Fabric API também na pasta `mods` do servidor.
> Lá o painel funciona para quem for OP.

## Comandos (se quiser usar sem o painel)

```
/painelcmd <ação> [alvo]          alvo = nome, eu ou todos (padrão: eu)
/painelcmd mundo <dia|tarde|noite|limpo|chuva|tempestade>
```

Ações: `curar`, `restaurar`, `quasemorto`, `matar`, `saciar`, `fome`, `meiafome`,
`limparefeitos`, `voar`, `deus`, `puxar`, `irpara`, `sobrevivencia`, `criativo`, `espectador`.

## Compilar

Precisa de Java 21:

```bash
./gradlew build
```

O `.jar` sai em `build/libs/`.
