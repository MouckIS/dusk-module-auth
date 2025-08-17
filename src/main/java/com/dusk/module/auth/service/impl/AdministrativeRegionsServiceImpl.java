package com.dusk.module.auth.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.redis.RedisUtil;
import com.dusk.module.auth.dto.administrativeregions.RegionsDto;
import com.dusk.module.auth.service.IAdministrativeRegionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: 黄辉鸿
 * @create: 2022-04-27 09:26
 **/
@Service
public class AdministrativeRegionsServiceImpl implements IAdministrativeRegionsService {
    @Autowired
    RedisUtil<Object> redisUtil;
    @Autowired
    ObjectMapper objectMapper;

    private static final String PROVINCE_PATH = "/regions/province.json";
    private static final String CITY_PATH = "/regions/city.json";
    private static final String AREA_PATH = "/regions/area.json";
    private static final String STREET_PATH = "/regions/street.json";

    private final List<RegionsDto> regionsList = new ArrayList<>();

    @SneakyThrows
    @PostConstruct
    private void initRegionToRedis() {
        List<Map<String, Object>> provinceList = objectMapper.readValue(this.getClass().getResourceAsStream(PROVINCE_PATH), new TypeReference<>() {
        });
        Map<String, List<Map<String, Object>>> cityMap = objectMapper.readValue(this.getClass().getResourceAsStream(CITY_PATH), new TypeReference<>() {
        });
        Map<String, List<Map<String, Object>>> areaMap = objectMapper.readValue(this.getClass().getResourceAsStream(AREA_PATH), new TypeReference<>() {
        });
        Map<String, List<Map<String, Object>>> streetMap = objectMapper.readValue(this.getClass().getResourceAsStream(STREET_PATH), new TypeReference<>() {
        });

        provinceList.forEach(i -> {
            regionsList.add(new RegionsDto(
                    (Integer) i.get("id"),
                    (String) i.get("name"),
                    null,
                    cityMap.containsKey(i.get("id").toString())
            ));
        });

        cityMap.forEach((k, v) -> {
            v.forEach(i -> {
                regionsList.add(new RegionsDto(
                        (Integer) i.get("id"),
                        (String) i.get("name"),
                        Integer.valueOf(k),
                        areaMap.containsKey(i.get("id").toString())
                ));
            });
        });

        areaMap.forEach((k, v) -> {
            v.forEach(i -> {
                regionsList.add(new RegionsDto(
                        (Integer) i.get("id"),
                        (String) i.get("name"),
                        Integer.valueOf(k),
                        streetMap.containsKey(i.get("id").toString())
                ));
            });
        });
    }

    @Override
    public List<RegionsDto> getRegions() {
        return regionsList;
    }

    @Override
    public List<RegionsDto> getStreet(String id) {
        Map<String, List<Map<String, Object>>> streetMap;
        try {
            streetMap = objectMapper.readValue(this.getClass().getResourceAsStream(STREET_PATH), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new BusinessException("序列化错误");
        }

        List<Map<String, Object>> street = streetMap.get(id);
        List<RegionsDto> regionsList = new ArrayList<>();

        if (street != null) {
            street.forEach(i -> {
                regionsList.add(new RegionsDto(
                        (Integer) i.get("id"),
                        (String) i.get("name"),
                        null,
                        false
                ));
            });
        }

        return regionsList;
    }
}
