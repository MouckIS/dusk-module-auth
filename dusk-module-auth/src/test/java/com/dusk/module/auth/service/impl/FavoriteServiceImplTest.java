package com.dusk.module.auth.service.impl;

import com.dusk.common.core.dto.*;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.module.auth.service.ICommonFavoriteRpcService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FavoriteServiceImpl} 单元测试。
 *
 * <p>泛型收藏 DTO 使用本类内定义的 {@link TestFavoriteDto}（父类泛型参数为 {@link TestContent}），
 * 以便 {@code getDeSerializeContent} 能通过 {@code ParameterizedType} 解析出内容类型。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FavoriteServiceImplTest {

    @Mock
    private ICommonFavoriteRpcService commonFavoriteRpcService;
    @Mock
    private ObjectMapper jackson;

    private FavoriteServiceImpl<TestFavoriteDto> service;

    private static TestFavoriteDto favorite(String name, boolean isPublic) {
        TestFavoriteDto dto = new TestFavoriteDto();
        dto.setName(name);
        dto.setContentData(new TestContent("payload"));
        dto.setIsPublic(isPublic);
        return dto;
    }

    @BeforeEach
    void setUp() {
        service = new FavoriteServiceImpl<>();
        ReflectionTestUtils.setField(service, "iCommonFavoriteRpcService", commonFavoriteRpcService);
        ReflectionTestUtils.setField(service, "jackson", jackson);
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    @DisplayName("save：isPublic 为空时默认为公开，并回填 id 与 isMine")
    void saveDefaultsPublicAndCopiesBack() throws Exception {
        when(jackson.writeValueAsString(any())).thenReturn("{\"text\":\"payload\"}");
        CommonFavoriteDto saved = new CommonFavoriteDto();
        saved.setId(5L);
        when(commonFavoriteRpcService.save(any(CommonFavoriteDto.class))).thenReturn(saved);

        TestFavoriteDto dto = favorite("我的收藏", false);
        dto.setIsPublic(null);

        TestFavoriteDto result = service.save(dto);

        assertThat(result.getIsPublic()).isTrue();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getIsMine()).isTrue();
        ArgumentCaptor<CommonFavoriteDto> captor = ArgumentCaptor.forClass(CommonFavoriteDto.class);
        verify(commonFavoriteRpcService).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("{\"text\":\"payload\"}");
        assertThat(captor.getValue().getName()).isEqualTo("我的收藏");
    }

    @Test
    @DisplayName("save：显式 isPublic=false 时保持原值")
    void saveKeepsExplicitPublicFlag() throws Exception {
        when(jackson.writeValueAsString(any())).thenReturn("{}");
        CommonFavoriteDto saved = new CommonFavoriteDto();
        saved.setId(6L);
        when(commonFavoriteRpcService.save(any(CommonFavoriteDto.class))).thenReturn(saved);

        TestFavoriteDto result = service.save(favorite("私有收藏", false));

        assertThat(result.getIsPublic()).isFalse();
        assertThat(result.getId()).isEqualTo(6L);
    }

    @Test
    @DisplayName("save：内容序列化失败时抛出业务异常")
    void saveThrowsWhenSerializationFails() throws Exception {
        when(jackson.writeValueAsString(any())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.save(favorite("坏数据", true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("序列化收藏内容失败");
    }

    // ------------------------------------------------------------------
    // delete / setPublic
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleteById：透传 id 与类型")
    void deleteByIdDelegates() {
        service.deleteById(7L, "role");

        verify(commonFavoriteRpcService).deleteById(7L, "role");
    }

    @Test
    @DisplayName("setPublic：透传 id 与公开标记")
    void setPublicDelegates() {
        service.setPublic(8L, true);

        verify(commonFavoriteRpcService).setPublic(8L, true);
    }

    // ------------------------------------------------------------------
    // get / list
    // ------------------------------------------------------------------

    @Test
    @DisplayName("get：反序列化内容并复制收藏属性")
    void getCopiesAndDeserializes() throws Exception {
        CommonFavoriteDto favoriteDto = new CommonFavoriteDto();
        favoriteDto.setId(9L);
        favoriteDto.setName("收藏A");
        favoriteDto.setContent("{\"text\":\"payload\"}");
        when(commonFavoriteRpcService.get(9L, "role")).thenReturn(favoriteDto);
        when(jackson.readValue(anyString(), eq(TestContent.class)))
                .thenReturn(new TestContent("payload"));

        TestFavoriteDto result = service.get(9L, "role", TestFavoriteDto.class);

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getName()).isEqualTo("收藏A");
        assertThat(result.getContentData()).isInstanceOf(TestContent.class);
        assertThat(((TestContent) result.getContentData()).getText()).isEqualTo("payload");
    }

    @Test
    @DisplayName("get：内容反序列化失败时抛出业务异常")
    void getThrowsWhenDeserializationFails() throws Exception {
        CommonFavoriteDto favoriteDto = new CommonFavoriteDto();
        favoriteDto.setContent("bad");
        when(commonFavoriteRpcService.get(1L, "role")).thenReturn(favoriteDto);
        when(jackson.readValue(anyString(), eq(TestContent.class)))
                .thenThrow(new RuntimeException("bad json"));

        assertThatThrownBy(() -> service.get(1L, "role", TestFavoriteDto.class))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("反序列化收藏内容失败");
    }

    @Test
    @DisplayName("list：以不分页参数查询并逐项反序列化")
    void listMapsItems() throws Exception {
        CommonFavoriteDto favoriteDto = new CommonFavoriteDto();
        favoriteDto.setId(2L);
        favoriteDto.setName("收藏B");
        favoriteDto.setContent("{\"text\":\"payload\"}");
        when(commonFavoriteRpcService.list(eq(true), any(PagedAndSortedInputDto.class), eq("role")))
                .thenReturn(new PagedResultDto<>(1, List.of(favoriteDto)));
        when(jackson.readValue(anyString(), eq(TestContent.class)))
                .thenReturn(new TestContent("payload"));

        List<TestFavoriteDto> result = service.list("role", true, TestFavoriteDto.class);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(2L);
        assertThat(result.getFirst().getContentData()).isInstanceOf(TestContent.class);
        ArgumentCaptor<PagedAndSortedInputDto> captor = ArgumentCaptor.forClass(PagedAndSortedInputDto.class);
        verify(commonFavoriteRpcService).list(anyBoolean(), captor.capture(), eq("role"));
        assertThat(captor.getValue().isUnPage()).isTrue();
    }

    @Test
    @DisplayName("list：空结果返回空集合")
    void listReturnsEmptyWhenNoItems() {
        when(commonFavoriteRpcService.list(anyBoolean(), any(PagedAndSortedInputDto.class), anyString()))
                .thenReturn(new PagedResultDto<>(0, new ArrayList<>()));

        assertThat(service.list("role", false, TestFavoriteDto.class)).isEmpty();
    }

    // ------------------------------------------------------------------
    // 测试用泛型载体
    // ------------------------------------------------------------------

    static class TestFavoriteDto extends AbstractBaseFavoriteDto<TestContent> {
    }

    static class TestContent extends AbstractFavoriteContent {
        private String text;

        TestContent() {
        }

        TestContent(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
