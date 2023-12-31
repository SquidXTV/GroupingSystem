package me.squidxtv.groupingsystem.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Timespan(int days, int hours, int minutes, int seconds) {

    private static final Pattern MATCH_TIMESPAN = Pattern.compile("^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$");

    public Timespan {
        if (days < 0) {
            throw new IllegalArgumentException("Days must be positive.");
        }
        if (hours < 0 || hours > 24) {
            throw new IllegalArgumentException("Hours must be in range [0;24].");
        }
        if (minutes < 0 || minutes > 60) {
            throw new IllegalArgumentException("Minutes must be in range [0;60].");
        }
        if (seconds < 0 || seconds > 60) {
            throw new IllegalArgumentException("Seconds must be in range [0;60].");
        }
    }

    public Timestamp toFutureTimestamp() {
        return new Timestamp(System.currentTimeMillis()
                + 1000L * seconds
                + 60_000L * minutes
                + 3_600_000L * hours
                + 86_400_000L * days);
    }

    public static Timespan parseTimespan(Player player, String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = MATCH_TIMESPAN.matcher(input);
        if (!matcher.find()) {
            return null;
        }

        int days = Integer.parseInt(Optional.ofNullable(matcher.group(1)).orElse("0"));
        int hours = Integer.parseInt(Optional.ofNullable(matcher.group(2)).orElse("0"));
        int minutes = Integer.parseInt(Optional.ofNullable(matcher.group(3)).orElse("0"));
        int seconds = Integer.parseInt(Optional.ofNullable(matcher.group(4)).orElse("0"));

        try {
            return new Timespan(days, hours, minutes, seconds);
        } catch (IllegalArgumentException e) {
            Component component = Component.text(e.getMessage());
            player.sendMessage(component);
        }

        return null;
    }

}