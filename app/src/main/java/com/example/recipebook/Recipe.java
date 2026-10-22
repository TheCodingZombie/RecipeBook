package com.example.recipebook;

import java.util.*;

// Object representation of a recipe.
public class Recipe {
    public long id;
    public String title = "", mainImage = "";
    public int prepMinutes, cookMinutes, additionalMinutes;
    public final ArrayList<String> ingredients = new ArrayList<>();
    public final ArrayList<String> steps = new ArrayList<>();
    public final ArrayList<String> media = new ArrayList<>();

    // helper function for simply returning how much total time a recipe will take.
    public int totalMinutes() { return prepMinutes + cookMinutes + additionalMinutes; }

    // format an ingredient for user interfaces, since ingredients are stored
    // with pipes and showing the original string would just look ugly.
    public static String formatIngredient(String raw) {
        String[] p = raw.split("\\|", -1);
        if (p.length >= 3) return (p[0].isEmpty() ? "" : p[0] + " ") + (p[1].isEmpty() ? "" : p[1] + " ") + p[2];
        return raw;
    }

    // format time to hours and minutes, since the user can input hours and/or minutes for recipe time.
    public static String formatTime(int minutes) {
        if (minutes <= 0) return "0 min";
        int h = minutes / 60, m = minutes % 60;
        if (h > 0 && m > 0) return h + " hr " + m + " min";
        if (h > 0) return h + (h == 1 ? " hr" : " hrs");
        return m + " min";
    }
}
