package eu.eosc.observatory.datasets.ingest.custom;

import com.opencsv.bean.CsvBindByName;

public class CustomCsv {

    @CsvBindByName
    String year;

    @CsvBindByName
    String country;

    @CsvBindByName
    String researchers;

    @CsvBindByName
    String publications;

    @CsvBindByName
    String datasets;

    @CsvBindByName
    String software;

    @CsvBindByName
    String services;

    @CsvBindByName
    String signatories;

    public CustomCsv() {
        // no-arg constructor
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getResearchers() {
        return researchers;
    }

    public void setResearchers(String researchers) {
        this.researchers = researchers;
    }

    public String getPublications() {
        return publications;
    }

    public void setPublications(String publications) {
        this.publications = publications;
    }

    public String getDatasets() {
        return datasets;
    }

    public void setDatasets(String datasets) {
        this.datasets = datasets;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getSignatories() {
        return signatories;
    }

    public void setSignatories(String signatories) {
        this.signatories = signatories;
    }
}
