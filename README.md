# Recipe Book (Java / XML, Android SDK 36)

A Recipe Book that stores recipes locally and allows for sharing outside of the app.

## Build
1. Open this folder in Android Studio (recent version with Android Gradle Plugin 8.10+).
2. Install Android SDK Platform 36 and let Gradle sync.
3. Run on Android 16.0+.

Package: `com.example.recipebook`.

## Included
- XML layouts for search grid, recipe detail, and add/edit.
- SQLite-based local storage through `SQLiteOpenHelper`.
- Case-insensitive substring title filtering (`LIKE %query%`).
- Prep/cook/additional time values stored as minutes; total calculated live.
- Main image and additional image URI selection.
- Add/remove ingredients and steps.
- Delete recipe confirmation.
- Recipe export as ZIP containing `recipe.json` plus copied images; ZIP import opens the add/edit form prefilled.
- Images are stored in persistent app memory that goes away when the app is deleted.

## Important implementation notes
- The shared ZIP includes `recipe.json` and image files where the app can read the selected image URI. Image selection uses Android's Photo Picker via `GetContent`. If a provider does not allow persistent URI access, the selected URI may become unavailable later; for production, copy selected images into app-private storage on selection.
- The shared file receiver is configured for `application/zip`. Some share targets wrap streams differently; the import handler supports `ACTION_SEND` and `ACTION_VIEW`.
- The current schema is new (`recipes.db`). If migrating an existing installed database, add a schema migration rather than changing the version silently.
