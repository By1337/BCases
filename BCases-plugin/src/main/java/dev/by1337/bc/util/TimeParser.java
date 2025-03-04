package dev.by1337.bc.util;

public class TimeParser {

    public static long parse(String s) {
        StringBuilder number = new StringBuilder();
        StringBuilder type = new StringBuilder();
        long out = 0;
        char[] arr = s.toCharArray();

        for (char c : arr) {
            if (Character.isDigit(c)) {
                if (!type.isEmpty() && !number.isEmpty()) {
                    out += getResult(Integer.parseInt(number.toString()), type.toString());
                    number = new StringBuilder();
                    type = new StringBuilder();
                }
                number.append(c);
            } else {
                type.append(c);
            }
        }
        if (!type.isEmpty() && !number.isEmpty()) {
            out += getResult(Integer.parseInt(number.toString()), type.toString());
        }
        return out;
    }

    private static long getResult(int x, String s) {
        return switch (s) {
            case "s" -> 1000L * x;
            case "m" -> 60000L * x;
            case "h" -> 3600000L * x;
            case "d" -> 86400000L * x;
            case "w" -> 604800000L * x;
            case "mo" -> 2629746000L * x;
            case "y" -> 31556908800L * x;
            default -> 0;
        };
    }
}