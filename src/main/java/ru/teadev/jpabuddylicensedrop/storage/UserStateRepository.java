package ru.teadev.jpabuddylicensedrop.storage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStateRepository extends JpaRepository<UserState, Long> {
}