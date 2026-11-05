package com.matheus.painel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /painelcmd <ação> [alvo]
 * /painelcmd mundo <dia|noite|limpo|chuva>
 *
 * alvo = nome do jogador, "eu" ou "todos" (se omitido, é você).
 *
 * Os comandos são silenciosos: não mandam nada no chat de ninguém, para não
 * aparecer na gravação. Quem confirma a ação é a própria interface do painel.
 */
public final class PainelCommands {
	public static final String COMANDO = "painelcmd";

	private static final SimpleCommandExceptionType JOGADOR_NAO_ENCONTRADO =
			new SimpleCommandExceptionType(Text.literal("Jogador não encontrado."));
	private static final SimpleCommandExceptionType PRECISA_SER_JOGADOR =
			new SimpleCommandExceptionType(Text.literal("Essa ação precisa ser usada por um jogador."));

	private static final SuggestionProvider<ServerCommandSource> SUGERIR_ALVOS = (ctx, builder) -> {
		List<String> opcoes = new ArrayList<>(List.of("eu", "todos"));
		opcoes.addAll(List.of(ctx.getSource().getServer().getPlayerNames()));
		return CommandSource.suggestMatching(opcoes, builder);
	};

	private PainelCommands() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> raiz = literal(COMANDO).requires(PainelCommands::podeUsar);

		acao(raiz, "curar", (src, p) -> {
			p.setHealth(p.getMaxHealth());
			p.extinguish();
		});
		acao(raiz, "restaurar", (src, p) -> {
			p.setHealth(p.getMaxHealth());
			p.extinguish();
			p.setFrozenTicks(0);
			p.clearStatusEffects();
			encherFome(p);
		});
		acao(raiz, "quasemorto", (src, p) -> p.setHealth(1.0F));
		acao(raiz, "matar", (src, p) -> p.kill());
		acao(raiz, "saciar", (src, p) -> encherFome(p));
		acao(raiz, "fome", (src, p) -> definirFome(p, 0));
		acao(raiz, "meiafome", (src, p) -> definirFome(p, 6));
		acao(raiz, "limparefeitos", (src, p) -> p.clearStatusEffects());

		acao(raiz, "voar", (src, p) -> {
			if (p.isCreative() || p.isSpectator()) {
				return; // nesses modos o voo já é controlado pelo jogo
			}
			PlayerAbilities hab = p.getAbilities();
			hab.allowFlying = !hab.allowFlying;
			if (!hab.allowFlying) {
				hab.flying = false;
			}
			p.sendAbilitiesUpdate();
		});
		acao(raiz, "deus", (src, p) -> {
			if (p.isCreative() || p.isSpectator()) {
				return;
			}
			PlayerAbilities hab = p.getAbilities();
			hab.invulnerable = !hab.invulnerable;
			p.sendAbilitiesUpdate();
		});

		acao(raiz, "sobrevivencia", (src, p) -> p.changeGameMode(GameMode.SURVIVAL));
		acao(raiz, "criativo", (src, p) -> p.changeGameMode(GameMode.CREATIVE));
		acao(raiz, "espectador", (src, p) -> p.changeGameMode(GameMode.SPECTATOR));

