package com.example.recipebook;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.FileProvider;
import java.io.*;
import java.util.zip.*;
import org.json.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class RecipeViewActivity extends AppCompatActivity {
    RecipeDatabase db;
    Recipe recipe;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_recipe_view);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (v,insets)->{
                    android.graphics.Insets x = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
                    v.setPadding(0,x.top,0,x.bottom);
                    return insets;
                }
        );

        db = new RecipeDatabase(this);
        recipe = db.get(getIntent().getLongExtra("id",-1));

        // cannot show a recipe that doesn't exist
        if (recipe == null) {
            finish();
            return;
        }

        findViewById(R.id.closeView).setOnClickListener(v->finish());
        findViewById(R.id.viewTitle).setTransitionName("recipeCard");
        ((TextView)findViewById(R.id.viewTitle)).setText(recipe.title);

        ImageView image = findViewById(R.id.viewImage);
        image.setImageResource(R.drawable.recipe_placeholder);
        if (recipe.mainImage != null && !recipe.mainImage.isEmpty()) {
            image.setImageURI(Uri.parse(recipe.mainImage));
        }

        StringBuilder ing = new StringBuilder();
        for (String s:recipe.ingredients) {
            ing.append("• ").append(Recipe.formatIngredient(s)).append("\n");
        }

        ((TextView)findViewById(R.id.viewIngredients)).setText(ing.length() == 0 ? "No ingredients added." : ing.toString().trim());

        ((TextView)findViewById(R.id.viewTimes)).setText(
                "Prep: "+Recipe.formatTime(recipe.prepMinutes)+
                "\nCook: "+Recipe.formatTime(recipe.cookMinutes)+
                "\nAdditional: "+Recipe.formatTime(recipe.additionalMinutes)
                +"\n\nTotal: "+Recipe.formatTime(recipe.totalMinutes())
        );

        StringBuilder steps = new StringBuilder();
        for(int i = 0; i < recipe.steps.size(); i++) {
            steps.append(i + 1).append(". ").append(recipe.steps.get(i)).append("\n\n");
        }
        ((TextView)findViewById(R.id.viewSteps)).setText(steps.length() == 0 ? "No steps added." : steps.toString().trim());

        LinearLayout media = findViewById(R.id.viewMedia);
        for (String path:recipe.media) {
            ImageView iv = new ImageView(this);
            iv.setAdjustViewBounds(true);
            iv.setImageURI(Uri.parse(path));
            media.addView(iv,new LinearLayout.LayoutParams(-1,160));
        }

        findViewById(R.id.editRecipe).setOnClickListener(v->{
            Intent i = new Intent(this,RecipeEditActivity.class);
            i.putExtra("id",recipe.id);
            startActivity(i);
            finish();
        });
        findViewById(R.id.deleteRecipe).setOnClickListener(
        v->new AlertDialog.Builder(this)
                .setTitle("Delete recipe?")
                .setMessage("This permanently deletes "+recipe.title+".")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Delete",(d,w)->{
                    db.delete(recipe.id);
                    finish();
                }
            ).show()
        );
        findViewById(R.id.shareRecipe).setOnClickListener(v->share());
    }

    // This function builds a JSON representation of the recipe used for sharing with other people.
    private JSONObject recipeJson(File exportDir) throws Exception {
        JSONObject j = new JSONObject();

        j.put("format", "recipe-book");
        j.put("version", 1);
        j.put("title", recipe.title);

        j.put("prepMinutes", recipe.prepMinutes);
        j.put("cookMinutes", recipe.cookMinutes);
        j.put("additionalMinutes", recipe.additionalMinutes);

        // Ingredients
        JSONArray ingredients = new JSONArray();

        for (String raw : recipe.ingredients) {
            String[] p = raw.split("\\|", -1);

            JSONObject ingredient = new JSONObject();
            ingredient.put("quantity", p.length > 0 ? p[0] : "");
            ingredient.put("unit", p.length > 1 ? p[1] : "");
            ingredient.put("name", p.length > 2 ? p[2] : raw);

            ingredients.put(ingredient);
        }

        j.put("ingredients", ingredients);

        // Steps
        j.put("steps", new JSONArray(recipe.steps));

        // Main image
        j.put("mainImage",
                copyImageForExport(recipe.mainImage, "main", exportDir));

        // Additional images
        JSONArray media = new JSONArray();

        for (int i = 0; i < recipe.media.size(); i++) {
            media.put(copyImageForExport(
                    recipe.media.get(i),
                    "media" + i,
                    exportDir
            ));
        }

        j.put("media", media);

        return j;
    }

    // grabs the image path and returns it for sharing purposes.
    private String copyImageToShare(String source,String name){
        // do not copy an empty image
        if (source == null || source.isEmpty()) return "";

        try {
            File dir = new File(getCacheDir(),"shared");
            dir.mkdirs();
            File dest = new File(dir,name+extension(source));
            try(InputStream in = getContentResolver().openInputStream(Uri.parse(source));
                OutputStream out = new FileOutputStream(dest)) {
                byte[] b = new byte[8192];
                int n;
                while((n = in.read(b)) > 0) {
                    out.write(b, 0, n);
                }
            }
            return dest.getName();
        }
        catch(Exception e){
            return "";
        }
    }
    private String extension(String s) {
        String p = Uri.parse(s).getLastPathSegment();
        if (p != null && p.contains(".")) {
            return p.substring(p.lastIndexOf('.'));
        }
        // by default, all images are jpgs
        return ".jpg";
    }
    // A function used for building a recipe up for sharing purposes.
    private void share() {
        File dir = null;

        try {
            dir = getShareExportsDir();

            // Remove leftovers from previous exports.
            File[] oldFiles = dir.listFiles();

            if (oldFiles != null) {
                for (File file : oldFiles) {
                    if (file.isFile() && !file.delete()) {
                        throw new IOException(
                                "Could not clean old export file: "
                                        + file.getName()
                        );
                    }
                }
            }

            // Build JSON and copy images into the export directory.
            JSONObject json = recipeJson(dir);

            File jsonFile = new File(dir, "recipe.json");

            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(json.toString(2));
            }

            // Create the ZIP in app-private storage.
            File zip = new File(
                    dir,
                    "recipe-" + System.currentTimeMillis() + ".zip"
            );

            try (ZipOutputStream out = new ZipOutputStream(
                    new FileOutputStream(zip))) {

                File[] files = dir.listFiles();

                if (files == null) {
                    throw new IOException(
                            "Could not list export files."
                    );
                }

                for (File file : files) {
                    if (!file.isFile() || file.equals(zip)) {
                        continue;
                    }

                    out.putNextEntry(new ZipEntry(file.getName()));

                    try (FileInputStream in =
                                 new FileInputStream(file)) {

                        byte[] buffer = new byte[8192];
                        int count;

                        while ((count = in.read(buffer)) != -1) {
                            out.write(buffer, 0, count);
                        }
                    }

                    out.closeEntry();
                }
            }

            // Share the ZIP using FileProvider.
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    zip
            );

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/zip");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(
                    send,
                    "Share recipe"
            ));

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Could not export recipe: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // Helper function to get/create the images directory
    private File getRecipeImagesDir() {
        File dir = new File(getFilesDir(), "recipe_images");

        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create recipe images directory"
            );
        }

        return dir;
    }

    // Helper function to get/create the recipe exports directory
    private File getShareExportsDir() {
        File dir = new File(getFilesDir(), "share_exports");

        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create share exports directory"
            );
        }

        return dir;
    }

    // Copy an image from a recipe to the app's private storage so it doesn't get deleted randomly
    private File copyImageToPrivateStorage(
            String imageSource,
            File destination
    ) throws IOException {

        if (imageSource == null || imageSource.trim().isEmpty()) {
            throw new IOException("Image source is empty.");
        }

        File parent = destination.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create image export directory.");
        }

        Uri uri = Uri.parse(imageSource);
        String scheme = uri.getScheme();

        InputStream input;

        if (scheme == null) {
            // A plain filesystem path, such as /data/.../image.jpg
            input = new FileInputStream(imageSource);
        } else if ("content".equalsIgnoreCase(scheme)) {
            // An image supplied through Android's content provider.
            input = getContentResolver().openInputStream(uri);

            if (input == null) {
                throw new IOException("Could not open image URI: " + imageSource);
            }
        } else if ("file".equalsIgnoreCase(scheme)) {
            // A file:// URI.
            input = new FileInputStream(new File(uri.getPath()));
        } else {
            throw new IOException("Unsupported image source: " + scheme);
        }

        try (InputStream in = input;
             OutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int count;

            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }

        return destination;
    }


    private String copyImageForExport(
            String source,
            String name,
            File exportDir
    ) throws IOException {

        if (source == null || source.trim().isEmpty()) {
            return "";
        }

        String ext = extension(source);

        // Keep a persistent private copy of the image.
        File permanentDir = getRecipeImagesDir();
        File permanentImage = new File(
                permanentDir,
                name + "_" + java.util.UUID.randomUUID() + ext
        );

        copyImageToPrivateStorage(source, permanentImage);

        // Copy into this export's staging directory.
        // The filename in JSON must match the ZIP entry.
        File exportImage = new File(exportDir, permanentImage.getName());

        copyImageToPrivateStorage(
                permanentImage.getAbsolutePath(),
                exportImage
        );

        return exportImage.getName();
    }

}
