package ru.teadev.jpabuddylicensedrop.storage;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseKeyRepository extends JpaRepository<LicenseKey, Long> {

    long countByOwner_UserIdNull();

    long countByOwner_UserIdNotNull();

    List<LicenseKey> findByOwner_UserIdNull();

    List<LicenseKey> findByOwner_UserIdNotNull();

    long deleteByKeyIn(Collection<String> keys);

}