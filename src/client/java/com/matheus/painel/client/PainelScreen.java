package com.matheus.painel.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface do painel, feita só com botões do próprio Minecraft.
 * Cada botão manda um /painelcmd silencioso para o servidor.
 */
public class PainelScreen extends Screen {
	private static final String COMANDO = "painelcmd";

	private static final int COLUNAS = 4;
	private static final int LARGURA_BOTAO = 76;
	private static final int ALTURA_BOTAO = 20;
	private static final int ESPACO = 4;

	/** Ações de jogador: rótulo, subcomando, dica. */
	private static final String[][] ACOES_JOGADOR = {
			{"Curar", "curar", "Enche a vida e apaga o fogo"},
			{"Restaurar", "restaurar", "Vida cheia, fome cheia, sem efeitos e sem fogo"},
			{"Quase morto", "quasemorto", "Deixa com meio coração"},
			{"Matar", "matar", "Mata o jogador (clique duas vezes para confirmar)"},
			{"Saciar", "saciar", "Enche a barra de fome"},
			{"Fome total", "fome", "Zera a fome (começa a perder vida)"},
			{"Meia fome", "meiafome", "Deixa com 3 coxas (não consegue correr)"},
			{"Tirar efeitos", "limparefeitos", "Remove todos os efeitos de poção"},
			{"Voar on/off", "voar", "Liga/desliga o voo no sobrevivência (dois toques no espaço)"},
			{"Deus on/off", "deus", "Liga/desliga a imortalidade no sobrevivência"},
			{"Puxar p/ mim", "puxar", "Teleporta o jogador até você"},
			{"Ir até ele", "irpara", "Teleporta você até o jogador"},
			{"Sobrevivência", "sobrevivencia", "Muda para o modo sobrevivência"},
			{"Criativo", "criativo", "Muda para o modo criativo"},
			{"Espectador", "espectador", "Muda para o modo espectador"},
	};

	/** Ações de mundo: rótulo, subcomando. */
	private static final String[][] ACOES_MUNDO = {
			{"Dia", "dia"},
			{"Tarde", "tarde"},
			{"Noite", "noite"},
			{"Tempo limpo", "limpo"},
			{"Chuva", "chuva"},
			{"Tempestade", "tempestade"},
	};

	/** Lembra o último alvo escolhido enquanto o jogo estiver aberto. */
	private static String ultimoAlvo = "eu";

	@Nullable
	private final Screen anterior;

	private final List<String> alvos = new ArrayList<>();
	private int indiceAlvo;

	private ButtonWidget botaoAlvo;
	private final List<ButtonWidget> botoesSoOutroJogador = new ArrayList<>();
	private ButtonWidget botaoMatar;
	private long confirmarMatarAte;

	private Text status = Text.empty();
	private int topo;
	private int yTituloJogador;
	private int yTituloMundo;
	private int yStatus;

	public PainelScreen(@Nullable Screen anterior) {
		super(Text.literal("Painel de Gravação"));
		this.anterior = anterior;
	}

	@Override
	protected void init() {
		carregarAlvos();
		botoesSoOutroJogador.clear();

		int linhasJogador = (ACOES_JOGADOR.length + COLUNAS - 1) / COLUNAS;
		int linhasMundo = (ACOES_MUNDO.length + COLUNAS - 1) / COLUNAS;
		int passo = ALTURA_BOTAO + 2;
		int alturaTotal = 14 + 24 + 12 + linhasJogador * passo + 6 + 12 + linhasMundo * passo + 16 + ALTURA_BOTAO;
		topo = Math.max(4, (height - alturaTotal) / 2);

		int larguraGrade = COLUNAS * LARGURA_BOTAO + (COLUNAS - 1) * ESPACO;
		int esquerda = (width - larguraGrade) / 2;
		int y = topo + 14;

		// Escolha do alvo
		botaoAlvo = addDrawableChild(ButtonWidget.builder(textoAlvo(), b -> trocarAlvo())
				.dimensions(esquerda, y, larguraGrade, ALTURA_BOTAO)
				.tooltip(Tooltip.of(Text.literal("Clique para trocar o jogador alvo")))
				.build());
		y += 24;

		// Jogador
		yTituloJogador = y;
		y += 12;
		for (int i = 0; i < ACOES_JOGADOR.length; i++) {
			String[] a = ACOES_JOGADOR[i];
			int x = esquerda + (i % COLUNAS) * (LARGURA_BOTAO + ESPACO);
			int by = y + (i / COLUNAS) * passo;
			String sub = a[1];
			ButtonWidget botao = addDrawableChild(ButtonWidget.builder(Text.literal(a[0]),
							b -> clicouAcaoJogador(b, a[0], sub))
					.dimensions(x, by, LARGURA_BOTAO, ALTURA_BOTAO)
					.tooltip(Tooltip.of(Text.literal(a[2])))
					.build());
			if (sub.equals("puxar") || sub.equals("irpara")) {
				botoesSoOutroJogador.add(botao);
			}
			if (sub.equals("matar")) {
				botaoMatar = botao;
			}
		}
		y += linhasJogador * passo + 6;

		// Mundo
		yTituloMundo = y;
		y += 12;
		for (int i = 0; i < ACOES_MUNDO.length; i++) {
			String[] a = ACOES_MUNDO[i];
			int x = esquerda + (i % COLUNAS) * (LARGURA_BOTAO + ESPACO);
			int by = y + (i / COLUNAS) * passo;
			addDrawableChild(ButtonWidget.builder(Text.literal(a[0]), b -> {
						enviar("mundo " + a[1]);
						definirStatus(a[0], null);
					})
					.dimensions(x, by, LARGURA_BOTAO, ALTURA_BOTAO)
					.build());
		}
		y += linhasMundo * passo;

		yStatus = y + 4;
		y += 16;

		addDrawableChild(ButtonWidget.builder(Text.literal("Pronto"), b -> close())
				.dimensions(width / 2 - 100, y, 200, ALTURA_BOTAO)
				.build());

		atualizarBotoes();
	}

