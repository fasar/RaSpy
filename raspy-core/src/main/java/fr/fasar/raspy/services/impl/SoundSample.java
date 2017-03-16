package fr.fasar.raspy.services.impl;

import java.util.Arrays;

/**
 * Created by fabien on 16/03/2017.
 */
public class SoundSample {
    private final byte[] buffer;
    private final int offset;
    private final int size;


    public SoundSample(byte[] buffer, int offset, int size) {
        this.buffer = buffer;
        this.offset = offset;
        this.size = size;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "SoundSample{" +
                "buffer=" + Arrays.toString(buffer) +
                ", offset=" + offset +
                ", size=" + size +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SoundSample that = (SoundSample) o;

        if (offset != that.offset) return false;
        if (size != that.size) return false;
        return Arrays.equals(buffer, that.buffer);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(buffer);
        result = 31 * result + offset;
        result = 31 * result + size;
        return result;
    }
}
