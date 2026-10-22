
package com.example.recipebook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class RecipeDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "recipes.db";
    private static final int DB_VERSION = 1;

    public RecipeDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE recipes (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "title TEXT NOT NULL, " +
                        "main_image TEXT, " +
                        "prep_minutes INTEGER DEFAULT 0, " +
                        "cook_minutes INTEGER DEFAULT 0, " +
                        "additional_minutes INTEGER DEFAULT 0, " +
                        "details_json TEXT)"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Add migrations here if the schema changes in a future version.
    }

    // create a JSON ingredient from an ingredient string.
    // ingredient strings are formatted as such: "quantity|unit|name".
    // All parts are stored as basic strings to preserve certain data
    // aka quantity allowing fractional quantities like 1/4 and decimal
    // quantities like 1.5.
    private JSONObject ingredientToJson(String raw) {
        String[] parts = raw.split("\\|", -1);

        JSONObject ingredient = new JSONObject();
        try {
            ingredient.put("quantity",
                    parts.length > 0 ? parts[0] : "");
            ingredient.put("unit",
                    parts.length > 1 ? parts[1] : "");
            ingredient.put("name",
                    parts.length > 2 ? parts[2] : raw);
        } catch (Exception ignored) {
        }

        return ingredient;
    }

    // get all parts of the recipe besides the title and main image.
    private String detailsToJson(Recipe recipe) {
        JSONObject details = new JSONObject();

        try {
            // grab all ingredients, converting them to JSON first.
            JSONArray ingredients = new JSONArray();
            for (String ingredient : recipe.ingredients) {
                ingredients.put(ingredientToJson(ingredient));
            }

            // then grab all steps, which are just strings.
            JSONArray steps = new JSONArray();
            for (String step : recipe.steps) {
                steps.put(step);
            }

            // then grab every other image besides the main image, stored as paths.
            JSONArray media = new JSONArray();
            for (String path : recipe.media) {
                media.put(path);
            }

            // build out the details from these parts.
            details.put("ingredients", ingredients);
            details.put("steps", steps);
            details.put("media", media);
        } catch (Exception ignored) {
        }

        // then return the details as a JSON formatted string.
        return details.toString();
    }

    // get all values of a recipe.
    private ContentValues values(Recipe recipe) {
        ContentValues values = new ContentValues();

        values.put("title", recipe.title);
        values.put("main_image", recipe.mainImage);
        values.put("prep_minutes", recipe.prepMinutes);
        values.put("cook_minutes", recipe.cookMinutes);
        values.put("additional_minutes", recipe.additionalMinutes);
        values.put("details_json", detailsToJson(recipe));

        return values;
    }

    // save a recipe to the database.
    public long save(Recipe recipe) {
        // open the database
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = values(recipe);

        if (recipe.id > 0) {
            int updated = db.update(
                    "recipes",
                    values,
                    "_id=?",
                    new String[]{String.valueOf(recipe.id)}
            );

            if (updated > 0) {
                return recipe.id;
            }
        }

        long id = db.insert("recipes", null, values);
        if (id > 0) {
            recipe.id = id;
        }
        return id;
    }

    // delete a recipe from the database
    public void delete(long id) {
        getWritableDatabase().delete(
                "recipes",
                "_id=?",
                new String[]{ String.valueOf(id) }
        );
    }

    // translate the JSON into a string array for use in the app.
    private void readStringArray(
            JSONObject details, String key, ArrayList<String> output) {

        JSONArray array = details.optJSONArray(key);
        if (array == null) return;

        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);

            if (item instanceof JSONObject && key.equals("ingredients")) {
                JSONObject ingredient = (JSONObject) item;

                String quantity = ingredient.optString("quantity", "");
                String unit = ingredient.optString("unit", "");
                String name = ingredient.optString("name", "");

                output.add(quantity + "|" + unit + "|" + name);
            } else if (item != null && item != JSONObject.NULL) {
                output.add(String.valueOf(item));
            }
        }
    }

    // grab a Recipe from the database, creating a Recipe object representation of it
    // for use inside the app.
    private Recipe read(Cursor cursor) {
        Recipe recipe = new Recipe();

        recipe.id = cursor.getLong(
                cursor.getColumnIndexOrThrow("_id"));
        recipe.title = cursor.getString(
                cursor.getColumnIndexOrThrow("title"));
        recipe.mainImage = cursor.getString(
                cursor.getColumnIndexOrThrow("main_image"));
        recipe.prepMinutes = cursor.getInt(
                cursor.getColumnIndexOrThrow("prep_minutes"));
        recipe.cookMinutes = cursor.getInt(
                cursor.getColumnIndexOrThrow("cook_minutes"));
        recipe.additionalMinutes = cursor.getInt(
                cursor.getColumnIndexOrThrow("additional_minutes"));

        String json = cursor.getString(
                cursor.getColumnIndexOrThrow("details_json"));

        if (json != null && !json.isEmpty()) {
            try {
                JSONObject details = new JSONObject(json);

                readStringArray(details, "ingredients", recipe.ingredients);
                readStringArray(details, "steps", recipe.steps);
                readStringArray(details, "media", recipe.media);

            } catch (Exception e) {
                android.util.Log.e(
                        "RecipeDatabase",
                        "Could not parse recipe details for ID " + recipe.id,
                        e
                );
            }
        }

        return recipe;
    }

    public Recipe get(long id) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM recipes WHERE _id=?",
                new String[]{String.valueOf(id)})) {

            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    // Search the database for recipes with titles matching the query.
    // This searches for any recipes with titles containing the query,
    // NOT titles exactly like the query. i.e. searching "uh" in the
    // database will provide all recipes with "uh" contained somewhere
    // in the recipe title, not just all recipes that have the title "uh".
    public ArrayList<Recipe> search(String query) {
        ArrayList<Recipe> results = new ArrayList<>();

        String search = query == null
                ? ""
                : query.toLowerCase(Locale.ROOT);

        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM recipes " +
                        "WHERE lower(title) LIKE ? " +
                        "ORDER BY title COLLATE NOCASE",
                new String[]{"%" + search + "%"})) {

            while (cursor.moveToNext()) {
                results.add(read(cursor));
            }
        }

        return results;
    }
}
