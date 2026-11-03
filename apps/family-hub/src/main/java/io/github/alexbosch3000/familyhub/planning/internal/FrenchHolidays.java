// Public holidays of metropolitan France, computed from the date of Easter.
package io.github.alexbosch3000.familyhub.planning.internal;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.TreeMap;

public final class FrenchHolidays {

    private FrenchHolidays() {
    }

    /** The eleven public holidays of a year, by date. */
    public static Map<LocalDate, String> of(int year) {
        LocalDate easter = easterSunday(year);
        Map<LocalDate, String> holidays = new TreeMap<>();
        holidays.put(LocalDate.of(year, Month.JANUARY, 1), "Jour de l'an");
        holidays.put(easter.plusDays(1), "Lundi de Pâques");
        holidays.put(LocalDate.of(year, Month.MAY, 1), "Fête du Travail");
        holidays.put(LocalDate.of(year, Month.MAY, 8), "Victoire 1945");
        holidays.put(easter.plusDays(39), "Ascension");
        holidays.put(easter.plusDays(50), "Lundi de Pentecôte");
        holidays.put(LocalDate.of(year, Month.JULY, 14), "Fête nationale");
        holidays.put(LocalDate.of(year, Month.AUGUST, 15), "Assomption");
        holidays.put(LocalDate.of(year, Month.NOVEMBER, 1), "Toussaint");
        holidays.put(LocalDate.of(year, Month.NOVEMBER, 11), "Armistice 1918");
        holidays.put(LocalDate.of(year, Month.DECEMBER, 25), "Noël");
        return holidays;
    }

    /** Gregorian Easter Sunday (anonymous algorithm, also known as Meeus/Jones/Butcher). */
    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }
}
