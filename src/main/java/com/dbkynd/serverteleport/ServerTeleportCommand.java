package com.dbkynd.serverteleport;

import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ServerTeleportCommand implements SimpleCommand {
    private final ProxyServer server;

    private final String langPrefix;
    private final String langUsage;
    private final String langNoServer;
    private final String langNoPermission;
    private final String langPlayerNum;
    private final String langPlayerName;
    private final String langSuccess;

    public ServerTeleportCommand(ProxyServer server, Toml toml) {
        this.server = server;

        // Localization
        Toml lang = toml.getTable("lang");
        this.langPrefix = lang.getString("prefix");
        this.langUsage = lang.getString("usage");
        this.langNoServer = lang.getString("noserver");
        this.langNoPermission = lang.getString("nopermission");
        this.langPlayerNum = lang.getString("player-num");
        this.langPlayerName = lang.getString("player-name");
        this.langSuccess = lang.getString("success");
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource player = invocation.source();
        String[] args = invocation.arguments();

        // Permission Validation
        if (!player.hasPermission("servertp")) {
            player.sendMessage(Component.text(langPrefix + langNoPermission));
            return;
        }

        // Argument Validation
        if (args.length < 2) {
            player.sendMessage(Component.text(langPrefix + langUsage));
            return;
        }
        String srcArg = args[0];
        String dstArg = args[1];

        // Destination Validation
        Optional<RegisteredServer> dstOptional = dstArg.startsWith("#")
                ? this.server.getServer(dstArg.substring(1))
                : this.server.getPlayer(dstArg).flatMap(Player::getCurrentServer).map(ServerConnection::getServer);
        if (dstOptional.isEmpty()) {
            player.sendMessage(Component.text(langPrefix + langNoServer));
            return;
        }
        RegisteredServer dst = dstOptional.get();

        // Source Validation
        List<Player> src = (
                srcArg.startsWith("#")
                        ? this.server.getServer(srcArg.substring(1)).map(RegisteredServer::getPlayersConnected).orElseGet(Collections::emptyList)
                        : "@a".equals(srcArg)
                        ? this.server.getAllPlayers()
                        : this.server.getPlayer(srcArg).map(Arrays::asList).orElseGet(Collections::emptyList)
        )
                .stream()
                .filter(p -> !dstOptional.equals(p.getCurrentServer().map(ServerConnection::getServer)))
                .collect(Collectors.toList());

        // Send Message
        player.sendMessage(Component.text(langPrefix)
                .append(Component.text(String.format(langSuccess,
                        dstArg,
                        src.size() == 1
                                ? String.format(langPlayerName, src.get(0).getUsername())
                                : String.format(langPlayerNum, src.size())
                ))));

        // Run Redirect
        src.forEach(p ->
                p.createConnectionRequest(dst).fireAndForget());
    }

    private List<String> candidate(String arg, List<String> candidates) {
        if (arg.isEmpty())
            return candidates;
        if (candidates.contains(arg))
            return List.of(arg);
        return candidates.stream().filter(e -> e.startsWith(arg)).collect(Collectors.toList());
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        CommandSource player = invocation.source();
        String[] args = invocation.arguments();

        // Permission Validation
        if (!player.hasPermission("servertp"))
            return Collections.emptyList();

        // Source Suggestion
        if (args.length == 1)
            return candidate(args[0],
                    Stream.of(
                                    Stream.of("@a"),
                                    this.server.getAllServers().stream().map(RegisteredServer::getServerInfo)
                                            .map(ServerInfo::getName).map(e -> "#" + e),
                                    this.server.getAllPlayers().stream().map(Player::getUsername)
                            )
                            .flatMap(Function.identity())
                            .collect(Collectors.toList())
            );

        // Destination Suggestion
        if (args.length == 2)
            return candidate(args[1],
                    Stream.of(
                                    this.server.getAllServers().stream().map(RegisteredServer::getServerInfo)
                                            .map(ServerInfo::getName).map(e -> "#" + e),
                                    this.server.getAllPlayers().stream().map(Player::getUsername)
                            )
                            .flatMap(Function.identity())
                            .collect(Collectors.toList())
            );

        return Collections.emptyList();
    }
}
