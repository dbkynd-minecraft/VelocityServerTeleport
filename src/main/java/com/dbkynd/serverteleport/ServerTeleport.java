package com.dbkynd.serverteleport;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(
        id = "serverteleport",
        name = "Server Teleport",
        version = BuildConstants.VERSION,
        authors = {"DBKynd"}
)
public class ServerTeleport {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private Toml loadConfig(Path path) {
        File folder = path.toFile();
        File file = new File(folder, "config.toml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            try {
                InputStream input = this.getClass().getResourceAsStream("/" + file.getName());
                Throwable t = null;

                try {
                    if (input != null) {
                        Files.copy(input, file.toPath());
                    } else {
                        file.createNewFile();
                    }
                } catch (Throwable e) {
                    t = e;
                    throw e;
                } finally {
                    if (input != null) {
                        if (t != null) {
                            try {
                                input.close();
                            } catch (Throwable ex) {
                                t.addSuppressed(ex);
                            }
                        } else {
                            input.close();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return (new Toml()).read(file);
    }

    @Inject
    public ServerTeleport(ProxyServer server, Logger logger, @DataDirectory Path folder) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = folder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Toml toml = this.loadConfig(dataDirectory);
        if (toml == null) {
            logger.warn("Failed to load config.toml. Shutting down.");
        } else {
            server.getCommandManager().register( "stp", new ServerTeleportCommand(server, toml));
            server.getCommandManager().register( "servertp", new ServerTeleportCommand(server, toml));
            logger.info("Plugin has enabled!");
        }
    }
}
