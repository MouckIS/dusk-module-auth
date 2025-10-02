package com.dusk.module.auth.repository;

import com.dusk.common.core.annotation.DisableGlobalFilter;
import com.dusk.common.core.enums.EUnitType;
import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.module.auth.entity.Station;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.dto.station.StationUserDto;
import com.dusk.module.auth.dto.station.StationUserListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface IStationRepository extends IBaseRepository<Station> {
    @DisableGlobalFilter
    List<Station> findAllByDr(int dr);

    @Query("select station from Station station inner join station.users u where u.id = ?1 ")
    List<Station> getStationsByUser(Long userId);

    /**
     * 查询厂站下的用户，stationIds为-1时查询所有用户
     * @param stationIds
     * @param filter
     * @param pageable
     * @return
     */
    @Query("select distinct u from Station station inner join station.users u " +
            "where (-1L in (:stationIds) or station.id in (:stationIds)) " +
            "and (:filter is null or :filter = '' or u.name like %:filter% or u.userName like %:filter%)")
    Page<User> findUsers(Set<Long> stationIds, String filter, Pageable pageable);


    /**
     * 查询厂站下的用户，queryOrgaIds为-1时查询所有用户
     * @param queryOrgaIds
     * @param filter
     * @param pageable
     * @return
     */
    @Query("select new com.dusk.module.auth.dto.station.StationUserListDto(" +
            "u.id, u.name, u.userName, u.emailAddress, station.id, station.displayName) " +
            "from Station station inner join station.users u " +
            "where (-1L in (:queryStationIds) or station.id in (:queryStationIds)) " +
            "and (:filter is null or :filter = '' or u.name like %:filter% or u.userName like %:filter%)" +
            "and (:userType is null or u.userType = :userType)")
    Page<StationUserListDto> getStationUsers(@Param("queryStationIds") Set<Long> queryOrgaIds, @Param("filter") String filter, EUnitType userType, Pageable pageable);


    @Query("select distinct new com.dusk.module.auth.dto.station.StationUserDto(u.id,u.name,u.userName) from User u where u.id not in (select u2.id from Station station inner join station.users u2 where station.id = :stationId) and (:filter is null or :filter = '' or u.name like %:filter% or u.userName like %:filter%) and (:userType is null or u.userType = :userType) and u.userName is not null")
    Page<StationUserDto> getNotAssignedStationUsers(@Param("stationId") Long stationId, @Param("filter") String filter, EUnitType userType, Pageable pageable);
}