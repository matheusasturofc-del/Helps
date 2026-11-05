package com.matheus.painel.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Lado do cliente (só quem vai controlar precisa instalar):
 *  - botão "Painel" no menu de pausa (ESC)
 *  - comando /painel no chat
 *  - tecla K (dá pra trocar em Opções > Controles)
 */
public class PainelClient implements ClientModInitializer {
	private static KeyBinding teclaAbrir;

	@Override
	public void onInitializeClient() {
		teclaAbrir = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.painel.abrir", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.painel"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (teclaAbrir.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					client.setScreen(new PainelScreen(null));
				}
			}
		});

		// /painel é um comando só do cliente: não vai pro servidor e não aparece pra ninguém.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(literal("painel").executes(ctx -> {
					MinecraftClient client = ctx.getSource().getClient();
					// Abre no próximo frame, depois que o chat terminar de fechar.
					client.send(() -> client.setScreen(new PainelScreen(null)));
					return 1;
				})));

		// Botão no menu de pausa.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof GameMenuScreen menu && menu.shouldShowMenu()) {
				adicionarBotaoNoPause(client, screen);
			}
		});
	}

	private static void adicionarBotaoNoPause(MinecraftClient client, Screen pause) {
		ButtonWidget botao = ButtonWidget.builder(Text.literal("Painel"),
						b -> client.setScreen(new PainelScreen(pause)))
				.dimensions(6, 6, 70, 20)
				.tooltip(Tooltip.of(Text.literal("Abrir o Painel de Gravação (tecla K ou /painel)")))
				.build();
		Screens.getButtons(pause).add(botao);
	}
}
