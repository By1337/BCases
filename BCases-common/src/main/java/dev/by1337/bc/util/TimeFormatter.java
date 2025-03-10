package dev.by1337.bc.util;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;

public class TimeFormatter {
    public static final Codec<TimeFormatter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("ago").forGetter(TimeFormatter::ago),
            Codec.STRING.fieldOf("in").forGetter(TimeFormatter::in),
            Codec.STRING.fieldOf("just-now").forGetter(TimeFormatter::justNow),
            Formats.CODEC.fieldOf("formats").forGetter(TimeFormatter::formats),
            WordForm.CODEC.fieldOf("years").forGetter(TimeFormatter::years),
            WordForm.CODEC.fieldOf("months").forGetter(TimeFormatter::months),
            WordForm.CODEC.fieldOf("days").forGetter(TimeFormatter::days),
            WordForm.CODEC.fieldOf("hours").forGetter(TimeFormatter::hours),
            WordForm.CODEC.fieldOf("minutes").forGetter(TimeFormatter::minutes),
            WordForm.CODEC.fieldOf("seconds").forGetter(TimeFormatter::seconds)
    ).apply(instance, TimeFormatter::new));

    private final String ago;
    private final String in;
    private final String justNow;
    private final Formats formats;
    private final WordForm years;
    private final WordForm months;
    private final WordForm days;
    private final WordForm hours;
    private final WordForm minutes;
    private final WordForm seconds;

    public TimeFormatter(String ago, String in, String justNow, Formats formats, WordForm years, WordForm months, WordForm days, WordForm hours, WordForm minutes, WordForm seconds) {
        this.ago = ago;
        this.in = in;
        this.justNow = justNow;
        this.formats = formats;
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }

    public String getFormat(long time) {
        return getFormat(time, true);
    }

    public String getFormat(long time, boolean prefix) {
        long currentTimeMillis = System.currentTimeMillis();
        long timeDifferenceMillis = currentTimeMillis - time;
        timeDifferenceMillis = timeDifferenceMillis < 0 ? -timeDifferenceMillis : timeDifferenceMillis;

        long seconds = timeDifferenceMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        long years = months / 12;

        if (timeDifferenceMillis > 999) {
            minutes %= 60;
            seconds %= 60;
            hours %= 24;
            days %= 30;
            months %= 12;
            String formattedTime = formatTime(years, months, days, hours, minutes, seconds);
            if (!prefix) return formattedTime;
            if (time < currentTimeMillis) {
                return formattedTime + " " + ago;
            } else {
                return in + " " + formattedTime;
            }

        } else {
            return justNow;
        }
    }

    private String formatTime(long years, long months, long days, long hours, long minutes, long seconds) {
        String str = getFormat(years, months, days, hours, minutes, seconds);
        if (years != 0) {
            str = str.replace("%years%", String.format("%s %s", years, getPluralForm(years, this.years.form1, this.years.form2, this.years.form5)));
        } else
            str = str.replace("%years%", "");
        if (months != 0)
            str = str.replace("%months%", String.format("%s %s", months, getPluralForm(months, this.months.form1, this.months.form2, this.months.form5)));
        else
            str = str.replace("%months%", "");
        if (days != 0)
            str = str.replace("%days%", String.format("%s %s", days, getPluralForm(days, this.days.form1, this.days.form2, this.days.form5)));
        else
            str = str.replace("%days%", "");
        if (hours != 0)
            str = str.replace("%hours%", String.format("%s %s", hours, getPluralForm(hours, this.hours.form1, this.hours.form2, this.hours.form5)));
        else
            str = str.replace("%hours%", "");
        if (minutes != 0)
            str = str.replace("%minutes%", String.format("%s %s", minutes, getPluralForm(minutes, this.minutes.form1, this.minutes.form2, this.minutes.form5)));
        else
            str = str.replace("%minutes%", "");
        if (seconds != 0)
            str = str.replace("%seconds%", String.format("%s %s", seconds, getPluralForm(seconds, this.seconds.form1, this.seconds.form2, this.seconds.form5)));
        else
            str = str.replace("%seconds%", "");
        return str;
    }

    private String getPluralForm(long number, String form1, String form2, String form5) {
        number = Math.abs(number);
        long lastDigit = number % 10;
        long lastTwoDigits = number % 100;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
            return form5;
        } else if (lastDigit == 1) {
            return form1;
        } else if (lastDigit >= 2 && lastDigit <= 4) {
            return form2;
        } else {
            return form5;
        }
    }

    private String getFormat(long years, long months, long days, long hours, long minutes, long seconds) {
        if (years != 0) {
            return formats.years;
        } else if (months != 0) {
            return formats.months;
        } else if (days != 0) {
            return formats.days;
        } else if (hours != 0) {
            return formats.hours;
        } else if (minutes != 0) {
            return formats.minutes;
        }
        return formats.seconds;
    }

    public String ago() {
        return ago;
    }

    public String in() {
        return in;
    }

    public String justNow() {
        return justNow;
    }

    public Formats formats() {
        return formats;
    }

    public WordForm years() {
        return years;
    }

    public WordForm months() {
        return months;
    }

    public WordForm days() {
        return days;
    }

    public WordForm hours() {
        return hours;
    }

    public WordForm minutes() {
        return minutes;
    }

    public WordForm seconds() {
        return seconds;
    }

    public record WordForm(String form1, String form2, String form5) {
        public static final Codec<WordForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("form-1").forGetter(WordForm::form1),
                Codec.STRING.fieldOf("form-2").forGetter(WordForm::form2),
                Codec.STRING.fieldOf("form-5").forGetter(WordForm::form5)
        ).apply(instance, WordForm::new));
    }

    public record Formats(String years, String months, String days, String hours, String minutes, String seconds) {
        public static final Codec<Formats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("years").forGetter(Formats::years),
                Codec.STRING.fieldOf("months").forGetter(Formats::months),
                Codec.STRING.fieldOf("days").forGetter(Formats::days),
                Codec.STRING.fieldOf("hours").forGetter(Formats::hours),
                Codec.STRING.fieldOf("minutes").forGetter(Formats::minutes),
                Codec.STRING.fieldOf("seconds").forGetter(Formats::seconds)
        ).apply(instance, Formats::new));
    }
}
