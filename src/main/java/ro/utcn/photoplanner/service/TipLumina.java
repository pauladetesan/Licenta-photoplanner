package ro.utcn.photoplanner.service;

/** Tipul de lumină pe care îl primește scena, în funcție de poziția soarelui față de direcția de fotografiere. */
public enum TipLumina {

    CONTRALUMINA("Contra-lumină", "Soarele e în fața aparatului, în spatele subiectului — siluete, halou, contrast mare."),
    LATERALA_DREAPTA("Lumină laterală (dreapta)", "Soarele vine din dreapta ta — relief și umbre lungi pe subiect."),
    LATERALA_STANGA("Lumină laterală (stânga)", "Soarele vine din stânga ta — relief și umbre lungi pe subiect."),
    FRONTALA("Lumină frontală", "Soarele e în spatele tău și luminează direct subiectul — culori saturate, umbre puține."),
    SUB_ORIZONT("Soarele sub orizont", "Soarele e sub linia orizontului la această oră.");

    private final String eticheta;
    private final String explicatie;

    TipLumina(String eticheta, String explicatie) {
        this.eticheta = eticheta;
        this.explicatie = explicatie;
    }

    public String getEticheta() { return eticheta; }
    public String getExplicatie() { return explicatie; }
}
