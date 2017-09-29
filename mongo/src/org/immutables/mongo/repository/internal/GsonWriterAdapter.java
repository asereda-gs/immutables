package org.immutables.mongo.repository.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import org.bson.BsonWriter;
import org.bson.json.JsonWriter;

class GsonWriterAdapter extends com.google.gson.stream.JsonWriter {
    private static final Writer UNWRITABLE_WRITER = new Writer() {
        @Override public void write(char[] buffer, int offset, int counter) {
            throw new AssertionError();
        }
        @Override public void flush() throws IOException {
            throw new AssertionError();
        }
        @Override public void close() throws IOException {
            throw new AssertionError();
        }
    };

    private final BsonWriter delegate;

    GsonWriterAdapter(JsonWriter delegate) {
        super(UNWRITABLE_WRITER);
        this.delegate = delegate;
    }

    @Override
    public com.google.gson.stream.JsonWriter beginArray() throws IOException {
        delegate.writeStartArray();
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter endArray() throws IOException {
        delegate.writeEndArray();
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter beginObject() throws IOException {
        delegate.writeStartDocument();
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter endObject() throws IOException {
        delegate.writeEndDocument();
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter name(String name) throws IOException {
        delegate.writeName(name);
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter value(String value) throws IOException {
        delegate.writeString(value);
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter jsonValue(String value) throws IOException {
        throw new UnsupportedOperationException("Can't write directly JSON");
    }

    @Override
    public com.google.gson.stream.JsonWriter nullValue() throws IOException {
        delegate.writeNull();
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter value(boolean value) throws IOException {
        delegate.writeBoolean(value);
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter value(Boolean value) throws IOException {
        if (value == null) {
            delegate.writeNull();
        } else {
            delegate.writeBoolean(value);
        }

        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter value(double value) throws IOException {
        delegate.writeDouble(value);
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter value(long value) throws IOException {
        delegate.writeInt64(value);
        return this;
    }

    @Override
    public com.google.gson.stream.JsonWriter value(Number value) throws IOException {
        if (value == null) {
            return nullValue();
        } else if (value instanceof Double) {
            return value(value.doubleValue());
        } else if (value instanceof Long){
            return value(value.longValue());
        } else if (value instanceof Integer) {
            return value(value.intValue());
        } else if (value instanceof Short) {
            return value((int) value.shortValue());
        } else {
            throw new UnsupportedOperationException(String.format("Don't know how to write %s: %s", value.getClass().getName(), value));
        }
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        if (delegate instanceof Closeable) {
            ((Closeable) delegate).close();
        }
    }
}
