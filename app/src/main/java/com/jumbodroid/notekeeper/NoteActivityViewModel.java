package com.jumbodroid.notekeeper;

import android.os.Bundle;

import androidx.lifecycle.ViewModel;

public class NoteActivityViewModel extends ViewModel {
    public static final String ORIGINAL_NOTE_COURSE_TITLE = NoteActivityViewModel.class.getName() +
            ".ORIGINAL_NOTE_COURSE_TITLE";
    public static final String ORIGINAL_NOTE_TITLE = NoteActivityViewModel.class.getName() +
            ".ORIGINAL_NOTE_TITLE";
    public static final String ORIGINAL_NOTE_TEXT = NoteActivityViewModel.class.getName() +
            ".ORIGINAL_NOTE_TEXT";

    public String mOriginalNoteCourseTitle;
    public String mOriginalNoteTitle;
    public String mOriginalNoteText;
    public boolean isNewlyCreated = true;

    public void saveState(Bundle outState) {
        outState.putString(ORIGINAL_NOTE_COURSE_TITLE, mOriginalNoteCourseTitle);
        outState.putString(ORIGINAL_NOTE_TITLE, mOriginalNoteTitle);
        outState.putString(ORIGINAL_NOTE_TEXT, mOriginalNoteText);
    }

    public void restoreState(Bundle inState) {
        mOriginalNoteCourseTitle = inState.getString(ORIGINAL_NOTE_COURSE_TITLE);
        mOriginalNoteTitle = inState.getString(ORIGINAL_NOTE_TITLE);
        mOriginalNoteText = inState.getString(ORIGINAL_NOTE_TEXT);
    }
}
