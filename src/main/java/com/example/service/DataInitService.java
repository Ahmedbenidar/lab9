package com.example.service;

import com.example.model.Auteur;
import com.example.model.Livre;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

public class DataInitService {

    private final EntityManagerFactory emf;

    public DataInitService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Crée 10 auteurs ayant chacun 5 livres (50 livres au total).
     *
     * @return nombre d'auteurs insérés
     */
    public int initData(int nbAuteurs, int nbLivresParAuteur) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            for (int i = 1; i <= nbAuteurs; i++) {
                Auteur auteur = new Auteur("Auteur " + i);
                for (int j = 1; j <= nbLivresParAuteur; j++) {
                    auteur.ajouterLivre(new Livre("Livre " + j + " de l'auteur " + i));
                }
                em.persist(auteur);
            }
            tx.commit();
            return nbAuteurs;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
