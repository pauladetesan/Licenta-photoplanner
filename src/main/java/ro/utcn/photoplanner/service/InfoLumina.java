package ro.utcn.photoplanner.service;

import java.time.ZonedDateTime;

/**
 * Cum cade lumina pe scenă la un moment dat.
 *
 * @param moment        eticheta momentului (ex. „Oră de aur — dimineață”)
 * @param cand          momentul efectiv pentru care s-a făcut calculul
 * @param azimutSoare   azimutul soarelui, în grade (0 = nord)
 * @param inaltimeSoare înălțimea soarelui deasupra orizontului, în grade
 * @param unghiRelativ  unghiul soarelui față de direcția de fotografiere, în [-180, 180];
 *                      0 = soarele exact în fața aparatului, pozitiv = spre dreapta
 * @param tip           interpretarea acestui unghi
 */
public record InfoLumina(
        String moment,
        ZonedDateTime cand,
        double azimutSoare,
        double inaltimeSoare,
        int unghiRelativ,
        TipLumina tip
) {
}
