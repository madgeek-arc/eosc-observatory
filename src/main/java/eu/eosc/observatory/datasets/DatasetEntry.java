package eu.eosc.observatory.datasets;

import jakarta.persistence.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity(name = "dataset_entry")
public class DatasetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Year is mandatory.")
    @Size(min = 4, max = 4)
    private String year;

    @NotNull
    @Size(min = 2, max = 3, message = "You must provide the two-letter country code.")
    private String country;

    @NotBlank(message = "Dataset name cannot be blank.")
    private String name;

    @NotBlank(message = "Dataset authority cannot be blank.")
    private String authority;

    private String value;

    private String comment;

    public DatasetEntry() {
    }

    public DatasetEntry(String year, String country, String name, String authority, String value, String comment) {
        this.year = year;
        this.country = country;
        this.name = name;
        this.authority = authority;
        this.value = value;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
