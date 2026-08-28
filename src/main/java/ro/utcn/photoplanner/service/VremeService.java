package ro.utcn.photoplanner.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prognoza meteo de la Open-Meteo, care nu cere cheie de acces.
 * <p>
 * Ne interesează în principal gradul de acoperire cu nori la orele de aur: o oră de aur sub
 * un cer complet acoperit nu se întâmplă. Dacă serviciul nu răspunde, pagina trebuie să se
 * încarce oricum — de aceea toate erorile se transformă în „prognoză indisponibilă”.
 */
@Service
public class VremeService {

    private static final Logger log = LoggerFactory.getLogger(VremeService.class);

    /** Cât în față oferă Open-Meteo prognoză. */
    private static final int ZILE_PROGNOZA = 16;

    /** Cât timp ținem un răspuns înainte să-l cerem din nou. */
    private static final Duration DURATA_CACHE = Duration.ofMinutes(30);

    /**
     * Cât timp ținem minte că serviciul nu a răspuns.
     * <p>
     * Fără asta, cât timp Open-Meteo e picat, fiecare afișare a unei locații ar aștepta din nou
     * expirarea conexiunii — adică pagina ar deveni lentă pentru toată lumea. Reținem eșecul
     * puțin, ca să nu insistăm, dar destul de scurt cât să ne revenim repede.
     */
    private static final Duration DURATA_CACHE_EROARE = Duration.ofMinutes(2);

    private final RestClient client;
    private final boolean activ;
    private final Map<String, Intrare> cache = new ConcurrentHashMap<>();

    private record Intrare(RaspunsOpenMeteo raspuns, java.time.Instant expira) {}

    public VremeService(@Value("${photoplanner.vreme.activ:true}") boolean activ,
                         @Value("${photoplanner.vreme.url:https://api.open-meteo.com/v1/forecast}") String url) {
        this.activ = activ;

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(4));
        fabrica.setReadTimeout(Duration.ofSeconds(6));

        this.client = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    /**
     * Prognoza la cele două ore de aur ale zilei.
     *
     * @param dimineata momentul din mijlocul orei de aur de dimineață, sau null dacă lipsește
     * @param seara     momentul din mijlocul orei de aur de seară, sau null dacă lipsește
     */
    public InfoVreme pentruOreleDeAur(double latitudine, double longitudine, LocalDate data,
                                       ZoneId zona, ZonedDateTime dimineata, ZonedDateTime seara) {
        if (!activ) {
            return InfoVreme.indisponibil("Prognoza este dezactivată.");
        }

        LocalDate azi = LocalDate.now(zona);
        if (data.isBefore(azi)) {
            return InfoVreme.indisponibil("Prognoza nu se poate face pentru o zi din trecut.");
        }
        long peste = ChronoUnit.DAYS.between(azi, data);
        if (peste > ZILE_PROGNOZA) {
            return InfoVreme.indisponibil(
                    "Prognoza merge doar cu " + ZILE_PROGNOZA + " zile în avans.");
        }

        RaspunsOpenMeteo raspuns = adu(latitudine, longitudine, zona, (int) peste + 1);
        if (raspuns == null || raspuns.hourly() == null || raspuns.hourly().time() == null) {
            return InfoVreme.indisponibil("Prognoza nu este disponibilă momentan.");
        }

        return InfoVreme.cu(
                laMoment(raspuns.hourly(), dimineata),
                laMoment(raspuns.hourly(), seara));
    }

    /**
     * Vremea la un singur moment — folosită pentru sesiunile planificate.
     *
     * @return vremea la ora respectivă, sau null dacă nu avem prognoză (zi trecută, prea departe
     *         în viitor, sau serviciul nu răspunde)
     */
    public VremeMoment laUnMoment(double latitudine, double longitudine, LocalDate data,
                                   ZoneId zona, ZonedDateTime moment) {
        if (moment == null) {
            return null;
        }
        InfoVreme info = pentruOreleDeAur(latitudine, longitudine, data, zona, moment, null);
        return info.disponibil() ? info.dimineata() : null;
    }

    /** Extrage vremea de la ora cea mai apropiată de momentul cerut. */
    private static VremeMoment laMoment(Orare orare, ZonedDateTime moment) {
        if (moment == null) {
            return null;
        }
        // Open-Meteo întoarce orele în fusul cerut, sub forma „2026-08-27T19:00”.
        String cautat = moment.truncatedTo(ChronoUnit.HOURS).toLocalDateTime().toString();

        int index = orare.time().indexOf(cautat);
        if (index < 0) {
            return null;
        }

        Integer nori = valoare(orare.cloudCover(), index);
        if (nori == null) {
            return null;
        }
        return new VremeMoment(nori, valoare(orare.precipitationProbability(), index),
                valoare(orare.temperature(), index));
    }

    private static <T> T valoare(List<T> lista, int index) {
        return lista != null && index < lista.size() ? lista.get(index) : null;
    }

    private RaspunsOpenMeteo adu(double latitudine, double longitudine, ZoneId zona, int zile) {
        String cheie = String.format("%.3f/%.3f/%s/%d", latitudine, longitudine, zona.getId(), zile);

        Intrare existenta = cache.get(cheie);
        if (existenta != null && existenta.expira().isAfter(java.time.Instant.now())) {
            return existenta.raspuns();
        }

        try {
            RaspunsOpenMeteo raspuns = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", latitudine)
                            .queryParam("longitude", longitudine)
                            .queryParam("hourly", "cloud_cover,precipitation_probability,temperature_2m")
                            .queryParam("timezone", zona.getId())
                            .queryParam("forecast_days", Math.min(zile, ZILE_PROGNOZA))
                            .build())
                    .retrieve()
                    .body(RaspunsOpenMeteo.class);

            cache.put(cheie, new Intrare(raspuns, java.time.Instant.now().plus(
                    raspuns != null ? DURATA_CACHE : DURATA_CACHE_EROARE)));
            return raspuns;
        } catch (RuntimeException e) {
            // Prognoza e un plus, nu o condiție: nu are voie să strice pagina locației.
            log.warn("Nu am putut lua prognoza pentru {}, {}: {}", latitudine, longitudine, e.toString());
            cache.put(cheie, new Intrare(null, java.time.Instant.now().plus(DURATA_CACHE_EROARE)));
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RaspunsOpenMeteo(Orare hourly) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Orare(
            List<String> time,
            @JsonProperty("cloud_cover") List<Integer> cloudCover,
            @JsonProperty("precipitation_probability") List<Integer> precipitationProbability,
            @JsonProperty("temperature_2m") List<Double> temperature) {}
}
