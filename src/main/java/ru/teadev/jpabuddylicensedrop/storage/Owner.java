package ru.teadev.jpabuddylicensedrop.storage;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@Getter
@Setter
@ToString
public class Owner {
    @Column(name = "userId", unique = true)
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "firtname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

}