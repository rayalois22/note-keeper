 package com.jumbodroid.notekeeper;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.jumbodroid.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.jumbodroid.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;
import com.jumbodroid.notekeeper.NoteKeeperProviderContract.Notes;

 public class NoteActivity extends AppCompatActivity {
     public static final String TAG = NoteActivity.class.getName();
     public static final String NOTE_ID = NoteActivity.class.getName();
     public static final int ID_NOT_SET = -1;
     private boolean mIsNewNote;
     private Spinner mSpinnerCourses;
     private EditText mTextNoteTitle;
     private EditText mTextNoteText;
     private int mNoteId;
     private boolean mIsCancelling;
     private NoteActivityViewModel mViewModel;
     private NoteKeeperOpenHelper mDbOpenHelper;
     private Cursor mNoteCursor;
     private int mCourseTitlePos;
     private int mNoteTextPos;
     private int mNoteTitlePos;
     private SimpleCursorAdapter mAdapterCourses;
     private Uri mNoteUri;

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewModelProvider viewModelProvider = new ViewModelProvider(getViewModelStore(),
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()));

        mViewModel = viewModelProvider.get(NoteActivityViewModel.class);

        if (mViewModel.isNewlyCreated && savedInstanceState != null)
            mViewModel.restoreState(savedInstanceState);

        mViewModel.isNewlyCreated = false;

        mSpinnerCourses = findViewById(R.id.spinner_courses);

        mDbOpenHelper = new NoteKeeperOpenHelper(this);

        mAdapterCourses = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, null,
                 new String[]{CourseInfoEntry.COLUMN_COURSE_TITLE},
         new int[]{android.R.id.text1}, 0);
        mAdapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(mAdapterCourses);

        loadCourseData();
        
        readDisplayStateValues();
        saveOriginalNoteValues();

         mTextNoteTitle = findViewById(R.id.text_note_title);
         mTextNoteText = findViewById(R.id.text_note_text);

         if (!mIsNewNote)
            loadNoteData();
    }

     private void loadCourseData() {
         SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
         String[] courseColumns = {
                 CourseInfoEntry.COLUMN_COURSE_TITLE,
                 CourseInfoEntry.COLUMN_COURSE_ID,
                 CourseInfoEntry._ID
         };

         Cursor cursor = db.query(CourseInfoEntry.TABLE_NAME, courseColumns,
                 null, null, null, null, CourseInfoEntry.COLUMN_COURSE_TITLE);
         mAdapterCourses.changeCursor(cursor);

//         final Uri uri = Uri.parse("content://com.jumbodroid.notekeeper.provider");
     }

     @Override
     protected void onDestroy() {
         mDbOpenHelper.close();
         super.onDestroy();
     }

     private void loadNoteData() {
         SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();

         String selection = NoteInfoEntry.getQName(NoteInfoEntry._ID) + " = ?";

         String[] selectionArgs = {Integer.toString(mNoteId)};

         String[] noteColumns = {
                 NoteInfoEntry.getQName(NoteInfoEntry._ID),
                 NoteInfoEntry.COLUMN_NOTE_TITLE,
                 NoteInfoEntry.COLUMN_NOTE_TEXT,
                 CourseInfoEntry.COLUMN_COURSE_TITLE
         };

         String tablesWithJoin = NoteInfoEntry.TABLE_NAME + " JOIN " +
                 CourseInfoEntry.TABLE_NAME + " ON " +
                 NoteInfoEntry.getQName(NoteInfoEntry.COLUMN_COURSE_ID) + " = " +
                 CourseInfoEntry.getQName(CourseInfoEntry.COLUMN_COURSE_ID);

         mNoteCursor = db.query(tablesWithJoin, noteColumns,
                 selection, selectionArgs, null, null, null);
         mCourseTitlePos = mNoteCursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_TITLE);
         mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
         mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
         mNoteCursor.moveToNext();
         displayNote();
     }

     private void loadNoteDataUsingContentProvider() {
         String[] noteColumns = {
                 Notes.COLUMN_COURSE_ID,
                 Notes.COLUMN_NOTE_TITLE,
                 Notes.COLUMN_NOTE_TEXT,
                 Notes.COLUMN_COURSE_TITLE
         };

         mNoteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
     }

     private void saveOriginalNoteValues() {
         if (mIsNewNote)
             return;

         SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
         String selection = NoteInfoEntry.getQName(NoteInfoEntry._ID) + " = ?";

         String[] selectionArgs = {Integer.toString(mNoteId)};

         String[] noteColumns = {
                 NoteInfoEntry.getQName(NoteInfoEntry._ID),
                 NoteInfoEntry.COLUMN_NOTE_TITLE,
                 NoteInfoEntry.COLUMN_NOTE_TEXT,
                 CourseInfoEntry.COLUMN_COURSE_TITLE
         };

         String tablesWithJoin = NoteInfoEntry.TABLE_NAME + " JOIN " +
                 CourseInfoEntry.TABLE_NAME + " ON " +
                 NoteInfoEntry.getQName(NoteInfoEntry.COLUMN_COURSE_ID) + " = " +
                 CourseInfoEntry.getQName(CourseInfoEntry.COLUMN_COURSE_ID);

         Cursor cursor = db.query(tablesWithJoin, noteColumns,
                 selection, selectionArgs, null, null, null);
         Integer courseTitlePos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_TITLE);
         Integer noteTitlePos = cursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
         Integer noteTextPos = cursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
         cursor.moveToNext();

         mViewModel.mOriginalNoteCourseTitle = cursor.getString(courseTitlePos);
         mViewModel.mOriginalNoteTitle = cursor.getString(noteTitlePos);
         mViewModel.mOriginalNoteText = cursor.getString(noteTextPos);
     }

     private void displayNote() {
         String courseTitle = mNoteCursor.getString(mCourseTitlePos);
         String noteTitle = mNoteCursor.getString(mNoteTitlePos);
         String noteText = mNoteCursor.getString(mNoteTextPos);

         int courseIndex = getIndexOfCourseTitle(courseTitle);
         mSpinnerCourses.setSelection(courseIndex);
         mTextNoteTitle.setText(noteTitle);
         mTextNoteText.setText(noteText);

         CourseEventBroadcastHelper.sendEventBroadcast(this, selectedCourseId(), "Editing note");
     }

     private int getIndexOfCourseTitle(String courseTitle) {
         Cursor cursor = mAdapterCourses.getCursor();
         int courseTitlePos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_TITLE);
         int courseRowIndex = 0;

         boolean more = cursor.moveToFirst();
         while (more) {
             String cursorCourseTitle = cursor.getString(courseTitlePos);
             if (courseTitle.equals(cursorCourseTitle))
                 break;

             courseRowIndex++;
             more = cursor.moveToNext();
         }
         return courseRowIndex;
     }

     private void readDisplayStateValues() {
         Intent intent = getIntent();
         mNoteId = intent.getIntExtra(NOTE_ID, ID_NOT_SET);
         mIsNewNote = mNoteId == ID_NOT_SET;

         if (mIsNewNote)
            createNewNote();
     }

     private void createNewNote() {
         AsyncTask<ContentValues, Integer, Uri> task = new AsyncTask<ContentValues, Integer, Uri>() {
             private ProgressBar mProgressBar;

             @Override
             protected void onPreExecute() {
                 mProgressBar = findViewById(R.id.progress_bar);
                 mProgressBar.setVisibility(View.VISIBLE);
                 mProgressBar.setProgress(1);
             }

             @Override
             protected Uri doInBackground(ContentValues... contentValues) {
                 publishProgress(2);
                 Log.d(TAG, "doInBackground - thread: " + Thread.currentThread().getId());
                 ContentValues insertValues = contentValues[0];
                 Uri rowUri = getContentResolver().insert(Notes.CONTENT_URI, insertValues);
                 publishProgress(3);

                 return rowUri;
             }

             @Override
             protected void onProgressUpdate(Integer... values) {
                 int progressValue = values[0];
                 mProgressBar.setProgress(progressValue);
             }

             @Override
             protected void onPostExecute(Uri uri) {
                 Log.d(TAG, "onPostExecute - thread: " + Thread.currentThread().getId());
                 mNoteUri = uri;
                 mProgressBar.setVisibility(View.GONE);
                 Log.d(TAG, mNoteUri.toString());
             }
         };

         ContentValues values = new ContentValues();
         values.put(Notes.COLUMN_COURSE_ID, "");
         values.put(Notes.COLUMN_NOTE_TITLE, "");
         values.put(Notes.COLUMN_NOTE_TEXT, "");

         Log.d(TAG, "Call to execute - thread: " + Thread.currentThread().getId());
         task.execute(values);
     }

     @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        }
        else if (id == R.id.action_cancel) {
            mIsCancelling = true;
            finish();
        }
        else if (id == R.id.action_next) {
            moveNext();
        }
        else if (id == R.id.action_reminder) {
            showReminderNotification();
        }

        return super.onOptionsItemSelected(item);
    }

     private void showReminderNotification() {

     }

     @Override
     public boolean onPrepareOptionsMenu(Menu menu) {
         MenuItem item = menu.findItem(R.id.action_next);
         int lastNoteIndex = DataManager.getInstance().getNotes().size() - 1;
         item.setEnabled(mNoteId < lastNoteIndex);
         return super.onPrepareOptionsMenu(menu);
     }

     private void moveNext() {
         saveNote();

         ++mNoteId;

         saveOriginalNoteValues();
         displayNote();
         invalidateOptionsMenu();
     }

     private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String text = "Checkout what I learnt at the Pluralsight course \"" +
                course.getTitle() + "\"\n" + mTextNoteText.getText().toString();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc2822");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(intent);
     }

     @Override
     protected void onPause() {
         super.onPause();
         if (mIsCancelling){
             if (mIsNewNote) {
                 deleteNoteFromDatabase();
             } else {
                 storePreviousNoteValues();
             }
         } else {
             saveNote();
         }
     }

     @Override
     protected void onSaveInstanceState(@NonNull Bundle outState) {
         super.onSaveInstanceState(outState);

         if (outState != null)
             mViewModel.saveState(outState);
     }

     private void deleteNoteFromDatabase() {
         AsyncTask task = new AsyncTask() {
             @Override
             protected Object doInBackground(Object[] params) {
                 getContentResolver().delete(mNoteUri, null, null);
                 return null;
             }
         };
         task.execute();
     }

     private void storePreviousNoteValues() {
         mSpinnerCourses.setSelection(getIndexOfCourseTitle(mViewModel.mOriginalNoteCourseTitle));
         mTextNoteTitle.setText(mViewModel.mOriginalNoteTitle);
         mTextNoteText.setText(mViewModel.mOriginalNoteText);
     }

     private void saveNote() {
         String courseId = selectedCourseId();
         String noteTitle = mTextNoteTitle.getText().toString();
         String noteText = mTextNoteText.getText().toString();
         saveNoteToDatabase(courseId, noteTitle, noteText);
     }

     private String selectedCourseId() {
         int selectedPosition = mSpinnerCourses.getSelectedItemPosition();
         Cursor cursor = mAdapterCourses.getCursor();
         cursor.moveToPosition(selectedPosition);
         int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
         String courseId = cursor.getString(courseIdPos);
         return courseId;
     }

     private void saveNoteToDatabase(String courseId, String noteTitle, String noteText)
     {
        String selection = NoteInfoEntry._ID + " = ?";
        String[] selectionArgs = { Integer.toString(mNoteId) };

         ContentValues values = new ContentValues();
         values.put(NoteInfoEntry.COLUMN_COURSE_ID, courseId);
         values.put(NoteInfoEntry.COLUMN_NOTE_TITLE, noteTitle);
         values.put(NoteInfoEntry.COLUMN_NOTE_TEXT, noteText);

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        db.update(NoteInfoEntry.TABLE_NAME, values, selection, selectionArgs);
     }
 }