package eu.eosc.observatory.service;

public interface Identifiable<T> {
    T getId();

    void setId(T id);
}
