package ro.utcn.photoplanner.service;

import java.time.Duration;
import java.time.ZonedDateTime;

/** Momentele-cheie de lumină pentru o zi, într-o anumită locație. */
public record InfoSoare(
        ZonedDateTime rasarit,
        ZonedDateTime apus,
        ZonedDateTime oraDeAurDimineataStart,
        ZonedDateTime oraDeAurDimineataStop,
        ZonedDateTime oraDeAurSearaStart,
        ZonedDateTime oraDeAurSearaStop
) {

    /** Mijlocul orei de aur de dimineață, sau null dacă fereastra nu există în ziua asta. */
    public ZonedDateTime mijlocDimineata() {
        return mijloc(oraDeAurDimineataStart, oraDeAurDimineataStop);
    }

    /** Mijlocul orei de aur de seară, sau null dacă fereastra nu există în ziua asta. */
    public ZonedDateTime mijlocSeara() {
        return mijloc(oraDeAurSearaStart, oraDeAurSearaStop);
    }

    private static ZonedDateTime mijloc(ZonedDateTime inceput, ZonedDateTime sfarsit) {
        if (inceput == null || sfarsit == null) {
            return null;
        }
        return inceput.plusSeconds(Duration.between(inceput, sfarsit).getSeconds() / 2);
    }
}
