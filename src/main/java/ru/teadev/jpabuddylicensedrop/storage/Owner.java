package ru.teadev.jpabuddylicensedrop.storage;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
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