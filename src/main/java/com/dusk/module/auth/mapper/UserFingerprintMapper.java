package com.dusk.module.auth.mapper;

import com.dusk.common.rpc.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.module.auth.dto.fingerprint.SaveFingerprintInputDto;
import com.dusk.module.auth.entity.UserFingerprint;
import org.mapstruct.Mapper;

@Mapper
public interface UserFingerprintMapper {
    UserFingerprintMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(UserFingerprintMapper.class);

    UserFingerprintDto toDto(UserFingerprint entity);

    UserFingerprint toEntity(SaveFingerprintInputDto dto);
}
