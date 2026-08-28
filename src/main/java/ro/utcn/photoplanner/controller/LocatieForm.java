package ro.utcn.photoplanner.controller;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Tema;
import ro.utcn.photoplanner.model.Vizibilitate;

import java.util.HashSet;
import java.util.Set;

/**
 * Datele din formularul de adăugare/editare a unei locații.
 * <p>
 * Clasă separată de entitate ca să putem valida ce vine din formular înainte să atingem baza
 * de date, și ca să putem re-afișa formularul cu erori fără să stricăm locația salvată.
 */
public class LocatieForm {

    @NotBlank(message = "Numele este obligatoriu.")
    @Size(max = 120, message = "Numele poate avea cel mult 120 de caractere.")
    private String nume;

    @Size(max = 1000, message = "Descrierea poate avea cel mult 1000 de caractere.")
    private String descriere;

    @NotNull(message = "Latitudinea este obligatorie.")
    @DecimalMin(value = "-90.0", message = "Latitudinea trebuie să fie între -90 și 90.")
    @DecimalMax(value = "90.0", message = "Latitudinea trebuie să fie între -90 și 90.")
    private Double latitudine;

    @NotNull(message = "Longitudinea este obligatorie.")
    @DecimalMin(value = "-180.0", message = "Longitudinea trebuie să fie între -180 și 180.")
    @DecimalMax(value = "180.0", message = "Longitudinea trebuie să fie între -180 și 180.")
    private Double longitudine;

    @Min(value = 0, message = "Orientarea trebuie să fie între 0 și 359 de grade.")
    @Max(value = 359, message = "Orientarea trebuie să fie între 0 și 359 de grade.")
    private Integer orientareScena;

    @NotNull(message = "Alege vizibilitatea.")
    private Vizibilitate vizibilitate = Vizibilitate.PRIVATA;

    private Set<Tema> teme = new HashSet<>();

    public LocatieForm() {
    }

    /** Pre-completează formularul cu valorile unei locații existente, pentru editare. */
    public static LocatieForm din(Locatie locatie) {
        LocatieForm form = new LocatieForm();
        form.nume = locatie.getNume();
        form.descriere = locatie.getDescriere();
        form.latitudine = locatie.getLatitudine();
        form.longitudine = locatie.getLongitudine();
        form.orientareScena = locatie.getOrientareScena();
        form.vizibilitate = locatie.getVizibilitate();
        form.teme = new HashSet<>(locatie.getTeme());
        return form;
    }

    public String getNume() { return nume; }
    public void setNume(String nume) { this.nume = nume; }

    public String getDescriere() { return descriere; }
    public void setDescriere(String descriere) { this.descriere = descriere; }

    public Double getLatitudine() { return latitudine; }
    public void setLatitudine(Double latitudine) { this.latitudine = latitudine; }

    public Double getLongitudine() { return longitudine; }
    public void setLongitudine(Double longitudine) { this.longitudine = longitudine; }

    public Integer getOrientareScena() { return orientareScena; }
    public void setOrientareScena(Integer orientareScena) { this.orientareScena = orientareScena; }

    public Vizibilitate getVizibilitate() { return vizibilitate; }
    public void setVizibilitate(Vizibilitate vizibilitate) { this.vizibilitate = vizibilitate; }

    public Set<Tema> getTeme() { return teme; }
    public void setTeme(Set<Tema> teme) { this.teme = teme != null ? teme : new HashSet<>(); }
}
