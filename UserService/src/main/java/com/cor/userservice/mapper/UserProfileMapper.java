package com.cor.userservice.mapper;

import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.dto.UserProfileKeycloakDto;
import com.cor.userservice.entities.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Маппер для конвертации между сущностью UserProfile и DTO.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserProfileMapper {

    /**
     * Преобразует сущность UserProfile в DTO.
     *
     * @param entity сущность UserProfile
     * @return DTO пользовательского профиля
     */
    UserProfileDto toDto(UserProfile entity);

    /**
     * Преобразует DTO в сущность UserProfile.
     *
     * @param dto DTO пользовательского профиля
     * @return сущность UserProfile
     */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toEntity(UserProfileKeycloakDto dto);

    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile updateEntityFromDto(UserProfileKeycloakDto dto, @MappingTarget UserProfile entity);
}
