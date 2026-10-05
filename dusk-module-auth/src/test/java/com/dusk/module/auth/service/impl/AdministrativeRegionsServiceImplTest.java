package com.dusk.module.auth.service.impl;

import com.dusk.module.auth.dto.administrativeregions.RegionsDto;
import com.dusk.module.auth.service.IAdministrativeRegionsService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AdministrativeRegionsServiceImpl} 单元测试，目标：100% 分支覆盖。
 * <p>
 * 直接复用生产资源 {@code /regions/*.json}，不 mock {@link ObjectMapper}，
 * 使真实反序列化路径也被执行。
 */
class AdministrativeRegionsServiceImplTest {

    private IAdministrativeRegionsService service;

    @BeforeEach
    void setUp() {
        AdministrativeRegionsServiceImpl impl = new AdministrativeRegionsServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(impl, "objectMapper", new ObjectMapper());
        service = impl;
    }

    @Test
    @DisplayName("getStreet：已知父级编码时返回其下级街道")
    void getStreetReturnsChildrenForKnownParent() {
        // street.json 的结构为 { "父级编码": [ {id, name}, ... ] }，143 为其真实存在的键
        List<RegionsDto> streets = service.getStreet("143");

        assertThat(streets).isNotEmpty();
        assertThat(streets.get(0).getNa()).isNotBlank();
        assertThat(streets.get(0).getHc()).isFalse();
    }

    @Test
    @DisplayName("getStreet：未知父级编码时返回空列表")
    void getStreetReturnsEmptyForUnknownParent() {
        List<RegionsDto> streets = service.getStreet("__not-exists__");

        assertThat(streets).isEmpty();
    }

    @Test
    @DisplayName("getRegions：未执行初始化时返回空集合")
    void getRegionsReturnsCachedList() {
        assertThat(service.getRegions()).isEmpty();
    }
}
