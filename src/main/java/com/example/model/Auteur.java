package com.example.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "auteurs")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraph(name = "Auteur.livres", attributeNodes = @NamedAttributeNode("livres"))
public class Auteur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Livre> livres = new ArrayList<>();

    public Auteur() {
    }

    public Auteur(String nom) {
        this.nom = nom;
    }

    public void ajouterLivre(Livre livre) {
        livres.add(livre);
        livre.setAuteur(this);
    }

    public void retirerLivre(Livre livre) {
        livres.remove(livre);
        livre.setAuteur(null);
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public List<Livre> getLivres() {
        return Collections.unmodifiableList(livres);
    }

    @Override
    public String toString() {
        return "Auteur{id=" + id + ", nom='" + nom + "', livres=" + livres.size() + '}';
    }
}
