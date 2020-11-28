package com.jumbodroid.notekeeper;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class NoteUploaderJobService extends Service {
    public NoteUploaderJobService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
