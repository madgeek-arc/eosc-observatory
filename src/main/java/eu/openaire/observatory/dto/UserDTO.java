package eu.openaire.observatory.dto;

import eu.openaire.observatory.domain.Profile;
import eu.openaire.observatory.domain.User;

public record UserDTO
        (
                String name,
                String surname,
                byte[] picture,
                String position,
                String affiliation,
                String webpage) {

    public UserDTO(String name, String surname, Profile profile) {
        this(
                name,
                surname,
                profile.getPicture(),
                profile.getPosition(),
                profile.getAffiliation(),
                profile.getWebpage());
    }

    public UserDTO(User user) {
        this(
                user.getName(),
                user.getSurname(),
                user.getProfile() != null ? user.getProfile() : new Profile());
    }
}
