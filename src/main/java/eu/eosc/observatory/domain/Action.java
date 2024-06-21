package eu.eosc.observatory.domain;

import java.io.Serializable;

public class Action implements Serializable {

    Type type = Type.UPDATE;
    Integer index;

    public Action() {
    }

    public Action(Type type, Integer index) {
        this.type = type;
        this.index = index;
    }

    public Type getType() {
        return type;
    }

    public Action setType(Type type) {
        this.type = type;
        return this;
    }

    public Integer getIndex() {
        return index;
    }

    public Action setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public enum Type {
        UPDATE,
        ADD,
        MOVE,
        DELETE
    }

}
