package com.thatguyjack.bannerfallCharters.integrations.bannerfall;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.thatguyjack.bannerfallCharters.BannerfallCharters;

public class BannersbaneMessageRewriter {
    private final BannerfallCharters plugin;

    public BannersbaneMessageRewriter(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            plugin.getLogger().warning("ProtocolLib not found. Bannersbane message rewriting is disabled.");
            return;
        }

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.SYSTEM_CHAT,
                PacketType.Play.Server.DISGUISED_CHAT,
                PacketType.Play.Server.CHAT,
                PacketType.Play.Server.SET_TITLE_TEXT,
                PacketType.Play.Server.SET_SUBTITLE_TEXT,
                PacketType.Play.Server.SET_ACTION_BAR_TEXT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketType type = event.getPacketType();

                plugin.getLogger().info("[BannersbaneDebug] Saw packet: " + type.name());

                rewriteStrings(event, type);
                rewriteChatComponents(event, type);
                debugPacket(event, type);
            }
        });

        plugin.getLogger().info("Bannersbane message rewriter enabled.");
    }

    private void rewriteStrings(PacketEvent event, PacketType type) {
        for (int i = 0; i < event.getPacket().getStrings().size(); i++) {
            String original = event.getPacket().getStrings().readSafely(i);

            if (original == null) {
                continue;
            }

            plugin.getLogger().info("[BannersbaneDebug] " + type.name() + " string[" + i + "]: " + original);

            String replaced = BannersbaneTextSkin.apply(original);

            if (!original.equals(replaced)) {
                plugin.getLogger().info("[BannersbaneDebug] Rewriting string[" + i + "]: " + replaced);
                event.getPacket().getStrings().writeSafely(i, replaced);
            }
        }
    }

    private void rewriteChatComponents(PacketEvent event, PacketType type) {
        for (int i = 0; i < event.getPacket().getChatComponents().size(); i++) {
            WrappedChatComponent component = event.getPacket().getChatComponents().readSafely(i);

            if (component == null) {
                continue;
            }

            String originalJson = component.getJson();

            if (originalJson == null) {
                continue;
            }

            plugin.getLogger().info("[BannersbaneDebug] " + type.name() + " chatComponent[" + i + "]: " + originalJson);

            String replacedJson = BannersbaneTextSkin.apply(originalJson);

            if (!originalJson.equals(replacedJson)) {
                plugin.getLogger().info("[BannersbaneDebug] Rewriting chatComponent[" + i + "]: " + replacedJson);

                event.getPacket().getChatComponents().writeSafely(
                        i,
                        WrappedChatComponent.fromJson(replacedJson)
                );
            }
        }
    }

    private void debugPacket(PacketEvent event, PacketType type) {
        try {
            Object handle = event.getPacket().getHandle();

            plugin.getLogger().info("[BannersbaneDebug] " + type.name() + " handle class: " + handle.getClass().getName());
            plugin.getLogger().info("[BannersbaneDebug] " + type.name() + " packet dump: " + event.getPacket());
        } catch (Exception exception) {
            plugin.getLogger().warning("[BannersbaneDebug] Could not dump packet: " + exception.getMessage());
        }
    }
}