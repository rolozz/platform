package com.cor.userservice.repository;

import com.cor.userservice.entities.UserProfile;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью UserProfile.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    /**
     * Находит пэйдж всех профилей
     *
     * @param pageable по профилям
     * @return страницу UserProfile
     */
    @NonNull
    Page<UserProfile> findAll(@NonNull Pageable pageable);

    /**
     * Находит профиль пользователя по email.
     *
     * @param email email пользователя
     * @return Optional с профилем пользователя, если найден
     */
    Optional<UserProfile> findByEmail(String email);

    /**
     * Проверяет существование профиля по email.
     *
     * @param email email для проверки
     * @return true, если профиль с таким email существует
     */
    boolean existsByEmail(String email);

    /**
     * Проверяет существование профиля по username.
     *
     * @param username username для проверки
     * @return true, если профиль с таким username существует
     */
    boolean existsByUsername(String username);
}
