 package com.jumbodroid.notekeeper;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.jumbodroid.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.jumbodroid.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;

import java.util.List;

 public class NoteActivity extends AppCompatActivity {
     public static final String NOTE_ID = NoteActivity.class.getName();
     public static final int POSITION_NOT_SET = -1;
     private NoteInfo mNote;
     private boolean mIsNewNote;
     private Spinner mSpinnerCourses;
     private EditText mTextNoteTitle;
     private EditText mTextNoteText;
     private int mNoteId;
     private boolean mIsCancelling;
     private NoteActivityViewModel mViewModel;
     private NoteKeeperOpenHelper mDbOpenHelper;
     private Cursor mNoteCursor;
     private int mCourseIdPos;
     private int mNoteTextPos;
     private int mNoteTitlePos;
     private SimpleCursorAdapter mAdapterCourses;

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
         mCourseIdPos = mNoteCursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_TITLE);
         mNoteTitlePos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
         mNoteTextPos = mNoteCursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
         mNoteCursor.moveToNext();
         displayNote();
     }

     private void saveOriginalNoteValues() {
         if (mIsNewNote)
             return;
         mViewModel.mOriginalNoteCourseId = mNote.getCourse().getCourseId();
         mViewModel.mOriginalNoteTitle = mNote.getTitle();
         mViewModel.mOriginalNoteText = mNote.getText();
     }

     private void displayNote() {
         String courseTitle = mNoteCursor.getString(mCourseIdPos);
         String noteTitle = mNoteCursor.getString(mNoteTitlePos);
         String noteText = mNoteCursor.getString(mNoteTextPos);

//         int courseIndex = getIndexOfCourseId(courseId);
         int courseIndex = getIndexOfCourseTitle(courseTitle);
         mSpinnerCourses.setSelection(courseIndex);
         mTextNoteTitle.setText(noteTitle);
         mTextNoteText.setText(noteText);
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

     private int getIndexOfCourseId(String courseId) {
         Cursor cursor = mAdapterCourses.getCursor();
         int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
         int courseRowIndex = 0;

         boolean more = cursor.moveToFirst();
         while (more) {
             String cursorCourseId = cursor.getString(courseIdPos);
             if (courseId.equals(cursorCourseId))
                 break;

             courseRowIndex++;
             more = cursor.moveToNext();
         }
         return courseRowIndex;
     }

     private void readDisplayStateValues() {
         Intent intent = getIntent();
         mNoteId = intent.getIntExtra(NOTE_ID, POSITION_NOT_SET);
         mIsNewNote = mNoteId == POSITION_NOT_SET;

         if (mIsNewNote)
            createNewNote();

         mNote = DataManager.getInstance().getNotes().get(mNoteId);
     }

     private void createNewNote() {
         DataManager dm = DataManager.getInstance();
         mNoteId = dm.createNewNote();
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

        return super.onOptionsItemSelected(item);
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
         mNote = DataManager.getInstance().getNotes().get(mNoteId);

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
                 DataManager.getInstance().removeNote(mNoteId);
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

     private void storePreviousNoteValues() {
         CourseInfo course = DataManager.getInstance().getCourse(mViewModel.mOriginalNoteCourseId);
         mNote.setCourse(course);
         mNote.setTitle(mViewModel.mOriginalNoteTitle);
         mNote.setText(mViewModel.mOriginalNoteText);
     }

     private void saveNote() {
         mNote.setTitle(mTextNoteTitle.getText().toString());
         mNote.setText(mTextNoteText.getText().toString());
         mNote.setCourse( (CourseInfo) mSpinnerCourses.getSelectedItem());
     }
 }