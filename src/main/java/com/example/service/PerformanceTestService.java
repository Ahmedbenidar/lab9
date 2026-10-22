package com.example.service;

import com.example.model.Auteur;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

public class PerformanceTestService {

    private final EntityManagerFactory emf;

    public PerformanceTestService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    private Statistics stats() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    /**
     * Les compteurs Hibernate sont cumulatifs : on prend un instantané avant
     * chaque test et on affiche la différence, ce qui laisse les statistiques
     * globales intactes pour le récapitulatif final.
     */
    private long[] compteurs() {
        Statistics s = stats();
        return new long[] {
                s.getPrepareStatementCount(),
                s.getSecondLevelCacheHitCount(),
                s.getSecondLevelCacheMissCount(),
                s.getSecondLevelCachePutCount()
        };
    }

    /**
     * Requête de base + accès à la collection LAZY : une requête par auteur,
     * c'est le problème N+1 (1 + N requêtes).
     */
    public Mesure testSansOptimisation() {
        long[] avant = compteurs();
        long start = System.nanoTime();
        int auteurs = 0;
        int livres = 0;
        EntityManager em = emf.createEntityManager();
        try {
            List<Auteur> resultat = em.createQuery("SELECT a FROM Auteur a", Auteur.class).getResultList();
            auteurs = resultat.size();
            for (Auteur auteur : resultat) {
                livres += auteur.getLivres().size();
            }
        } finally {
            em.close();
        }
        return mesure("Sans optimisation (N+1)", start, auteurs, livres, avant);
    }

    /**
     * JOIN FETCH charge auteurs et livres en une seule requête SQL.
     * DISTINCT evite les doublons d'auteurs dus à la jointure.
     */
    public Mesure testAvecJoinFetch() {
        long[] avant = compteurs();
        long start = System.nanoTime();
        int auteurs = 0;
        int livres = 0;
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Auteur> query = em.createQuery(
                    "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
            List<Auteur> resultat = query.getResultList();
            auteurs = resultat.size();
            for (Auteur auteur : resultat) {
                livres += auteur.getLivres().size();
            }
        } finally {
            em.close();
        }
        return mesure("JOIN FETCH", start, auteurs, livres, avant);
    }

    /**
     * Même résultat que JOIN FETCH, mais le chargement est décrit de façon
     * déclarative par un Entity Graph : la requête JPQL reste inchangée.
     */
    public Mesure testAvecEntityGraph() {
        long[] avant = compteurs();
        long start = System.nanoTime();
        int auteurs = 0;
        int livres = 0;
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<?> graph = em.createEntityGraph("Auteur.livres");
            List<Auteur> resultat = em.createQuery("SELECT a FROM Auteur a", Auteur.class)
                    .setHint("javax.persistence.fetchgraph", graph)
                    .getResultList();
            auteurs = resultat.size();
            for (Auteur auteur : resultat) {
                livres += auteur.getLivres().size();
            }
        } finally {
            em.close();
        }
        return mesure("Entity Graph (Auteur.livres)", start, auteurs, livres, avant);
    }

    /**
     * Deux lectures successives dans deux sessions distinctes : la première
     * alimente le cache de second niveau, la seconde ne doit plus toucher la base.
     */
    public List<Mesure> testAvecCache(Long auteurId) {
        emf.unwrap(SessionFactory.class).getCache().evictAll();
        List<Mesure> mesures = new ArrayList<>();

        mesures.add(lectureParId(auteurId, "Cache L2 - lecture 1 (miss)"));
        mesures.add(lectureParId(auteurId, "Cache L2 - lecture 2 (hit)"));

        return mesures;
    }

    private Mesure lectureParId(Long auteurId, String libelle) {
        long[] avant = compteurs();
        long start = System.nanoTime();
        int auteurs = 0;
        int livres = 0;
        EntityManager em = emf.createEntityManager();
        try {
            Auteur auteur = em.find(Auteur.class, auteurId);
            if (auteur != null) {
                auteurs = 1;
                livres = auteur.getLivres().size();
            }
        } finally {
            em.close();
        }
        return mesure(libelle, start, auteurs, livres, avant);
    }

    private Mesure mesure(String libelle, long startNanos, int auteurs, int livres, long[] avant) {
        long dureeMs = (System.nanoTime() - startNanos) / 1_000_000;
        long[] apres = compteurs();
        return new Mesure(libelle, dureeMs, apres[0] - avant[0], auteurs, livres,
                apres[1] - avant[1], apres[2] - avant[2]);
    }

    public void afficherStatistiquesGlobales() {
        Statistics s = stats();
        System.out.println("--- Statistiques Hibernate (cumul) ---");
        System.out.println("Entités chargées (load)          : " + s.getEntityLoadCount());
        System.out.println("Requêtes HQL exécutées           : " + s.getQueryExecutionCount());
        System.out.println("Cache L2 - hits                  : " + s.getSecondLevelCacheHitCount());
        System.out.println("Cache L2 - misses                : " + s.getSecondLevelCacheMissCount());
        System.out.println("Cache L2 - puts                  : " + s.getSecondLevelCachePutCount());
    }
}
