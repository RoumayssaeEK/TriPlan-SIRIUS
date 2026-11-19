package fr.triplan.bffvoyageur;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record ServicesProperties(String profil, String voyage, String hebergement, String activites,
                                 String transport, String itineraire, String budget) {
}
