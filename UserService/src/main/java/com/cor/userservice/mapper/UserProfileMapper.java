package com.cor.userservice.mapper;

import com.cor.userservice.dto.UserProfileCreateDto;
import com.cor.userservice.dto.UserProfileDto;
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
    @Mapping(target = "keycloakId", ignore = true)
    UserProfileDto toDto(UserProfile entity);
    
    /**
     * Преобразует DTO в сущность UserProfile.
     *
     * @param dto DTO пользовательского профиля
     * @return сущность UserProfile
     */
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    UserProfile toEntity(UserProfileCreateDto dto);

    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    UserProfile updateEntityFromDto(UserProfileDto dto, @MappingTarget UserProfile entity);
}
