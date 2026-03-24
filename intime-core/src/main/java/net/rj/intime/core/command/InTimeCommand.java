package net.rj.intime.core.command;

import net.rj.intime.core.InTimeCorePlugin;
import net.rj.intime.core.util.ChatUtil;
import net.rj.intime.core.util.TimeFormatUtil;
import net.rj.intime.core.util.TimeParseUtil;
import net.rj.intime.core.util.DisplayNameUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InTimeCommand implements CommandExecutor, TabCompleter {

    private final InTimeCorePlugin plugin;

    public InTimeCommand(InTimeCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = ChatUtil.color(plugin.getConfig().getString("messages.prefix", "&8[&6InTime&8] "));

        if (args.length == 0) {
            sender.sendMessage(prefix + "/intime check [player]");
            sender.sendMessage(prefix + "/intime add <player> <seconds>");
            sender.sendMessage(prefix + "/intime remove <player> <seconds>");
            sender.sendMessage(prefix + "/intime set <player> <seconds>");
            sender.sendMessage(prefix + "/intime save");
            sender.sendMessage(prefix + "/intime reload");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "check" -> {
                handleCheck(sender, args, prefix);
                return true;
            }
            case "add", "remove", "set" -> {
                if (!sender.hasPermission("intime.admin")) {
                    sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.no-permission", "&c권한이 없습니다.")));
                    return true;
                }
                handleModify(sender, args, prefix, sub);
                return true;
            }
            case "save" -> {
                if (!sender.hasPermission("intime.admin")) {
                    sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.no-permission", "&c권한이 없습니다.")));
                    return true;
                }

                plugin.getTimeService().saveAllNow();
                sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.saved", "&a저장이 완료되었습니다.")));
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("intime.admin")) {
                    sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.no-permission", "&c권한이 없습니다.")));
                    return true;
                }

                plugin.reloadConfig();
                sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.reloaded", "&a리로드 완료.")));
                return true;
            }
            default -> {
                sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.unknown-command", "&c알 수 없는 명령어입니다.")));
                return true;
            }
        }
    }

    private void handleCheck(CommandSender sender, String[] args, String prefix) {
        UUID uuid;
        String targetName;

        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + ChatUtil.color("&c콘솔은 플레이어를 지정해야 합니다."));
                return;
            }
            uuid = player.getUniqueId();
            targetName = player.getName();
        } else {
            Optional<UUID> found = plugin.getTimeService().findUuidByName(args[1]);
            if (found.isEmpty()) {
                sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.player-not-found", "&c플레이어를 찾을 수 없습니다.")));
                return;
            }
            uuid = found.get();
            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            String baseName = target.getName() == null ? args[1] : target.getName();
            targetName = DisplayNameUtil.getDisplayName(plugin, uuid, baseName);
        }

        long seconds = plugin.getTimeService().getRemainingSeconds(uuid);
        String smart = TimeFormatUtil.formatSmart(seconds);
        String fixed = TimeFormatUtil.formatFixed(seconds);

        sender.sendMessage(prefix + ChatUtil.color("&e" + targetName + "&f의 남은 시간: &a" + smart + " &7(" + fixed + ", " + seconds + "s)"));
    }

    private void handleModify(CommandSender sender, String[] args, String prefix, String mode) {
        if (args.length < 3) {
            sender.sendMessage(prefix + ChatUtil.color("&c사용법: /intime " + mode + " <player> <seconds>"));
            return;
        }

        Optional<UUID> found = plugin.getTimeService().findUuidByName(args[1]);
        if (found.isEmpty()) {
            sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.player-not-found", "&c플레이어를 찾을 수 없습니다.")));
            return;
        }

        long seconds;
        try {
            seconds = TimeParseUtil.parseToSeconds(args[2]);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(prefix + ChatUtil.color(plugin.getConfig().getString("messages.invalid-time-format", "&c올바른 시간 형식이 아닙니다. 예: 10, 30s, 5m, 1h10m")));
            return;
        }

        UUID uuid = found.get();
        OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
        String baseName = target.getName() == null ? args[1] : target.getName();
        String targetName = DisplayNameUtil.getDisplayName(plugin, uuid, baseName);

        switch (mode) {
            case "add" -> plugin.getTimeService().addSeconds(uuid, seconds);
            case "remove" -> plugin.getTimeService().removeSeconds(uuid, seconds);
            case "set" -> plugin.getTimeService().setRemainingSeconds(uuid, seconds);
            default -> {
                sender.sendMessage(prefix + ChatUtil.color("&c알 수 없는 모드입니다."));
                return;
            }
        }

        long now = plugin.getTimeService().getRemainingSeconds(uuid);
        sender.sendMessage(prefix + ChatUtil.color("&e" + targetName + "&f의 시간이 변경되었습니다. 현재: &a" + TimeFormatUtil.formatSmart(now)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(args[0], List.of("check", "add", "remove", "set", "save", "reload"));
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("save") && !args[0].equalsIgnoreCase("reload")) {
            Set<String> names = Stream.concat(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName),
                    Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).filter(Objects::nonNull)
            ).collect(Collectors.toCollection(TreeSet::new));

            return filterStartsWith(args[1], new ArrayList<>(names));
        }

        return Collections.emptyList();
    }

    private List<String> filterStartsWith(String input, List<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
