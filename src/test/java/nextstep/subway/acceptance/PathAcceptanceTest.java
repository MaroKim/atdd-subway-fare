package nextstep.subway.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static nextstep.subway.acceptance.AcceptanceTestSteps.given;
import static nextstep.subway.acceptance.LineSteps.지하철_노선에_지하철_구간_생성_요청;
import static nextstep.subway.acceptance.StationSteps.지하철역_생성_요청;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 경로 검색")
class PathAcceptanceTest extends AcceptanceTest {
    private Long 교대역;
    private Long 강남역;
    private Long 양재역;
    private Long 남부터미널역;
    private Long 이호선;
    private Long 신분당선;
    private Long 삼호선;
    private String 거리기반 = "DISTANCE";
    private String 시간기반 = "DURATION";

    /**
     * 교대역    --- *2호선* ---   강남역
     * |                        |
     * *3호선*                   *신분당선*
     * |                        |
     * 남부터미널역  --- *3호선* ---   양재
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        교대역 = 지하철역_생성_요청(관리자, "교대역").jsonPath().getLong("id");
        강남역 = 지하철역_생성_요청(관리자, "강남역").jsonPath().getLong("id");
        양재역 = 지하철역_생성_요청(관리자, "양재역").jsonPath().getLong("id");
        남부터미널역 = 지하철역_생성_요청(관리자, "남부터미널역").jsonPath().getLong("id");

        이호선 = 지하철_노선_생성_요청("2호선", "green", 교대역, 강남역, 10, 5, 100);
        신분당선 = 지하철_노선_생성_요청("신분당선", "red", 강남역, 양재역, 10, 4, 500);
        삼호선 = 지하철_노선_생성_요청("3호선", "orange", 교대역, 남부터미널역, 2, 6, 200);

        지하철_노선에_지하철_구간_생성_요청(관리자, 삼호선, createSectionCreateParams(남부터미널역, 양재역, 10, 6));
    }

    /**
     * When : 두 역에 대해 거리를 기반으로 조회를 요청하면
     * Then : 거리가 가까운 순으로 조회가 된다.
     */

    @DisplayName("두 역의 최단 거리 경로를 조회한다.")
    @Test
    void findPathByDistance() {
        // when
        ExtractableResponse<Response> response = 비회원유저로_두_역의_경로_조회를_요청(교대역, 양재역, 거리기반);

        // then
        경로조회_방법에_따라_지나가는_역을_확인할수_있다(response, 교대역, 남부터미널역, 양재역);
    }



    /**
     * When : 두 역에 대해 거리를 기반으로 조회를 요청하면
     * Then : 거리가 가까운 순으로 조회가 되고 노선 추가 요금이 더해진다.
     */

    @DisplayName("두 역의 최단 거리 경로를 조회하고 요금을 확인한다")
    @Test
    void findPathByDistanceAndFare() {
        // when
        ExtractableResponse<Response> response = 비회원유저로_두_역의_경로_조회를_요청(교대역, 양재역, 거리기반);

        // then
        경로조회_방법에_따라_지나가는_역을_확인할수_있다(response, 교대역, 남부터미널역, 양재역);
        최단거리에_대한_요금을_확인할수_있다(response, 1550);

    }

    /**
     * When : 두 역에 대해 걸리는 시간을 기반으로 조회를 요청하면
     * Then : 시간이 적게 걸리는 순으로 조회가 된다.
     */

    @DisplayName("두 역의 최단 거리 시간을 조회한다.")
    @Test
    void findPathByDuration() {
        // when
        ExtractableResponse<Response> response = 비회원유저로_두_역의_경로_조회를_요청(교대역, 양재역, 시간기반);

        // then
        경로조회_방법에_따라_지나가는_역을_확인할수_있다(response, 교대역, 강남역, 양재역);
    }

    /**
     * When : 두 역에 대해 걸리는 시간을 기반으로 조회를 요청하면
     * Then : 시간이 적게 걸리는 순으로 조회가 되고 요금은 최단거리 기준으로 계산되고 노선 추가
     * 요금이 더해진다.
     */

