package com.matheus.painel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lado do servidor: registra o comando /painelcmd.
 *
 * Só usa tipos de argumento do vanilla, então quem entra no mundo
 * (LAN ou servidor) NÃO precisa ter o mod instalado.
 */
public class PainelMod implements ModInitializer {
	public static final String MOD_ID = "painel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> PainelCommands.register(dispatcher));
		LOGGER.info("Painel de Gravação carregado.");
	}
}
