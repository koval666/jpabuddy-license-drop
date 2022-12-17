package ru.teadev.jpabuddylicensedrop.storage;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

@Entity
@Table(name = "license_key")
@Getter
@Setter
@ToString
public class LicenseKey {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "license_key_seq")
    @SequenceGenerator(name = "license_key_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "keyString", nullable = false, unique = true)
    private String key;

    @Embedded
    private Owner owner;//todo проверить что null, когда все поля null

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        LicenseKey that = (LicenseKey) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}