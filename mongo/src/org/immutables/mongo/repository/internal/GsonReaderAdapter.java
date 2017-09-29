package org.immutables.mongo.repository.internal;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import org.bson.AbstractBsonReader;
import org.bson.AbstractBsonReader.State;
import org.bson.BsonReader;
import org.bson.BsonType;

class GsonReaderAdapter extends JsonReader {

    private static final Reader UNREADABLE_READER = new Reader() {
        @Override public int read(char[] buffer, int offset, int count) throws IOException {
            throw new AssertionError();
        }
        @Override public void close() throws IOException {
            throw new AssertionError();
        }
    };

    private final AbstractBsonReader delegate;

    GsonReaderAdapter(BsonReader bson) {
        super(UNREADABLE_READER);
        this.delegate = (AbstractBsonReader) bson;
    }

    private void advance() {
        delegate.readBsonType();
    }

    @Override
    public void beginArray() throws IOException {
        delegate.readStartArray();
    }

    @Override
    public void endArray() throws IOException {
        delegate.readEndArray();
    }

    @Override
    public void beginObject() throws IOException {
        delegate.readStartDocument();
    }

    @Override
    public void endObject() throws IOException {
        delegate.readEndDocument();
    }

    @Override
    public boolean hasNext() throws IOException {
        if (!hasMoreElements()) return false;
        advance();
        return hasMoreElements();
    }

    private boolean hasMoreElements() {
        return !(state() == State.END_OF_DOCUMENT || state() == State.END_OF_ARRAY || state() == State.DONE);
    }

    @Override
    public JsonToken peek() throws IOException {
        JsonToken token = null;

        if (state() == State.INITIAL || state() == State.DONE || state() == State.SCOPE_DOCUMENT) {
            advance();
            token = toGsonToken(delegate.getCurrentBsonType());
        } else if (state() == State.TYPE) {
            advance();
            token = toGsonToken(delegate.getCurrentBsonType());
        } else if (state() == State.NAME) {
            token = JsonToken.NAME;
        } else if (state() == State.END_OF_DOCUMENT) {
            token = JsonToken.END_OBJECT;
        } else if (state() == State.END_OF_ARRAY) {
            token = JsonToken.END_ARRAY;
        } else if (state() == State.DONE) {
            token = JsonToken.END_DOCUMENT;
        } else if (state() == State.VALUE) {
            token = toGsonToken(delegate.getCurrentBsonType());
        }

        if (token == null) {
            throw new IllegalStateException("Shouldn't get here. Last state is " + state() + " currentType:" +
                delegate.getCurrentBsonType() + "  token:" + token);
        }

        return token;
    }

    private State state() {
        return delegate.getState();
    }

    private static JsonToken toGsonToken(BsonType type) {
        final JsonToken token;
        switch (type) {
            case END_OF_DOCUMENT:
                token = JsonToken.END_DOCUMENT;
                break;
            case DOUBLE:
                token = JsonToken.NUMBER;
                break;
            case STRING:
                token = JsonToken.STRING;
                break;
            case DOCUMENT:
                token = JsonToken.BEGIN_OBJECT;
                break;
            case ARRAY:
                token = JsonToken.BEGIN_ARRAY;
                break;
            case OBJECT_ID:
                token = JsonToken.STRING;
                break;
            case BOOLEAN:
                token = JsonToken.BOOLEAN;
                break;
            case DATE_TIME:
                token = JsonToken.NUMBER;
                break;
            case NULL:
                token = JsonToken.NULL;
                break;
            case REGULAR_EXPRESSION:
                token = JsonToken.STRING;
                break;
            case SYMBOL:
                token = JsonToken.STRING;
                break;
            case INT32:
                token = JsonToken.NUMBER;
                break;
            case INT64:
                token = JsonToken.NUMBER;
                break;
            case TIMESTAMP:
                token = JsonToken.NUMBER;
                break;
            case DECIMAL128:
                token = JsonToken.NUMBER;
                break;
            default:
                // not really sure what to do with this type
                token = JsonToken.NULL;
        }

        return token;
    }

    @Override
    public String nextName() throws IOException {
        return delegate.readName();
    }

    @Override
    public String nextString() throws IOException {
        return scalarToString();
    }

    /**
     * Gson library reads numbers lazily (parsing strings on demand).
     * This is inefficient but (binary) number has to be converted to string then back to number.
     */
    private String scalarToString() throws IOException {
        final BsonType type = delegate.getCurrentBsonType();

        if (type == BsonType.STRING || type == BsonType.SYMBOL) {
            return delegate.readString();
        } else if (type == BsonType.INT32) {
            return Integer.toString(nextInt());
        } else if (type == BsonType.INT64) {
            return Long.toString(nextLong());
        } else if (type == BsonType.DOUBLE) {
            return Double.toString(nextDouble());
        }

        throw new IllegalStateException(String.format("Unknown scalar type to be converted to string: %s", type));
    }

    @Override
    public boolean nextBoolean() throws IOException {
        return delegate.readBoolean();
    }

    @Override
    public void nextNull() throws IOException {
        delegate.readNull();
    }

    @Override
    public double nextDouble() throws IOException {
        return delegate.readDouble();
    }

    @Override
    public long nextLong() throws IOException {
        return delegate.readInt64();
    }

    @Override
    public int nextInt() throws IOException {
        return delegate.readInt32();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void skipValue() throws IOException {
        delegate.skipValue();
    }
}
