package com.example.service;

/**
 * Résultat d'une mesure : nombre de requêtes SQL réellement émises (statistiques
 * Hibernate) et durée observée pour une lecture complète.
 */
public class Mesure {

    private final String libelle;
    private final long dureeMs;
    private final long requetesSql;
    private final int auteurs;
    private final int livres;
    private final long cacheHits;
    private final long cacheMisses;

    public Mesure(String libelle, long dureeMs, long requetesSql,
                  int auteurs, int livres, long cacheHits, long cacheMisses) {
        this.libelle = libelle;
        this.dureeMs = dureeMs;
        this.requetesSql = requetesSql;
        this.auteurs = auteurs;
        this.livres = livres;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
    }

    public String getLibelle() {
        return libelle;
    }

    public long getDureeMs() {
        return dureeMs;
    }

    public long getRequetesSql() {
        return requetesSql;
    }

    public int getAuteurs() {
        return auteurs;
    }

    public int getLivres() {
        return livres;
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

    public String ligne() {
        return String.format("%-34s | SQL: %2d | auteurs: %2d | livres: %2d | hits: %d | misses: %d | %d ms",
                libelle, requetesSql, auteurs, livres, cacheHits, cacheMisses, dureeMs);
    }
}
