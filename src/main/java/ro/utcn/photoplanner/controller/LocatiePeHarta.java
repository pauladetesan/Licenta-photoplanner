package ro.utcn.photoplanner.controller;

import java.util.List;

/**
 * O locație așa cum apare pe harta de pe prima pagină.
 * Doar câmpurile necesare afișării — nu trimitem mai mult decât se vede.
 *
 * @param coperta id-ul primei fotografii, sau null dacă locația nu are poze
 * @param privata true doar pentru locațiile private ale celui care se uită la hartă
 */
public record LocatiePeHarta(Long id, String nume, double latitudine, double longitudine,
                              List<String> teme, Long coperta, boolean privata) {
}
