package com.example.recipebook;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class RecipeEditActivity extends AppCompatActivity {
    RecipeDatabase db;
    Recipe recipe = new Recipe();
    EditText title, prepH,prepM,cookH,cookM,addH,addM;
    TextView total;
    LinearLayout ingredientsBox,stepsBox,mediaBox;
    ImageView mainImage;
    String mainPath = "";
    ArrayList<String> mediaPaths=new ArrayList<>();
    ActivityResultLauncher<String> imagePicker,mainPicker;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);setContentView(R.layout.activity_recipe_edit);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v,insets)->{
                    android.graphics.Insets x= insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
                    v.setPadding(0,x.top,0,x.bottom);return insets;
                });

        db = new RecipeDatabase(this);

        // Recipe Title and Main
        title = findViewById(R.id.titleInput);
        mainImage=findViewById(R.id.editMainImage);


        // Time variables
        prepH=findViewById(R.id.prepHours);
        prepM=findViewById(R.id.prepMinutes);

        cookH=findViewById(R.id.cookHours);
        cookM=findViewById(R.id.cookMinutes);

        addH=findViewById(R.id.additionalHours);
        addM=findViewById(R.id.additionalMinutes);

        total=findViewById(R.id.totalTime);

        ingredientsBox=findViewById(R.id.ingredientsContainer);
        stepsBox=findViewById(R.id.stepsContainer);
        mediaBox=findViewById(R.id.mediaContainer);

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri->{
                    if (uri!=null) {
                        mediaPaths.add(uri.toString());renderMedia();
                    }
                }
        );
        mainPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri->{
                    if (uri!=null) {
                        try{
                            getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                        catch (Exception ignored) {

                        }
                        mainPath=uri.toString();mainImage.setImageURI(uri);
                    }
                });

        long id = getIntent().getLongExtra("id",-1);
        if (id>0) {
            Recipe loaded=db.get(id);
            if (loaded!=null) recipe = loaded;
        }
        else if (getIntent().getAction()!=null) {
            importShared(getIntent());
        }

        // Title management
        title.setText(recipe.title);
        mainPath=recipe.mainImage==null?"":recipe.mainImage;
        if (!mainPath.isEmpty()) mainImage.setImageURI(Uri.parse(mainPath));

        // Time management
        setTime(prepH,prepM,recipe.prepMinutes);
        setTime(cookH,cookM,recipe.cookMinutes);
        setTime(addH,addM,recipe.additionalMinutes);

        // Steps and Ingredients
        for (String s:recipe.ingredients) {
            addIngredientRow(s);
        }
        for (String s:recipe.steps) {
            addStepRow(s);
        }
        mediaPaths.addAll(recipe.media);renderMedia();updateTotal();

        TextWatcher watcher = new TextWatcher(){
            public void beforeTextChanged(CharSequence s, int st, int c, int a){}
            public void onTextChanged(CharSequence s, int st, int before, int count){
                updateTotal();
            }
            public void afterTextChanged(android.text.Editable e){}
        };

        // setting listeners for click events
        for (EditText e:new EditText[]{prepH,prepM,cookH,cookM,addH,addM}) {
            e.addTextChangedListener(watcher);
        }
        findViewById(R.id.chooseMainImage).setOnClickListener(v->mainPicker.launch("image/*"));
        findViewById(R.id.addIngredient).setOnClickListener(v->addIngredientRow(""));
        findViewById(R.id.addStep).setOnClickListener(v->addStepRow(""));
        findViewById(R.id.addMedia).setOnClickListener(v->imagePicker.launch("image/*"));
        findViewById(R.id.cancelEdit).setOnClickListener(v->finish());
        findViewById(R.id.confirmEdit).setOnClickListener(v->save());
    }
    private void setTime(EditText h,EditText m,int total){
        h.setText(String.valueOf(total/60));
        m.setText(String.valueOf(total%60));
    }
    private int value(EditText e){
        try{
            return Math.max(0,Integer.parseInt(e.getText().toString().trim()));
        }
        catch (Exception ex){
            return 0;
        }
    }
    private void updateTotal(){
        if (total!=null) {
            total.setText("Total: " + Recipe.formatTime(
        value(prepH) * 60 + value(prepM) + value(cookH) * 60 + value(cookM) + value(addH) * 60 + value(addM))
            );
        }
    }
    private EditText field(String hint,String value,int input) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setInputType(input);
        e.setMinHeight(dp(48));
        e.setPadding(dp(10),dp(6),dp(10),dp(6));
        return e;
    }
    private int dp(int n) {
        return (int)(n*getResources().getDisplayMetrics().density+.5f);
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(8), dp(6), dp(8), dp(6));

        // Use a solid cream background instead of the recipe image.
        l.setBackgroundColor(android.graphics.Color.parseColor("#FFF9F1"));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(4), 0, dp(4));
        l.setLayoutParams(p);

        return l;
    }

    private void addIngredientRow(String raw){
        String[] p = raw.split("\\|",-1);
        String qty = p.length>0?p[0]:"",unit=p.length>1?p[1]:"",name=p.length>2?p[2]:raw;

        LinearLayout r = row();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        EditText q = field(
                "Qty (e.g. 1.5)",
                qty,
                android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        Spinner units = new Spinner(this);
        String[] options={
                "unit",
                "tsp","tbsp",
                "cup","fl oz","pint","quart","gallon","oz","lb",
                "mg","g","kg",
                "mL","L",
                "pinch", "dash", "peck",
                "clove","slice","can","package"};
        units.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options));
        int ix = Arrays.asList(options).indexOf(unit);
        if (ix >= 0) {
            units.setSelection(ix);
        }
        top.addView(q,new LinearLayout.LayoutParams(0,-2,1));
        top.addView(units,new LinearLayout.LayoutParams(0,-2,1));

        EditText n = field(
            "Ingredient name",
            name,
        android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        Button remove = new Button(this);
        remove.setText("Delete Ingredient");
        remove.setMinHeight(dp(48));
        remove.setOnClickListener(v->{
            ingredientsBox.removeView(r);
        });

        r.addView(top);
        r.addView(n);
        r.addView(remove);

        r.setTag(new EditText[]{q,n});
        r.setContentDescription("UNIT:"+units.toString());
        r.setTag(R.id.addIngredient,new Object[]{q,units,n});
        ingredientsBox.addView(r);
    }
    private void addStepRow(String text){
        LinearLayout r = row();
        EditText e = field(
                "Describe this step",
                text,
                android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setMinLines(2);

        Button remove = new Button(this);
        remove.setText("Delete Step");
        remove.setMinHeight(dp(48));
        remove.setOnClickListener(v->{
            stepsBox.removeView(r);
        });
        r.addView(e);
        r.addView(remove);
        r.setTag(e);
        stepsBox.addView(r);
    }
    private void renderMedia(){
        // clear all views so we don't get old ones.
        mediaBox.removeAllViews();
        // add each image one by one.
        for (int i = 0; i < mediaPaths.size(); i++) {
            final int index = i;
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);

            ImageView iv = new ImageView(this);
            iv.setImageURI(Uri.parse(mediaPaths.get(i)));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

            r.addView(iv, new LinearLayout.LayoutParams(dp(96),dp(80)));

            // add a button to remove said image.
            Button remove = new Button(this);
            remove.setText("Remove");
            remove.setMinHeight(dp(48));
            remove.setOnClickListener(v->{
                mediaPaths.remove(index);renderMedia();
            });

            r.addView(remove);
            mediaBox.addView(r);
        }
    }
    private void save(){
        String name = title.getText().toString().trim();
        if (name.isEmpty()) {
            // Cannot have an empty title for a recipe.
            title.setError("Enter a recipe title");
            title.requestFocus();
            return;
        }

        // Now we build a recipe.
        recipe.title = name;
        recipe.mainImage = mainPath;

        // Time (converting hours and minutes to just minutes)
        recipe.prepMinutes = value(prepH)*60+value(prepM);
        recipe.cookMinutes = value(cookH)*60+value(cookM);
        recipe.additionalMinutes = value(addH)*60+value(addM);

        // Get every ingredient
        recipe.ingredients.clear();
        for (int i = 0; i < ingredientsBox.getChildCount(); i++) {
            View v = ingredientsBox.getChildAt(i);
            Object[] a = (Object[])v.getTag(R.id.addIngredient);
            if (a!=null) {
                String q=(
                        (EditText)a[0]).getText().toString().trim(), // quantity
                        u = ((Spinner)a[1]).getSelectedItem().toString(), // unit
                        n = ((EditText)a[2]).getText().toString().trim(); // text
                if (!n.isEmpty()) {
                    recipe.ingredients.add(q + "|" + u + "|" + n);
                }
            }
        }

        // All steps... step.
        recipe.steps.clear();
        for (int i = 0; i < stepsBox.getChildCount(); i++) {
            String s = ((EditText)stepsBox.getChildAt(i).getTag()).getText().toString().trim();
            // add this step if it isn't a blank string.
            if (!s.isEmpty()) {
                recipe.steps.add(s);
            }
        }

        // Add all images
        recipe.media.clear();
        recipe.media.addAll(mediaPaths);

        // save this recipe to the database and finish.
        db.save(recipe);
        finish();
    }
    private void importShared(Intent intent){
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            uri = intent.getData(); // attempt to get the data from the intent first before stopping
        }

        if (uri==null) return;

        try(InputStream in = getContentResolver().openInputStream(uri);
            ZipInputStream zip = new ZipInputStream(in)){
            File dir = new File(getFilesDir(),"imported");
            dir.mkdirs();

            ZipEntry e;
            JSONObject json = null;

            while ((e = zip.getNextEntry()) != null) {
                File out = new File(dir,new File(e.getName()).getName());
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] b = new byte[8192];
                    int n;
                    while ((n = zip.read(b))>0) {
                        fos.write(b, 0, n);
                    }
                }
                if (out.getName().equals("recipe.json")) {
                    String content = new String(java.nio.file.Files.readAllBytes(out.toPath()),java.nio.charset.StandardCharsets.UTF_8);
                    json = new JSONObject(content);
                }
            }
            if (json == null) {
                throw new Exception("recipe.json missing");
            }

            recipe.title = json.optString("title","Imported recipe");

            // Time
            recipe.prepMinutes = json.optInt("prepMinutes");
            recipe.cookMinutes = json.optInt("cookMinutes");
            recipe.additionalMinutes = json.optInt("additionalMinutes");

            // Ingredients
            JSONArray ing = json.optJSONArray("ingredients");
            if (ing != null) {
                for (int i = 0; i < ing.length(); i++) {
                    JSONObject x = ing.getJSONObject(i);
                    recipe.ingredients.add(x.optString("quantity") + "|" + x.optString("unit") + "|" + x.optString("name"));
                }
            }

            // Steps
            JSONArray st = json.optJSONArray("steps");
            if (st != null) {
                for (int i = 0; i < st.length(); i++) {
                    recipe.steps.add(st.optString(i));
                }
            }

            // Main Image
            String main = json.optString("mainImage", "");

            if (!main.isEmpty()) {
                File mainFile = new File(dir, new File(main).getName());

                if (mainFile.isFile()) {
                    mainPath = Uri.fromFile(mainFile).toString();
                }
                else {
                    mainPath = "";
                }
            }
            else {
                mainPath = "";
            }

            recipe.mainImage = mainPath;

            // Other Images
            JSONArray m = json.optJSONArray("media");
            if (m != null) {
                for (int i = 0; i < m.length(); i++) {
                    String f = m.optString(i);
                    if (!f.isEmpty()) {
                        mediaPaths.add(Uri.fromFile(new File(dir, new File(f).getName())).toString());
                    }
                }
            }

            Toast.makeText(this,"Imported recipe loaded. Review it and tap Confirm to save.",Toast.LENGTH_LONG).show();
        }
        catch(Exception ex) {
            Toast.makeText(this,"Could not import recipe: " + ex.getMessage(),Toast.LENGTH_LONG).show();
        }
    }
}
