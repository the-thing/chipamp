package com.github.thething.chipgroove.common;

import com.github.thething.chipgroove.mod.Effect;
import com.github.thething.chipgroove.mod.Mod;
import com.github.thething.chipgroove.mod.ModTables;
import com.github.thething.chipgroove.mod.Instrument;

// TODO add new column (sound) for volume and compare with MPT (not sure yet)
public final class Formatters {

    private static final char[] HEX_NIBBLES = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final String[] HEX_BYTES = new String[]{
            "00", "01", "02", "03", "04", "05", "06", "07",
            "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
            "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
            "20", "21", "22", "23", "24", "25", "26", "27",
            "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
            "30", "31", "32", "33", "34", "35", "36", "37",
            "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
            "40", "41", "42", "43", "44", "45", "46", "47",
            "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
            "50", "51", "52", "53", "54", "55", "56", "57",
            "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
            "60", "61", "62", "63", "64", "65", "66", "67",
            "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
            "70", "71", "72", "73", "74", "75", "76", "77",
            "78", "79", "7A", "7B", "7C", "7D", "7E", "7F",
            "80", "81", "82", "83", "84", "85", "86", "87",
            "88", "89", "8A", "8B", "8C", "8D", "8E", "8F",
            "90", "91", "92", "93", "94", "95", "96", "97",
            "98", "99", "9A", "9B", "9C", "9D", "9E", "9F",
            "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7",
            "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF",
            "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7",
            "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF",
            "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7",
            "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF",
            "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7",
            "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF",
            "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7",
            "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
            "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
            "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF"
    };

    private Formatters() {
    }

    public static String formatPatterns(Mod mod) {
        StringBuilder out = new StringBuilder();
        formatPatterns(mod, out);
        return out.toString();
    }

    public static void formatPatterns(Mod mod, StringBuilder out) {
        for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
            formatPattern(mod, patternIndex, out);
        }
    }

    public static String formatPattern(Mod mod, int patternIndex) {
        StringBuilder out = new StringBuilder();
        formatPattern(mod, patternIndex, out);
        return out.toString();
    }

    public static void formatPattern(Mod mod, int patternIndex, StringBuilder out) {
        for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
            formatRow(mod, patternIndex, rowIndex, out);
            out.append("\r\n");
        }
    }

    public static String formatRow(Mod mod, int patternIndex, int rowIndex) {
        StringBuilder out = new StringBuilder();
        formatRow(mod, patternIndex, rowIndex, out);
        return out.toString();
    }

    public static void formatRow(Mod mod, int patternIndex, int rowIndex, StringBuilder out) {
        int offset = patternIndex * Mod.ROW_COUNT + rowIndex;

        formatHexInt(offset, out);
        out.append(" |");

        out.append(' ');
        out.append(formatHexByte(patternIndex));
        out.append(" |");

        out.append(' ');
        out.append(formatHexByte(rowIndex));
        out.append(" |");

        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Instrument pattern = mod.getInstrument(patternIndex, rowIndex, channelIndex);
            String note = ModTables.getNote(pattern.period());
            note = note != null ? note : "---";

            // TODO check if there are any modules where non standard note samples are used
            // TODD if found, we probably want to search for the closest note and print it as a custom note e.g "C-X"

            int sampleInt = pattern.sampleNumber();
            String sample;

            if (sampleInt == 0) {
                sample = "--";
            } else {
                sample = formatHexByte(sampleInt);
            }

            char effect;
            String effectArgument;

            if (pattern.effect() == Effect.NONE) {
                effect = '-';
                effectArgument = "--";
            } else {
                effect = getHexCharacter(pattern.effect().getCode());
                effectArgument = formatHexByte(pattern.effectArgumentX(), pattern.effectArgumentY());
            }

            out.append(" ");
            out.append(note);
            out.append(' ');
            out.append(sample);
            out.append(' ');
            out.append(effect);
            out.append(effectArgument);
            out.append(" |");
        }
    }

    public static String formatHexByte(int highNibble, int lowNibble) {
        return formatHexByte(((highNibble & 0x0F) << 4) | (lowNibble & 0x0F));
    }

    public static String formatHexByte(int value) {
        return HEX_BYTES[value & 0xFF];
    }

    public static String formatHexInt(int value) {
        StringBuilder out = new StringBuilder(8);
        formatHexInt(value, out);
        return out.toString();
    }

    public static void formatHexInt(int value, StringBuilder out) {
        out.append(HEX_BYTES[value >> 24 & 0xFF]);
        out.append(HEX_BYTES[value >> 16 & 0xFF]);
        out.append(HEX_BYTES[value >> 8 & 0xFF]);
        out.append(HEX_BYTES[value & 0xFF]);
    }

    public static char getHexCharacter(int value) {
        return HEX_NIBBLES[value & 0x0F];
    }

    public static String formatEffects(Mod mod, int patternIndex, int rowIndex) {
        StringBuilder out = new StringBuilder();
        formatEffects(mod, patternIndex, rowIndex, out);
        return out.toString();
    }

    public static void formatEffects(Mod mod, int patternIndex, int rowIndex, StringBuilder out) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);

            if (channelIndex != 0) {
                out.append(" / ");
            }

            if (instrument.effect() != Effect.EXTENDED_EFFECT) {
                out.append(instrument.effect());
            } else {
                out.append(instrument.extendedEffect());
            }
        }
    }
}
