package ru.teadev.jpabuddylicensedrop.storage;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "user_state")
public class UserState {
    @Id
    @Column(name = "userId", nullable = false)
    @NonNull
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_action")
    private AdminAction previousAction;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        UserState userState = (UserState) o;
        return userId != null && Objects.equals(userId, userState.userId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}