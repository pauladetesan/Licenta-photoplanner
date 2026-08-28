package ro.utcn.photoplanner.service;

import ro.utcn.photoplanner.model.MomentZi;
import ro.utcn.photoplanner.model.Tema;

/**
 * Ce înseamnă „condiții bune” pentru fiecare temă.
 * <p>
 * Regulile de aici nu sunt inventate acum: ele scriau deja, în cuvinte, în
 * {@link VremeMoment#verdictPentru} și {@link InfoLuna#verdictAstro}. Aici sunt aceleași
 * judecăți, puse în cifre ca să poată fi comparate între ele.
 * <p>
 * Aproape toate se reduc la aceeași formă — „valoarea ideală e X, iar abaterea acceptabilă e Y” —
 * așa că se folosesc toate prin {@link ScorService#apropiere}. Excepțiile sunt luna și ploaia,
 * unde chiar e adevărat că mai puțin e mai bine.
 */
final class PreferinteTema {

    private PreferinteTema() {
    }

    /** O valoare ideală împreună cu cât de repede scade nota pe măsură ce te depărtezi de ea. */
    record Optim(double valoare, double toleranta) {}

    /**
     * Cât de potrivit e momentul zilei pentru temă, între 0 și 1.
     * <p>
     * Nu e un factor cu pondere, ci un multiplicator: astrofotografia la prânz nu e „o idee mai
     * slabă”, e imposibilă, și niciun cer senin n-ar trebui s-o salveze.
     */
    static double potrivireMoment(Tema tema, MomentZi moment) {
        return switch (tema) {
            case PORTRET -> switch (moment) {
                case ORA_AUR_DIMINEATA, ORA_AUR_SEARA -> 1.0;
                case RASARIT, APUS -> 0.7;
                case ZI -> 0.5;
                case NOAPTE -> 0.1;
            };
            case PEISAJ -> switch (moment) {
                case ORA_AUR_DIMINEATA, ORA_AUR_SEARA -> 1.0;
                case RASARIT, APUS -> 0.9;
                case ZI -> 0.4;
                case NOAPTE -> 0.3;
            };
            case ARHITECTURA -> switch (moment) {
                case ORA_AUR_DIMINEATA, ORA_AUR_SEARA -> 0.9;
                case APUS -> 0.8;
                case RASARIT, ZI -> 0.7;
                // Clădirile luminate pe cer albastru sunt un clasic, nu o excepție.
                case NOAPTE -> 0.6;
            };
            // Astrofotografia are un singur moment posibil. Restul chiar sunt zero.
            case ASTRO -> moment == MomentZi.NOAPTE ? 1.0 : 0.0;
            case STRADA -> switch (moment) {
                case ZI, ORA_AUR_SEARA -> 0.9;
                case ORA_AUR_DIMINEATA -> 0.8;
                case APUS -> 0.7;
                case NOAPTE -> 0.6;
                case RASARIT -> 0.5;
            };
        };
    }

    /**
     * Acoperirea ideală cu nori, în procente.
     * <p>
     * Aici se vede cel mai bine de ce scorul nu poate fi „mai puțin, mai bine”: la o oră de aur
     * un cer gol nu are ce colora, iar pentru portret o zi acoperită e chiar lumina dorită.
     */
    static Optim nori(Tema tema, MomentZi moment) {
        // Noaptea contează doar cât cer liber rămâne, indiferent de temă.
        if (moment == MomentZi.NOAPTE) {
            return new Optim(0, 25);
        }

        return switch (tema) {
            // Lumină difuză, fără umbre tăioase pe față.
            case PORTRET -> moment == MomentZi.ZI
                    ? new Optim(85, 30)
                    : new Optim(35, 30);
            // Norii împrăștiați prind culoarea; cerul acoperit o stinge.
            case PEISAJ -> moment == MomentZi.ZI
                    ? new Optim(40, 35)
                    : new Optim(40, 25);
            case ARHITECTURA -> new Optim(30, 35);
            // Strada se descurcă în aproape orice lumină.
            case STRADA -> new Optim(55, 45);
            case ASTRO -> new Optim(0, 20);
        };
    }

    /**
     * Unghiul ideal al soarelui față de direcția de fotografiere, în grade.
     * <p>
     * Se citește pe {@code Math.abs(unghiRelativ)}: 0 = soarele în fața aparatului
     * (contra-lumină), 90 = lumină laterală, 180 = soarele în spatele tău (lumină frontală).
     */
    static Optim unghiLumina(Tema tema) {
        return switch (tema) {
            // Contra-lumina dă conturul luminos din jurul părului; frontala orbește subiectul.
            case PORTRET -> new Optim(20, 60);
            case PEISAJ -> new Optim(60, 60);
            // Lumina laterală scoate în relief textura fațadei.
            case ARHITECTURA -> new Optim(90, 50);
            case STRADA -> new Optim(45, 70);
            case ASTRO -> new Optim(0, 180);
        };
    }

    /** Temperatura la care se stă cel mai comod afară, în grade Celsius. */
    static Optim temperatura() {
        return new Optim(18, 15);
    }
}
