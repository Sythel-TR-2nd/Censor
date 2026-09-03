package com.sythel.censor.command;

import com.sythel.censor.logging.LogEntry;
import com.sythel.censor.logging.LogService;
import com.sythel.censor.moderation.WordTestService;
import com.sythel.censor.service.ReloadService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CensorCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "censor.admin";
    private static final int PAGE_SIZE = 8;
    private static final int VARIATIONS_PER_LINE = 5;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final ReloadService reloadService;
    private final LogService logService;
    private final WordTestService wordTestService;

    public CensorCommand(
            ReloadService reloadService,
            LogService logService,
            WordTestService wordTestService
    ) {
        this.reloadService = reloadService;
        this.logService = logService;
        this.wordTestService = wordTestService;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(
                    "§cBu komutu kullanmak için yetkiniz yok."
            );
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }

        if (args[0].equalsIgnoreCase("logs")) {
            handleLogs(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {
            handleTest(sender, args);
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        try {
            reloadService.reload();

            sender.sendMessage(
                    "§aCensor yapılandırması başarıyla yenilendi."
            );
        } catch (RuntimeException exception) {
            sender.sendMessage(
                    "§cCensor yapılandırması yenilenirken bir hata oluştu."
            );
        }

        return true;
    }

    private void handleTest(
            CommandSender sender,
            String[] args
    ) {
        if (args.length < 2) {
            sender.sendMessage(
                    "§cKullanım: §7/censor test <engellenen kelime>"
            );
            return;
        }

        String word = args[1].trim();

        if (word.isEmpty()) {
            sender.sendMessage(
                    "§cTest edilecek kelime boş olamaz."
            );
            return;
        }

        if (!wordTestService.isBlockedWord(word)) {
            sender.sendMessage(
                    "§cBu kelime mevcut Censor filtresinde engellenmiyor."
            );
            sender.sendMessage(
                    "§7Önce kelimenin censored.yml içerisinde bulunduğundan emin olun."
            );
            return;
        }

        WordTestService.WordTestResult testResult =
                wordTestService.test(word);

        sendTestHeader(
                sender,
                word,
                testResult
        );

        for (Map.Entry<String, List<WordTestService.VariationResult>> entry :
                testResult.results().entrySet()) {

            sendVariationCategory(
                    sender,
                    entry.getKey(),
                    entry.getValue()
            );
        }

        sendFailedVariations(
                sender,
                testResult
        );

        sendTestSummary(
                sender,
                testResult
        );
    }

    private void sendTestHeader(
            CommandSender sender,
            String word,
            WordTestService.WordTestResult testResult
    ) {
        sender.sendMessage("");
        sender.sendMessage(
                "§8§m--------------------------------"
        );
        sender.sendMessage(
                "§cCensor §7- Kelime Testi"
        );
        sender.sendMessage(
                "§7Kelime: §f"
                        + word
        );
        sender.sendMessage(
                "§7Sistemin ürettiği varyasyon: §f"
                        + testResult.totalVariations()
        );
        sender.sendMessage(
                "§7Gösterilen örnek: §f"
                        + testResult.displayed()
        );
        sender.sendMessage("");
    }

    private void sendVariationCategory(
            CommandSender sender,
            String category,
            List<WordTestService.VariationResult> variations
    ) {
        sender.sendMessage(
                "§e"
                        + category
                        + " §8("
                        + variations.size()
                        + ")"
        );

        StringBuilder line =
                new StringBuilder();

        int count = 0;

        for (WordTestService.VariationResult result :
                variations) {

            String status =
                    result.blocked()
                            ? "§a✓"
                            : "§c✗";

            if (count > 0) {
                line.append("  ");
            }

            line.append(
                    status
                            + "§f"
                            + result.value()
            );

            count++;

            if (count == VARIATIONS_PER_LINE) {
                sender.sendMessage(
                        line.toString()
                );

                line.setLength(0);
                count = 0;
            }
        }

        if (!line.isEmpty()) {
            sender.sendMessage(
                    line.toString()
            );
        }

        sender.sendMessage("");
    }

    private void sendFailedVariations(
            CommandSender sender,
            WordTestService.WordTestResult testResult
    ) {
        if (testResult.failed() == 0) {
            return;
        }

        sender.sendMessage(
                "§cBaşarısız Varyasyonlar §8("
                        + testResult.failed()
                        + ")"
        );

        StringBuilder line =
                new StringBuilder();

        int count = 0;

        for (List<WordTestService.VariationResult> variations :
                testResult.results().values()) {

            for (WordTestService.VariationResult result :
                    variations) {

                if (result.blocked()) {
                    continue;
                }

                if (count > 0) {
                    line.append("  ");
                }

                line.append(
                        "§c✗§f"
                                + result.value()
                );

                count++;

                if (count == VARIATIONS_PER_LINE) {
                    sender.sendMessage(
                            line.toString()
                    );

                    line.setLength(0);
                    count = 0;
                }
            }
        }

        if (!line.isEmpty()) {
            sender.sendMessage(
                    line.toString()
            );
        }

        sender.sendMessage("");
    }

    private void sendTestSummary(
            CommandSender sender,
            WordTestService.WordTestResult testResult
    ) {
        sender.sendMessage(
                "§8§m--------------------------------"
        );

        sender.sendMessage(
                "§7Yakalanan: §a"
                        + testResult.passed()
                        + "§7/§f"
                        + testResult.totalVariations()
        );

        sender.sendMessage(
                "§7Başarısız: §c"
                        + testResult.failed()
        );

        sender.sendMessage(
                "§7Başarı: §f"
                        + String.format(
                        Locale.ROOT,
                        "%.1f",
                        testResult.percentage()
                )
                        + "%"
        );

        sender.sendMessage(
                "§7Gösterilen örnek: §f"
                        + testResult.displayed()
                        + "§7/"
                        + testResult.totalVariations()
        );

        sender.sendMessage(
                "§8§m--------------------------------"
        );
    }

    private void handleLogs(
            CommandSender sender,
            String[] args
    ) {
        String playerName = null;
        int page = 1;

        if (args.length >= 2) {
            if (isNumber(args[1])) {
                page = parsePage(args[1]);
            } else {
                playerName = args[1];
            }
        }

        if (args.length >= 3) {
            if (!isNumber(args[2])) {
                sender.sendMessage(
                        "§cSayfa numarası geçerli değil."
                );
                return;
            }

            page = parsePage(args[2]);
        }

        List<LogEntry> entries = playerName == null
                ? logService.findAll()
                : logService.findByPlayer(playerName);

        sendLogPage(
                sender,
                entries,
                playerName,
                page
        );
    }

    private void sendLogPage(
            CommandSender sender,
            List<LogEntry> entries,
            String playerName,
            int page
    ) {
        if (entries.isEmpty()) {
            sender.sendMessage(
                    "§eGösterilecek moderasyon logu bulunamadı."
            );
            return;
        }

        int totalPages = (int) Math.ceil(
                (double) entries.size() / PAGE_SIZE
        );

        if (page > totalPages) {
            sender.sendMessage(
                    "§cBu sayfa bulunmuyor. §7Toplam sayfa: §f"
                            + totalPages
            );
            return;
        }

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(
                fromIndex + PAGE_SIZE,
                entries.size()
        );

        List<LogEntry> pageEntries = entries.subList(
                fromIndex,
                toIndex
        );

        sender.sendMessage("");
        sender.sendMessage(
                "§8§m--------------------------------"
        );
        sender.sendMessage(
                "§cCensor §7- Moderasyon Logları"
        );

        if (playerName != null) {
            sender.sendMessage(
                    "§7Oyuncu: §f" + playerName
            );
        }

        sender.sendMessage(
                "§7Sayfa: §f"
                        + page
                        + "§7/§f"
                        + totalPages
        );
        sender.sendMessage("");

        for (LogEntry entry : pageEntries) {
            sender.sendMessage(
                    "§8[§7"
                            + entry.timestamp().format(TIME_FORMAT)
                            + "§8] §f"
                            + entry.playerName()
            );

            sender.sendMessage(
                    " §7Tür: §f"
                            + String.join(
                                    "§7, §f",
                                    entry.types()
                            )
            );

            sender.sendMessage(
                    " §7Orijinal: §c"
                            + entry.originalMessage()
            );

            sender.sendMessage(
                    " §7Sansürlü: §a"
                            + entry.censoredMessage()
            );

            sender.sendMessage("");
        }

        sender.sendMessage(
                "§8§m--------------------------------"
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of(
                            "reload",
                            "logs",
                            "test"
                    )
                    .stream()
                    .filter(value ->
                            value.startsWith(
                                    args[0].toLowerCase()
                            )
                    )
                    .toList();
        }

        if (args.length == 2
                && args[0].equalsIgnoreCase("logs")) {

            List<String> suggestions = new ArrayList<>();
            suggestions.add("1");

            return suggestions.stream()
                    .filter(value ->
                            value.startsWith(args[1])
                    )
                    .toList();
        }

        return List.of();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§cKullanım:");
        sender.sendMessage("§7/censor reload");
        sender.sendMessage("§7/censor logs");
        sender.sendMessage("§7/censor logs <sayfa>");
        sender.sendMessage("§7/censor logs <oyuncu>");
        sender.sendMessage(
                "§7/censor logs <oyuncu> <sayfa>"
        );
        sender.sendMessage(
                "§7/censor test <engellenen kelime>"
        );
    }

    private boolean isNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private int parsePage(String value) {
        try {
            int page = Integer.parseInt(value);
            return Math.max(page, 1);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }
}