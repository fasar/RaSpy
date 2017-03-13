package fr.fasar.raspy.services;

/**
 * Created by Sartor on 13.03.2017.
 */
public class ServiceException extends Exception{
    public ServiceException() {
    }

    public ServiceException(String s) {
        super(s);
    }

    public ServiceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ServiceException(Throwable throwable) {
        super(throwable);
    }

    public ServiceException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
