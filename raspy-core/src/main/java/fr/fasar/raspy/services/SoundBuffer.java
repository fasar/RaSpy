package fr.fasar.raspy.services;

import com.google.common.collect.EvictingQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by Sartor on 13.03.2017.
 */
public class SoundBuffer implements Queue<Byte> {

    private final EvictingQueue<Byte> buffer;

    public SoundBuffer( int timeBufferSeconds, int frameRate, int frameSize) {
        int bufferSize = timeBufferSeconds * frameRate * frameSize;
        this.buffer = EvictingQueue.create(bufferSize);
    }

    public static <E> EvictingQueue<E> create(int maxSize) {
        return EvictingQueue.create(maxSize);
    }

    public int remainingCapacity() {
        return buffer.remainingCapacity();
    }

    public boolean offer(Byte aByte) {
        return buffer.offer(aByte);
    }

    public boolean add(Byte aByte) {
        return buffer.add(aByte);
    }

    public boolean addAll(Collection<? extends Byte> collection) {
        return buffer.addAll(collection);
    }

    public boolean contains(Object object) {
        return buffer.contains(object);
    }

    public boolean remove(Object object) {
        return buffer.remove(object);
    }

    public Byte poll() {
        return buffer.poll();
    }

    public Byte remove() {
        return buffer.remove();
    }

    public Byte peek() {
        return buffer.peek();
    }

    public Byte element() {
        return buffer.element();
    }

    public Iterator<Byte> iterator() {
        return buffer.iterator();
    }

    public int size() {
        return buffer.size();
    }

    public boolean removeAll(Collection<?> collection) {
        return buffer.removeAll(collection);
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public boolean containsAll(Collection<?> collection) {
        return buffer.containsAll(collection);
    }

    public boolean retainAll(Collection<?> collection) {
        return buffer.retainAll(collection);
    }

    public void clear() {
        buffer.clear();
    }

    public Object[] toArray() {
        return buffer.toArray();
    }

    public <T> T[] toArray(T[] array) {
        return buffer.toArray(array);
    }

    public boolean removeIf(Predicate<? super Byte> predicate) {
        return buffer.removeIf(predicate);
    }

    @Override
    public boolean equals(Object o) {
        return buffer.equals(o);
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }

    public Spliterator<Byte> spliterator() {
        return buffer.spliterator();
    }

    public Stream<Byte> stream() {
        return buffer.stream();
    }

    public Stream<Byte> parallelStream() {
        return buffer.parallelStream();
    }

    public void forEach(Consumer<? super Byte> consumer) {
        buffer.forEach(consumer);
    }
}
