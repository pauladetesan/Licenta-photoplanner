package ro.utcn.photoplanner.controller;

import ro.utcn.photoplanner.model.SesiuneFoto;
import ro.utcn.photoplanner.service.VremeMoment;

import java.time.ZonedDateTime;

/**
 * O sesiune planificată împreună cu ce știm despre ziua ei: ora exactă a momentului ales și,
 * dacă e destul de aproape, vremea de atunci.
 *
 * @param moment ora momentului ales, în fusul locației; null dacă în ziua aceea nu există
 *               (de exemplu o oră de aur dincolo de cercul polar)
 * @param vreme  prognoza la acea oră, sau null dacă nu e disponibilă
 */
public record SesiunePlanificata(SesiuneFoto sesiune, ZonedDateTime moment, VremeMoment vreme) {
}
