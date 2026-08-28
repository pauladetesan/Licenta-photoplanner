package ro.utcn.photoplanner.service;

import ro.utcn.photoplanner.model.FactorScor;

/**
 * Cât a contat un factor în scorul unui moment.
 * <p>
 * Se păstrează ca să se poată arăta <em>de ce</em> un moment a ieșit bine sau prost. Un scor
 * fără explicație e greu de crezut și imposibil de reglat.
 *
 * @param factor  factorul
 * @param nota    nota lui, între 0 și 1
 * @param pondere ponderea aleasă de utilizator
 * @param detaliu valoarea măsurată, în cuvinte (ex. „40% nori”)
 */
public record ContributieFactor(FactorScor factor, double nota, int pondere, String detaliu) {

    /** Cât aduce în suma ponderată. */
    public double aport() {
        return nota * pondere;
    }

    /** Nota ca procent, pentru afișare. */
    public int procent() {
        return (int) Math.round(nota * 100);
    }
}
