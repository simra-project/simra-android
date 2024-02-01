package de.tuberlin.mcc.simra.app.obslite;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.datastore.core.CorruptionException;
import androidx.datastore.core.Serializer;
import de.tuberlin.mcc.simra.app.Event;
import de.tuberlin.mcc.simra.app.Time;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public class EventSerializer implements Serializer<Event> {
    @Override
    public Event getDefaultValue() {
        return Event.getDefaultInstance();
    }

    @Override
    public Event readFrom(@NonNull InputStream inputStream, @NonNull Continuation<? super Event> continuation) {
        try {
            Log.d("EventSerializer_LOG", Event.parseFrom(inputStream).toString());
            return Event.parseFrom(inputStream);
        } catch (IOException exception) {
            try {
                throw new CorruptionException("Cannot read proto.", exception);
            } catch (CorruptionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Boolean writeTo(Event event, @NonNull OutputStream outputStream, @NonNull Continuation<? super Unit> continuation) {
        try {
            event.writeTo(outputStream);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
