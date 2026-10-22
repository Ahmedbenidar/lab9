package com.example;

import com.example.service.DataInitService;
import com.example.service.Mesure;
import com.example.service.PerformanceTestService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        try {
            int nbAuteurs = 10;
            int nbLivres = 5;

            DataInitService dataInitService = new DataInitService(emf);
            dataInitService.initData(nbAuteurs, nbLivres);
            PerformanceTestService performanceTestService = new PerformanceTestService(emf);

            Long premierAuteurId = premierAuteurId(emf);

            System.out.println();
            System.out.println("=== TP 7 : cache de second niveau, N+1, JOIN FETCH, Entity Graph ===");
            System.out.println("Données initialisées : " + nbAuteurs + " auteurs, "
                    + (nbAuteurs * nbLivres) + " livres");
            System.out.println();

            Mesure n1 = performanceTestService.testSansOptimisation();
            System.out.println(n1.ligne());

            Mesure joinFetch = performanceTestService.testAvecJoinFetch();
            System.out.println(joinFetch.ligne());

            Mesure graph = performanceTestService.testAvecEntityGraph();
            System.out.println(graph.ligne());

            System.out.println();
            for (Mesure mesure : performanceTestService.testAvecCache(premierAuteurId)) {
                System.out.println(mesure.ligne());
            }

            System.out.println();
            performanceTestService.afficherStatistiquesGlobales();
            System.out.println();
            System.out.println("Gain JOIN FETCH : " + n1.getRequetesSql() + " requêtes -> "
                    + joinFetch.getRequetesSql() + " requête(s)");
        } finally {
            emf.close();
        }
    }

    private static Long premierAuteurId(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT MIN(a.id) FROM Auteur a", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }
}
