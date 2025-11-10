package javacafe.realtime_sujeong.collection.rss.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javacafe.realtime_sujeong.collection.rss.controller.dto.RssCollectionRequest;
import javacafe.realtime_sujeong.collection.rss.service.RssCollectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RssCollectionController.class)
@DisplayName("RssCollectionController 테스트")
class RssCollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RssCollectionService rssCollectionService;

    @Test
    @DisplayName("POST /api/collection/rss/collect - 성공")
    void collectRssFeed_Success() throws Exception {
        // given
        RssCollectionRequest request = RssCollectionRequest.builder()
                .source("chosun")
                .build();

        RssCollectionService.CollectionResult mockResult =
                new RssCollectionService.CollectionResult(10, 8, 2);

        given(rssCollectionService.collectFeed(anyString()))
                .willReturn(mockResult);

        // when & then
        mockMvc.perform(post("/api/collection/rss/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCount").value(10))
                .andExpect(jsonPath("$.savedCount").value(8))
                .andExpect(jsonPath("$.duplicateCount").value(2));
    }



    @Test
    @DisplayName("POST /api/collection/rss/collect - Validation 실패 (source 없음)")
    void collectRssFeed_ValidationFail_NoSource() throws Exception {
        // given
        RssCollectionRequest request = RssCollectionRequest.builder()
                .build();

        // when & then
        mockMvc.perform(post("/api/collection/rss/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

@Test
    @DisplayName("GET /api/collection/rss/stats - 통계 조회")
    void getStats() throws Exception {
        // given
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("totalCount", 2L);
        mockStats.put("sources", List.of("chosun", "maeil"));
        mockStats.put("countBySource", Map.of("chosun", 1L, "maeil", 1L));

        given(rssCollectionService.getStats()).willReturn(mockStats);

        // when & then
        mockMvc.perform(get("/api/collection/rss/stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.sources").isArray());
    }

    @Test
    @DisplayName("GET /api/collection/rss/sources - 지원 소스 목록 조회")
    void getSupportedSources() throws Exception {
        // when & then
        mockMvc.perform(get("/api/collection/rss/sources"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("chosun"))
                .andExpect(jsonPath("$[1]").value("maeil"))
                .andExpect(jsonPath("$[2]").value("donga"));
    }

}
