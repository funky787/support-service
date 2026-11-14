package com.example.support.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;

    protected Category() {}
    public Long getId() { return id; }
    public String getName() { return name; }
}
