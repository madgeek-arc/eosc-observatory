package eu.eosc.observatory;

import java.util.*;
import java.util.stream.Collectors;

public class CsvBuilder {

    private LinkedHashMap<String, Column> columns = new LinkedHashMap<>();
    private transient Character delimiter = '\t';
    private transient String listDelimiter = ";";
    private static final String DOUBLE_QUOTE_ENCODED = "%22";
    private int columnSize = 0;


    public CsvBuilder() {
        // no-arg constructor
    }

    public void setDelimiter(Character delimiter) {
        this.delimiter = delimiter;
    }

    public void setListDelimiter(String listDelimiter) {
        this.listDelimiter = listDelimiter;
    }

    public void addColumn(String label) {
        columns.putIfAbsent(label, new Column(label));
    }

    public void addColumn(String label, int dataSize) {
        if (dataSize < columnSize) {
            dataSize = columnSize;
        }
        columns.putIfAbsent(label, new Column(label, dataSize));
    }

    public void addColumn(String label, List<String> data) {
        if (!columns.isEmpty()) {
            if (data.size() != columnSize) {
                throw new RuntimeException("Cannot add column of different size.");
            } else if (columns.containsKey(label)) {
                throw new RuntimeException("Column with the specified label already exists.");
            }
        } else {
            columnSize = data.size();
        }
        columns.put(label, new Column(label, data));
    }

    public void appendColumnToStart(String label, List<String> data) {
        LinkedHashMap<String, Column> temp = (LinkedHashMap<String, Column>) columns.clone();
        columns.clear();
        columns.put(label, new Column(label, data));
        columns.putAll(temp);
    }

    public void addValue(String label, int index, List<String> data) {
        if (index >= columnSize) {
            for (Column column : columns.values()) {
                for (int i = columnSize - 1; i <= index; i++) {
                    column.add(i, null);
                }
            }
            columnSize = index + 1;
        }
        columns.putIfAbsent(label, new Column(label, columnSize));
        this.columns.get(label).add(index, data);
    }

    public Map<String, List<String>> getData() {
        Map<String, List<String>> data = new LinkedHashMap<>();
        for (Map.Entry<String, Column> entry : columns.entrySet()) {
            data.put(entry.getKey(), new ArrayList<>());
            List<String> values = data.get(entry.getKey());
            for (List<String> item : entry.getValue().getContents()) {
                values.add(formatText(createCellData(item)));
            }
            for (int i = 0; i < entry.getValue().getSiblings().size(); i++) {
                String siblingLabel = String.format("%s (%s)", entry.getKey(), i + 1);
                data.putIfAbsent(siblingLabel, new ArrayList<>());
                for (List<String> siblingItem : entry.getValue().getSiblings().get(i).getContents()) {
                    data.get(siblingLabel).add(formatText(createCellData(siblingItem)));
                }
            }
        }
        return data;
    }

    public String toCsv() {
        StringBuilder csv = new StringBuilder();
        Map<String, List<String>> data = getData();

        for (String label : data.keySet()) {
            csv.append(label);
            csv.append(delimiter);
        }
        csv.append("\n");

        for (int i = 0; i < columnSize; i++) {
            for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                csv.append(entry.getValue().get(i));
                csv.append(delimiter);
            }
            csv.append("\n");
        }

        return csv.toString();
    }

    String formatText(String text) {
        text = text.replace("\t", "    ");
        // enclose text inside double quotes if it contains break lines
        if (text.contains("\n")) {
            // replace existing double quotes to avoid display problems with Excel, Numbers (mac), etc.
            text = text.replace("\"", DOUBLE_QUOTE_ENCODED);
            text = String.format("\"%s\"", text);
        }
        return text;
    }

    private String createCellData(List<String> data) {
        if (data != null) {
            try {
                data = data.stream().map(item -> {
                    if (item == null) return "";
                    else return item;
                }).collect(Collectors.toList());
            } catch (RuntimeException e) {
                throw e;
            }
        }
        return data != null ? String.join(listDelimiter, data) : "";
    }

    class Column {

        private String label;
        private List<List<String>> contents = new LinkedList<>();
        private List<Column> siblings = new LinkedList<>();

        public Column(String label) {
            this.label = label;
        }

        public Column(String label, int contentSize) {
            this.label = label;
            this.contents = new LinkedList<>();
            for (int i = 0; i < contentSize; i++) {
                contents.add(i, null);
            }
        }

        public Column(String label, List<String> data) {
            this.label = label;
            this.contents = new LinkedList<>();
            for (String item : data) {
                contents.add(List.of(item));
            }
        }

        public void add(int i, List<String> data) {

            if (i >= contents.size()) {
                try {
                for (int j = contents.size(); j <= i; j++) {
                    contents.add(null);
                    for (Column sibling : siblings) {
                        sibling.getContents().add(i, null);
                    }
                }
                contents.set(i, data);

            } catch (IndexOutOfBoundsException e) {
                    System.out.println("error");
                    throw e;
                }
            } else if (contents.get(i) == null) {
                contents.set(i, data);
            } else {
                boolean pending = true;
                for (Column sibling : siblings) {
                    if (sibling.getContents().get(i) == null) {
                        sibling.getContents().set(i, data);
                        pending = false;
                        break;
                    }
                }
                if (pending) {
                    Column sibling = new Column(label, columnSize);
                    siblings.add(sibling);
                    sibling.getContents().set(i, data);
                }
            }


        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<List<String>> getContents() {
            return contents;
        }

        public void setContents(List<List<String>> contents) {
            this.contents = contents;
        }

        public List<Column> getSiblings() {
            return siblings;
        }

        public void setSiblings(List<Column> siblings) {
            this.siblings = siblings;
        }
    }
}
