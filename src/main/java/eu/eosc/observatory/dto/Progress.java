package eu.eosc.observatory.dto;

public class Progress {

    int current;
    int total;

    public Progress() {
    }

    public Progress(int current, int total) {
        this.current = current;
        this.total = total;
    }

    public void addToCurrent(int value) {
        this.current += value;
    }

    public void addToTotal(int value) {
        this.total += value;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
