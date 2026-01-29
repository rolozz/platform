package com.cor.userservice.mapper;

import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.entities.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
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
    UserProfile toEntity(UserProfileDto dto);
}
