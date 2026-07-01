# Mapping File MD Base44 ke Android Native

| File MD / Base44 | Android Native Rebuild |
|---|---|
| `UserProfile` entity | `UserProfileModel.java` + `DataStore.java` |
| `Task` entity | `TaskModel.java` + `TaskAdapter.java` |
| `Note` entity | `NoteModel.java` + `NoteAdapter.java` |
| `Media` entity | `MediaModel.java` + `MediaAdapter.java` |
| `FocusSession` entity | `FocusSessionModel.java` + `FocusHistoryAdapter.java` |
| `Home.jsx` | `MainActivity.showDashboard()` |
| `Tasks.jsx` | `MainActivity.showTasks()` |
| `TaskEditor.jsx` | `MainActivity.showTaskForm()` |
| `TaskDetail.jsx` | `MainActivity.showTaskDetail()` |
| `Notes.jsx` | `MainActivity.showNotes()` |
| `NoteEditor.jsx` | `MainActivity.showNoteForm()` |
| `NoteDetail.jsx` | `MainActivity.showNoteDetail()` |
| `MediaGallery.jsx` | `MainActivity.showMedia()` dan `showMediaForm()` |
| `Focus.jsx` | `MainActivity.showFocus()` |
| `Profile.jsx` | `MainActivity.showProfile()` |
| `BottomNav.jsx` | `bottom_nav_menu.xml` + `BottomNavigationView` |
| Base44 database | `SharedPreferences` + Gson local JSON |
| Base44 file upload | Android `ACTION_OPEN_DOCUMENT` local media picker |
| Browser SpeechRecognition | Android `RecognizerIntent` |
| Browser SpeechSynthesis | Android `TextToSpeech` |
| Motion detection via `devicemotion` | Android `SensorManager` accelerometer |
