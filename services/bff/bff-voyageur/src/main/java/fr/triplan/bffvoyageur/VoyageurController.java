package fr.triplan.bffvoyageur;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * BFF du portail voyageur : une API pensee pour l'ecran du voyageur.
 * Il agrege plusieurs microservices (le dashboard) et tolere qu'un service de calcul soit en panne.
 */
@RestController
@RequestMapping("/api/voyageur")
public class VoyageurController {

    private final Gateway gw;
    private final ServicesProperties urls;
    private final ObjectMapper mapper;

    public VoyageurController(Gateway gw, ServicesProperties urls, ObjectMapper mapper) {
        this.gw = gw;
        this.urls = urls;
        this.mapper = mapper;
    }

    // ---------- profil
    @GetMapping("/profils")
    public JsonNode profils() {
        return gw.get(urls.profil() + "/api/profils");
    }

    @GetMapping("/profil/{id}")
    public JsonNode profil(@PathVariable String id) {
        return gw.get(urls.profil() + "/api/profils/{id}", id);
    }

    @PostMapping("/profil")
    @ResponseStatus(HttpStatus.CREATED)
    public JsonNode creerProfil(@RequestBody JsonNode body) {
        return gw.post(urls.profil() + "/api/profils", body);
    }

    // ---------- voyages
    @GetMapping("/voyages")
    public JsonNode voyages(@RequestParam Long voyageurId) {
        return gw.getWithParams(urls.voyage() + "/api/voyages", Map.of("voyageurId", String.valueOf(voyageurId)));
    }

    @PostMapping("/voyages")
    @ResponseStatus(HttpStatus.CREATED)
    public JsonNode creerVoyage(@RequestBody JsonNode body) {
        return gw.post(urls.voyage() + "/api/voyages", body);
    }

    @PostMapping("/voyages/{id}/etapes")
    @ResponseStatus(HttpStatus.CREATED)
    public JsonNode ajouterEtape(@PathVariable Long id, @RequestBody JsonNode body) {
        return gw.post(urls.voyage() + "/api/voyages/{id}/etapes", body, id);
    }

    @DeleteMapping("/voyages/{id}/etapes/{etapeId}")
    public JsonNode retirerEtape(@PathVariable Long id, @PathVariable Long etapeId) {
        return gw.deleteJson(urls.voyage() + "/api/voyages/{id}/etapes/{etapeId}", id, etapeId);
    }

    // ---------- catalogue (lecture seule pour le voyageur)
    @GetMapping("/catalogue/{type}")
    public JsonNode catalogue(@PathVariable String type, @RequestParam Map<String, String> params) {
        return gw.getWithParams(urlCatalogue(type), params);
    }

    // ---------- dashboard : l'appel qui montre l'interet du BFF
    @GetMapping("/voyages/{id}/dashboard")
    public JsonNode dashboard(@PathVariable Long id) {
        ArrayNode avertissements = mapper.createArrayNode();

        // Indispensable : si le voyage n'existe pas, l'erreur remonte telle quelle.
        JsonNode voyage = gw.get(urls.voyage() + "/api/voyages/{id}", id);

        ArrayNode etapesDetaillees = mapper.createArrayNode();
        ArrayNode lignesBudget = mapper.createArrayNode();
        ArrayNode etapesItineraire = mapper.createArrayNode();

        for (JsonNode etape : voyage.path("etapes")) {
            String type = etape.path("type").asText();
            String refId = etape.path("refId").asText();
            int quantite = etape.path("quantite").asInt(1);

            JsonNode detail = chargerDetail(type, refId, avertissements);

            ObjectNode enrichie = ((ObjectNode) etape).deepCopy();
            enrichie.set("detail", detail);
            etapesDetaillees.add(enrichie);

            Resume r = detail == null ? null : resumer(type, detail);
            if (r == null) {
                continue;
            }

            ObjectNode ligne = mapper.createObjectNode();
            ligne.put("categorie", r.categorie());
            ligne.put("libelle", r.libelle());
            ligne.set("prixUnitaire", r.prix());
            ligne.put("quantite", quantite);
            lignesBudget.add(ligne);

            ObjectNode it = mapper.createObjectNode();
            it.put("jour", etape.path("jour").asText());
            it.put("type", type);
            it.put("libelle", r.libelle());
            it.put("ville", r.ville());
            etapesItineraire.add(it);
        }

        ObjectNode reqItineraire = mapper.createObjectNode();
        reqItineraire.set("etapes", etapesItineraire);
        ObjectNode reqBudget = mapper.createObjectNode();
        reqBudget.set("lignes", lignesBudget);

        JsonNode itineraire = optionnel(
                () -> gw.post(urls.itineraire() + "/api/itineraires/calculer", reqItineraire),
                "Service Itineraire indisponible", avertissements);
        JsonNode budget = optionnel(
                () -> gw.post(urls.budget() + "/api/budgets/calculer", reqBudget),
                "Service Budget indisponible", avertissements);

        String voyageurId = voyage.path("voyageurId").asText();
        JsonNode profil = optionnel(
                () -> gw.get(urls.profil() + "/api/profils/{id}", voyageurId),
                "Service Profil indisponible", avertissements);

        ObjectNode result = mapper.createObjectNode();
        result.set("voyage", voyage);
        result.set("etapes", etapesDetaillees);
        result.set("itineraire", itineraire);
        result.set("budget", budget);
        result.set("profil", profil);
        result.set("avertissements", avertissements);
        return result;
    }

    // ---------- utilitaires
    private record Resume(String categorie, String libelle, String ville, JsonNode prix) {
    }

    private Resume resumer(String type, JsonNode d) {
        return switch (type) {
            case "HEBERGEMENT" -> new Resume("Hebergement", d.path("nom").asText(),
                    d.path("ville").asText(), d.path("prixNuit"));
            case "ACTIVITE" -> new Resume("Activites", d.path("nom").asText(),
                    d.path("ville").asText(), d.path("prix"));
            case "TRANSPORT" -> new Resume("Transport",
                    d.path("type").asText() + " " + d.path("depart").asText() + " -> " + d.path("arrivee").asText(),
                    d.path("arrivee").asText(), d.path("prix"));
            default -> null;
        };
    }

    private JsonNode chargerDetail(String type, String refId, ArrayNode avertissements) {
        String url = switch (type) {
            case "HEBERGEMENT" -> urls.hebergement() + "/api/hebergements/{id}";
            case "ACTIVITE" -> urls.activites() + "/api/activites/{id}";
            case "TRANSPORT" -> urls.transport() + "/api/transports/{id}";
            default -> null;
        };
        if (url == null) {
            avertissements.add("Type d'etape inconnu : " + type);
            return null;
        }
        return optionnel(() -> gw.get(url, refId),
                type + " " + refId + " introuvable ou service indisponible", avertissements);
    }

    private JsonNode optionnel(Supplier<JsonNode> appel, String message, ArrayNode avertissements) {
        try {
            return appel.get();
        } catch (RestClientException ex) {
            avertissements.add(message);
            return null;
        }
    }

    private String urlCatalogue(String type) {
        return switch (type) {
            case "hebergements" -> urls.hebergement() + "/api/hebergements";
            case "activites" -> urls.activites() + "/api/activites";
            case "transports" -> urls.transport() + "/api/transports";
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalogue inconnu : " + type);
        };
    }
}
