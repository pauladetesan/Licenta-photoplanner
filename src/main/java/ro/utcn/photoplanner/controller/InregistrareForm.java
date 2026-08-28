package ro.utcn.photoplanner.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datele din formularul de creare a contului.
 * <p>
 * Până acum câmpurile veneau direct ca {@code @RequestParam}, fără nicio verificare pe server:
 * atributele {@code type="email"} și {@code required} din pagină opresc doar un browser, nu și
 * o cerere trimisă de-a dreptul. Se puteau astfel crea conturi cu adresa „nu-e-email” sau cu
 * numele gol — primele rămân definitiv fără cale de recuperare a parolei, fiindcă linkul de
 * resetare nu are unde să ajungă.
 */
public class InregistrareForm {

    @NotBlank(message = "Numele afișat este obligatoriu.")
    @Size(max = 100, message = "Numele afișat poate avea cel mult 100 de caractere.")
    private String numeAfisat;

    /*
     * `@Email` fără regexp e mai permisiv decât pare: acceptă „cineva@domeniu”, fără punct și
     * fără domeniu de nivel superior. Pentru o aplicație în care singura cale de recuperare a
     * parolei e emailul, o adresă către care nu se poate trimite nimic e la fel de rea ca una
     * lipsă, așa că cerem explicit un domeniu cu punct.
     */
    @NotBlank(message = "Adresa de email este obligatorie.")
    @Email(regexp = "^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$",
           message = "Adresa de email nu pare validă.")
    @Size(max = 150, message = "Adresa de email poate avea cel mult 150 de caractere.")
    private String email;

    /*
     * Maximul nu e o toană: bcrypt ia în calcul doar primii 72 de octeți ai parolei și îi
     * ignoră tăcut pe ceilalți. Mai bine spunem limita decât să lăsăm pe cineva să creadă că
     * are o parolă de 200 de caractere.
     */
    @NotBlank(message = "Parola este obligatorie.")
    @Size(min = 8, max = 72, message = "Parola trebuie să aibă între 8 și 72 de caractere.")
    private String parola;

    public String getNumeAfisat() { return numeAfisat; }
    public void setNumeAfisat(String numeAfisat) { this.numeAfisat = numeAfisat; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getParola() { return parola; }
    public void setParola(String parola) { this.parola = parola; }
}
