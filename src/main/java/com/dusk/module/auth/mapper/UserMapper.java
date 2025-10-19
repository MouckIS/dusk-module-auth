package com.dusk.module.auth.mapper;

import com.dusk.common.rpc.auth.dto.*;
import com.dusk.module.auth.dto.mobilelogin.MobileUserDto;
import com.dusk.module.auth.dto.user.*;
import com.dusk.module.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(UserMapper.class);

    //@Mapping(target = MobileUserDto.Fields.tenant, source = "tenant.name")
    //@Mapping(target = MobileUserDto.Fields.tenantName, source = "tenant.tenantName")
    //MobileUserDto toMobileUserDto(User entity);

    UserListDto toListDto(User entity);

    UserFullListDto toFullListDto(User entity);

    UserFullListSyncDto toFullListSyncDto(User entity);

    GetUsersInput inputToGetUsersInput(UserInputDto dto);

    UserOrgDto toUserOrgDto(User entity);

    User userSimpleDtoToEntity(UserSimpleDto dto);

    UserExcellDto toExcellDto(User entity);

    UserEditDto toEditDto(User entity);

    User CreateExternalUserInputToEntity(CreateExternalUserInput input);

    User editDtoToEntity(UserEditDto dto);

    UserListForLoginDto toListForLoginDto(User entity);

    UserForSelectDto toSelectDto(User entity);

    BaseUserInfoDto toBaseUserInfoDto(User entity);

    AccountInfoDto toAccountInfoDto(User entity);

    GetExternalUserEditOutput toGetExternalUserEditOutput(User entity);
}
