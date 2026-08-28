package ro.utcn.photoplanner.service;

import ro.utcn.photoplanner.model.Utilizator;

/**
 * Trimite linkul de resetare către utilizator.
 * <p>
 * E o interfață separată ca modul de livrare să poată fi schimbat fără să se atingă logica de
 * securitate: implicit linkul se scrie în log (util în dezvoltare), iar pe un server real se
 * poate pune în loc o implementare care trimite email.
 */
public interface ExpeditorLinkResetare {

    void trimite(Utilizator utilizator, String link);
}
