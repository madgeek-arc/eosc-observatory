package eu.eosc.observatory.domain;

import eu.eosc.observatory.dto.ProfileDTO;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

public class Profile {

    byte[] picture;
    String position;
    String affiliation;
    String webpage;

    public Profile() {
        // no-arg constructor
    }

    public Profile(byte[] picture) {
        this.picture = picture;
    }

    public static Profile of(ProfileDTO profileDTO) {
        Profile profile = new Profile();
        if (profileDTO.getPicture() != null) {
            profile.setPicture(Base64.getDecoder().decode(profileDTO.getPicture().getBytes()));
        } else {
            profile.setPicture(new byte[0]);
        }
        profile.setAffiliation(profileDTO.getAffiliation());
        profile.setPosition(profileDTO.getPosition());
        profile.setWebpage(profileDTO.getWebpage());
        return profile;
    }

    public static byte[] imageFromUrl(String url) {
        if (url != null) {
            RestTemplate restTemplate = new RestTemplate();
            URI uri = null;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                return null;
            }
            return restTemplate.getForObject(uri.toString(), byte[].class);
        }
        return null;
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

    public String getWebpage() {
        return webpage;
    }

    public void setWebpage(String webpage) {
        this.webpage = webpage;
    }
}
