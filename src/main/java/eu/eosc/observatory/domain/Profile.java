package eu.eosc.observatory.domain;

import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Array;

public class Profile {

    byte[] picture;
    String position;
    String affiliation;

    public Profile() {
        // no-arg constructor
    }

    public Profile(byte[] picture) {
        this.picture = picture;
    }

    public static byte[] imageFromUrl(String url) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, byte[].class);
    }

    public byte[] getPicture() {
        return picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
}
