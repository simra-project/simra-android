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
import de.tuberlin.mcc.simra.app.UserInput;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public class UserInputSerializer implements Serializer<UserInput> {
    @Override
    public UserInput getDefaultValue() {
        return null;
    }

    @Nullable
    @Override
    public UserInput readFrom(@NonNull InputStream inputStream, @NonNull Continuation<? super UserInput> continuation) {
        try {
            return UserInput.parseFrom(inputStream);
        } catch (IOException exception) {
            try {
                throw new CorruptionException("Cannot read proto.", exception);
            } catch (CorruptionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nullable
    @Override
    public Boolean writeTo(UserInput userInput, @NonNull OutputStream outputStream, @NonNull Continuation<? super Unit> continuation) {
        try {
            userInput.writeTo(outputStream);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