	private void carregarAlvos() {
		alvos.clear();
		alvos.add("eu");
		ClientPlayNetworkHandler rede = client == null ? null : client.getNetworkHandler();
		String meuNome = client != null && client.player != null ? client.player.getGameProfile().getName() : "";
		if (rede != null) {
			List<String> outros = new ArrayList<>();
			for (PlayerListEntry entrada : rede.getPlayerList()) {
				String nome = entrada.getProfile().getName();
				if (!nome.equalsIgnoreCase(meuNome)) {
					outros.add(nome);
				}
			}
			outros.sort(String.CASE_INSENSITIVE_ORDER);
			alvos.addAll(outros);
		}
		alvos.add("todos");

		indiceAlvo = Math.max(0, alvos.indexOf(ultimoAlvo));
		ultimoAlvo = alvos.get(indiceAlvo);
	}

	private void trocarAlvo() {
		carregarAlvos(); // pega quem entrou/saiu desde que abriu
		indiceAlvo = (indiceAlvo + 1) % alvos.size();
		ultimoAlvo = alvos.get(indiceAlvo);
		botaoAlvo.setMessage(textoAlvo());
		atualizarBotoes();
	}

	private String alvoAtual() {
		return alvos.get(indiceAlvo);
	}

	private Text textoAlvo() {
		return Text.literal("Alvo: ").append(Text.literal(nomeBonito(alvoAtual())).formatted(Formatting.YELLOW));
	}

	private static String nomeBonito(String alvo) {
		return switch (alvo) {
			case "eu" -> "Você";
			case "todos" -> "Todos";
			default -> alvo;
		};
	}

	private void atualizarBotoes() {
		boolean outroJogador = !alvoAtual().equals("eu") && !alvoAtual().equals("todos");
		for (ButtonWidget b : botoesSoOutroJogador) {
			b.active = outroJogador;
		}
		confirmarMatarAte = 0;
		if (botaoMatar != null) {
			botaoMatar.setMessage(Text.literal("Matar"));
		}
	}

	private void clicouAcaoJogador(ButtonWidget botao, String rotulo, String sub) {
		if (sub.equals("matar")) {
			long agora = System.currentTimeMillis();
			if (agora > confirmarMatarAte) {
				confirmarMatarAte = agora + 3000;
				botao.setMessage(Text.literal("Certeza?").formatted(Formatting.RED));
				return;
			}
			confirmarMatarAte = 0;
			botao.setMessage(Text.literal("Matar"));
		}
		enviar(sub + " " + alvoAtual());
		definirStatus(rotulo, alvoAtual());
	}

	private void enviar(String argumentos) {
		if (client != null && client.player != null) {
			client.player.networkHandler.sendChatCommand(COMANDO + " " + argumentos);
		}
	}

	private void definirStatus(String acao, @Nullable String alvo) {
		Text t = Text.literal("✔ " + acao).formatted(Formatting.GREEN);
		if (alvo != null) {
			t = t.copy().append(Text.literal(" → " + nomeBonito(alvo)).formatted(Formatting.GRAY));
		}
		status = t;
	}

	@Override
	public void tick() {
		super.tick();
		if (confirmarMatarAte != 0 && System.currentTimeMillis() > confirmarMatarAte && botaoMatar != null) {
			confirmarMatarAte = 0;
			botaoMatar.setMessage(Text.literal("Matar"));
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, topo, 0xFFFFFF);

		int larguraGrade = COLUNAS * LARGURA_BOTAO + (COLUNAS - 1) * ESPACO;
		int esquerda = (width - larguraGrade) / 2;
		context.drawTextWithShadow(textRenderer, Text.literal("Jogador").formatted(Formatting.GOLD),
				esquerda, yTituloJogador + 2, 0xFFFFFF);
		context.drawTextWithShadow(textRenderer, Text.literal("Mundo").formatted(Formatting.GOLD),
				esquerda, yTituloMundo + 2, 0xFFFFFF);
		context.drawCenteredTextWithShadow(textRenderer, status, width / 2, yStatus, 0xFFFFFF);
	}

	@Override
	public boolean shouldPause() {
		// Não pausa o jogo: o amigo continua jogando normalmente.
		return false;
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(anterior);
		}
	}
}
