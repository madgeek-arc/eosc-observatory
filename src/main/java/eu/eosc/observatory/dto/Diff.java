package eu.eosc.observatory.dto;

import java.util.LinkedList;
import java.util.List;

public class Diff {

    List<Node> differences = new LinkedList<>();

    public Diff() {
    }

    public Diff(List<Node> differences) {
        this.differences = differences;
    }

    public List<Node> getDifferences() {
        return differences;
    }

    public void setDifferences(List<Node> differences) {
        this.differences = differences;
    }
}
