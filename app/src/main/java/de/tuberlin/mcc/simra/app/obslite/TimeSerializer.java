package de.tuberlin.mcc.simra.app.obslite;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.datastore.core.CorruptionException;
import androidx.datastore.core.Serializer;
import de.tuberlin.mcc.simra.app.Time;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public class TimeSerializer implements Serializer<Time> {
    @Override
    public Time getDefaultValue() {
        return Time.getDefaultInstance();
    }

    @Override
    public Time readFrom(@NonNull InputStream inputStream, @NonNull Continuation<? super Time> continuation) {

        try {
            Log.d("TimeSerializer_LOG",Time.parseFrom(inputStream).getSeconds() + "s");
            return Time.parseFrom(inputStream);
        } catch (IOException exception) {
            try {
                throw new CorruptionException("Cannot read proto.", exception);
            } catch (CorruptionException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public Boolean writeTo(Time time, @NonNull OutputStream outputStream, @NonNull Continuation<? super Unit> continuation) {
        try {
            time.writeTo(outputStream);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
