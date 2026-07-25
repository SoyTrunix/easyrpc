package com.soytrunix;

import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class EasyRPCClient implements ClientModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("easyrpc");
    private static final String CLIENT_ID = "1520287571744133241";
    private static Instant startTime;
    private static String playerName = "Unknown Player";
    private static GameStatus currentStatus = GameStatus.MAIN_MENU;
    private static boolean initialized = false;
    
    private enum GameStatus {
        MAIN_MENU("► Main Menu"),
        SINGLEPLAYER("► Playing on Singleplayer"),
        MULTIPLAYER("► Playing on Multiplayer");
        
        private final String displayName;
        
        GameStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("EasyRPC Client: Initializing...");
        
        // Get the real player name from the Minecraft session
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getUser() != null) {
            playerName = client.getUser().getName();
        }

        // Initialize Discord RPC
        initializeDiscord();
        
        // Register event listeners
        registerEvents();
        
        LOGGER.info("EasyRPC Client: Initialized successfully!");
    }
    
    private void initializeDiscord() {
        try {
            // Create event handlers
            DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .setReadyEventHandler((user) -> {
                    LOGGER.info("EasyRPC: Discord RPC ready for user: " + user.username + "#" + user.discriminator);
                })
                .setDisconnectedEventHandler((errorCode, message) -> {
                    LOGGER.warn("EasyRPC: Discord RPC disconnected: " + message);
                })
                .setErroredEventHandler((errorCode, message) -> {
                    LOGGER.error("EasyRPC: Discord RPC error: " + message);
                })
                .build();
            
            // Initialize Discord RPC
            DiscordRPC.discordInitialize(CLIENT_ID, handlers, true);
            
            // Set start time
            startTime = Instant.now();
            initialized = true;
            
            // Create initial presence
            updatePresence();
            
            LOGGER.info("EasyRPC: Discord RPC initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("EasyRPC: Failed to initialize Discord RPC", e);
            initialized = false;
        }
    }
    
    private void registerEvents() {
        // Update presence and run callbacks every tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (initialized) {
                try {
                    // Run Discord callbacks
                    DiscordRPC.discordRunCallbacks();
                    
                    // Update game status
                    updateGameStatus();
                    
                } catch (Exception e) {
                    LOGGER.error("EasyRPC: Error in tick callback", e);
                }
            }
        });
        
        // When joining a server (multiplayer or singleplayer)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (handler != null && handler.getLocalGameProfile() != null) {
                String profileName = handler.getLocalGameProfile().name();
                if (profileName != null && !profileName.isEmpty()) {
                    playerName = profileName;
                }
            }

            if (client != null && client.hasSingleplayerServer()) {
                currentStatus = GameStatus.SINGLEPLAYER;
                LOGGER.info("EasyRPC: Joined singleplayer world as " + playerName);
            } else {
                currentStatus = GameStatus.MULTIPLAYER;
                LOGGER.info("EasyRPC: Joined multiplayer server as " + playerName);
            }
            
            if (initialized) {
                updatePresence();
            }
        });
        
        // When leaving a server/world
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentStatus = GameStatus.MAIN_MENU;
            LOGGER.info("EasyRPC: Disconnected from world/server");
            if (initialized) {
                updatePresence();
            }
        });
    }
    
    private void updateGameStatus() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        GameStatus newStatus = currentStatus;
        
        // Si el jugador NO está dentro de un mundo/nivel (level == null o player == null)
        if (client.player == null) {
            // Si no hay jugador cargado en el mundo, por defecto está en los menús
            newStatus = GameStatus.MAIN_MENU;
        }
        
        if (newStatus != currentStatus) {
            currentStatus = newStatus;
            updatePresence();
        }
    }

    private void updatePresence() {
        if (!initialized) return;
        
        try {
            // Create rich presence
            DiscordRichPresence.Builder presence = new DiscordRichPresence.Builder(currentStatus.getDisplayName());
            
            // Details: Nickname with actual player name
            presence.setDetails("► " + playerName);
            
            // Large image with hover text showing modloader and version
            presence.setBigImage("icon", "Fabric 26.2");
            
            // Set start timestamp for "Time played"
            presence.setStartTimestamps(startTime != null ? startTime.toEpochMilli() / 1000 : 0);
            
            // Update the presence
            DiscordRPC.discordUpdatePresence(presence.build());
            
        } catch (Exception e) {
            LOGGER.error("EasyRPC: Failed to update presence", e);
        }
    }
    
    public static void shutdown() {
        if (initialized) {
            DiscordRPC.discordShutdown();
            LOGGER.info("EasyRPC: Discord RPC shut down");
        }
    }
}