    @DisplayName("두 역의 최단 거리 시간을 조회하고 요금은 최단거리 기준으로 계산된다")
    @Test
    void findPathByDurationAndFare() {
        // when
        ExtractableResponse<Response> response = 비회원유저로_두_역의_경로_조회를_요청(교대역, 양재역, 시간기반);

        // then
        경로조회_방법에_따라_지나가는_역을_확인할수_있다(response, 교대역, 강남역, 양재역);
        최단거리에_대한_요금을_확인할수_있다(response, 1850);
    }

    /**
     * Given : 청소년으로 로그인 했을때
     * When : 두 역에 대해 걸리는 시간을 기반으로 조회를 요청하면
     * Then : 시간이 적게 걸리는 순으로 조회가 되고 요금은
     *        최단거리 기준으로 청소년 할인이 적용된다.
     */
    @DisplayName("두 역의 최단 거리 시간을 조회하고 요금은 최단거리 기준으로 청소년 할인이 적용된다")
    @Test
    void findPathByDurationAndYouthDiscountFare() {
        // when
        ExtractableResponse<Response> response = 로그인유저로_두_역의_경로_조회를_요청(청소년, 교대역, 양재역, 시간기반);

        // then
        경로조회_방법에_따라_지나가는_역을_확인할수_있다(response, 교대역, 강남역, 양재역);
        최단거리에_대한_요금을_확인할수_있다(response, 1200);
    }

    /**
     * Given : 어린이로 로그인 했을때
     * When : 두 역에 대해 걸리는 시간을 기반으로 조회를 요청하면
     * Then : 시간이 적게 걸리는 순으로 조회가 되고 요금은
     *        최단거리 기준으로 어린이 할인이 적용된다.
     */
    @DisplayName("두 역의 최단 거리 시간을 조회하고 요금은 최단거리 기준으로 어린이 할인이 적용된다")
    @Test
    void findPathByDurationAndChildDiscountFare() {
        // when
        ExtractableResponse<Response> response = 로그인유저로_두_역의_경로_조회를_요청(어린이, 교대역, 양재역, 시간기반);

        // then
        경로조회_방법에_따라_지나가는_역을_확인할수_있다(response, 교대역, 강남역, 양재역);
        최단거리에_대한_요금을_확인할수_있다(response, 750);
    }

    private ExtractableResponse<Response> 비회원유저로_두_역의_경로_조회를_요청(Long source, Long target, String type) {
        return RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/paths?source={sourceId}&target={targetId}&type={type}", source, target, type)
                .then().log().all().extract();
    }

    private ExtractableResponse<Response> 로그인유저로_두_역의_경로_조회를_요청(String accessToken, Long source,
            Long target, String type) {
        return given(accessToken)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/paths?source={sourceId}&target={targetId}&type={type}", source, target, type)
                .then().log().all().extract();
    }

    public void 경로조회_방법에_따라_지나가는_역을_확인할수_있다(ExtractableResponse<Response> response, Long ...stationId){
        assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(stationId);
    }
    public void 최단거리에_대한_요금을_확인할수_있다(ExtractableResponse<Response> response, int fare){
        assertThat(response.jsonPath().getLong("fare")).isEqualTo(fare);
    }


    private Long 지하철_노선_생성_요청(String name, String color, Long upStation, Long downStation,
            int distance, int duration, int extraCharge) {
        Map<String, String> lineCreateParams;
        lineCreateParams = new HashMap<>();
        lineCreateParams.put("name", name);
        lineCreateParams.put("color", color);
        lineCreateParams.put("upStationId", upStation + "");
        lineCreateParams.put("downStationId", downStation + "");
        lineCreateParams.put("distance", distance + "");
        lineCreateParams.put("duration", duration + "");
        lineCreateParams.put("extraCharge", extraCharge + "");

        return LineSteps.지하철_노선_생성_요청(관리자, lineCreateParams).jsonPath().getLong("id");
    }

    private Map<String, String> createSectionCreateParams(Long upStationId, Long downStationId, int distance, int duration) {
        Map<String, String> params = new HashMap<>();
        params.put("upStationId", upStationId + "");
        params.put("downStationId", downStationId + "");
        params.put("distance", distance + "");
        params.put("duration", duration + "");
        return params;
    }
}