		// Traz o alvo até você.
		acao(raiz, "puxar", (src, p) -> {
			ServerPlayerEntity eu = src.getPlayer();
			if (eu != null && eu != p) {
				p.teleport(eu.getServerWorld(), eu.getX(), eu.getY(), eu.getZ(), eu.getYaw(), eu.getPitch());
			}
		});
		// Leva você até o alvo.
		acao(raiz, "irpara", (src, p) -> {
			ServerPlayerEntity eu = src.getPlayer();
			if (eu != null && eu != p) {
				eu.teleport(p.getServerWorld(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
			}
		});

		raiz.then(literal("mundo")
				.then(literal("dia").executes(ctx -> horario(ctx, 1000)))
				.then(literal("tarde").executes(ctx -> horario(ctx, 9000)))
				.then(literal("noite").executes(ctx -> horario(ctx, 13000)))
				.then(literal("limpo").executes(ctx -> clima(ctx, false, false)))
				.then(literal("chuva").executes(ctx -> clima(ctx, true, false)))
				.then(literal("tempestade").executes(ctx -> clima(ctx, true, true))));

		dispatcher.register(raiz);
	}

	/**
	 * Pode usar: quem é OP (nível 2+), o console, ou o dono do mundo em LAN/singleplayer
	 * mesmo com "Permitir comandos" desligado.
	 */
	private static boolean podeUsar(ServerCommandSource src) {
		if (src.hasPermissionLevel(2)) {
			return true;
		}
		ServerPlayerEntity jogador = src.getPlayer();
		return jogador != null && src.getServer().isHost(jogador.getGameProfile());
	}

	private static void acao(LiteralArgumentBuilder<ServerCommandSource> raiz, String nome,
			BiConsumer<ServerCommandSource, ServerPlayerEntity> efeito) {
		raiz.then(literal(nome)
				.executes(ctx -> aplicar(ctx, "eu", efeito))
				.then(argument("alvo", StringArgumentType.word())
						.suggests(SUGERIR_ALVOS)
						.executes(ctx -> aplicar(ctx, StringArgumentType.getString(ctx, "alvo"), efeito))));
	}

	private static int aplicar(CommandContext<ServerCommandSource> ctx, String alvo,
			BiConsumer<ServerCommandSource, ServerPlayerEntity> efeito) throws CommandSyntaxException {
		ServerCommandSource src = ctx.getSource();
		Collection<ServerPlayerEntity> alvos = resolverAlvos(src, alvo);
		for (ServerPlayerEntity p : alvos) {
			efeito.accept(src, p);
		}
		return alvos.size();
	}

	private static Collection<ServerPlayerEntity> resolverAlvos(ServerCommandSource src, String alvo)
			throws CommandSyntaxException {
		MinecraftServer server = src.getServer();
		if (alvo.equalsIgnoreCase("todos")) {
			return List.copyOf(server.getPlayerManager().getPlayerList());
		}
		if (alvo.equalsIgnoreCase("eu")) {
			ServerPlayerEntity eu = src.getPlayer();
			if (eu == null) {
				throw PRECISA_SER_JOGADOR.create();
			}
			return List.of(eu);
		}
		ServerPlayerEntity p = server.getPlayerManager().getPlayer(alvo);
		if (p == null) {
			throw JOGADOR_NAO_ENCONTRADO.create();
		}
		return List.of(p);
	}

	private static void encherFome(ServerPlayerEntity p) {
		HungerManager fome = p.getHungerManager();
		fome.setFoodLevel(20);
		fome.setSaturationLevel(20.0F);
	}

	private static void definirFome(ServerPlayerEntity p, int nivel) {
		HungerManager fome = p.getHungerManager();
		fome.setFoodLevel(nivel);
		fome.setSaturationLevel(0.0F);
	}

	/** Muda o horário sem zerar a contagem de dias (sempre avança no tempo). */
	private static int horario(CommandContext<ServerCommandSource> ctx, long alvoNoDia) {
		for (ServerWorld mundo : ctx.getSource().getServer().getWorlds()) {
			long agora = mundo.getTimeOfDay();
			long novo = agora - Math.floorMod(agora, 24000L) + alvoNoDia;
			if (novo < agora) {
				novo += 24000L;
			}
			mundo.setTimeOfDay(novo);
		}
		return 1;
	}

	private static int clima(CommandContext<ServerCommandSource> ctx, boolean chuva, boolean trovoada) {
		ServerWorld mundo = ctx.getSource().getServer().getOverworld();
		int duracao = 20 * 60 * 10; // 10 minutos
		if (chuva) {
			mundo.setWeather(0, duracao, true, trovoada);
		} else {
			mundo.setWeather(duracao, 0, false, false);
		}
		return 1;
	}
}
