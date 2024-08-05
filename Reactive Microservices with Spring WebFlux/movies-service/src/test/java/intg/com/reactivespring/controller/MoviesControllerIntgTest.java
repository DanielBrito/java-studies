package com.reactivespring.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reactivespring.domain.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebClient
@AutoConfigureWireMock(port = 8084) // automatically spins up a httpserver in port 8084
@TestPropertySource(properties = {
        "restClient.moviesInfoUrl=http://localhost:8084/v1/movieinfos",
        "restClient.reviewsUrl=http://localhost:8084/v1/reviews",
})
public class MoviesControllerIntgTest {

    @Autowired
    WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        WireMock.reset();
    }

    @Test
    void retrieveMovieById() {
        var movieId = "abc";
        stubFor(get(urlEqualTo("/v1/movieinfos/" + movieId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("movieinfo.json")));

        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .withQueryParam("movieInfoId", equalTo(movieId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("reviews.json")));

        webTestClient.get()
                .uri("/v1/movies/{id}", "abc")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Movie.class)
                .consumeWith(movieEntityExchangeResult -> {
                            var movie = movieEntityExchangeResult.getResponseBody();
                            assert Objects.requireNonNull(movie).getReviewList().size() == 2;
                            assertEquals("Batman Begins", movie.getMovieInfo().getName());
                        }
                );
    }

    @Test
    void retrieveMovieById_404() {
        var movieId = "abc";

        stubFor(get(urlEqualTo("/v1/movieinfos/" + movieId))
                .willReturn(aResponse()
                        .withStatus(404)));
        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .withQueryParam("movieInfoId", equalTo(movieId))
                .willReturn(aResponse()
                        .withStatus(404)));

        webTestClient.get()
                .uri("/v1/movies/{id}", "abc")
                .exchange()
                .expectStatus().is4xxClientError();

//        WireMock.verify(4, getRequestedFor(urlEqualTo("/v1/movieinfos/" + movieId)));;
    }


    @Test
    void retrieveMovieById_Reviews_404() {
        var movieId = "abc";

        stubFor(get(urlEqualTo("/v1/movieinfos/" + movieId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("movieinfo.json")));
        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .withQueryParam("movieInfoId", equalTo(movieId))
                .willReturn(aResponse()
                        .withStatus(404)));

        webTestClient.get()
                .uri("/v1/movies/{id}", "abc")
                .exchange()
                .expectStatus().is2xxSuccessful();

//         WireMock.verify(4, getRequestedFor(urlEqualTo("/v1/movieinfos/" + movieId)));;
    }

    @Test
    void retrieveMovieById_5XX() {
        var movieId = "abc";

        stubFor(get(urlEqualTo("/v1/movieinfos/" + movieId))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("MovieInfo Service Unavailable")));

//        stubFor(get(urlPathEqualTo("/v1/reviews"))
//                .withQueryParam("movieInfoId", equalTo(movieId))
//                .willReturn(aResponse()
//                        .withStatus(500)));

        webTestClient.get()
                .uri("/v1/movies/{id}", "abc")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .value(message -> {
                    assertEquals("MovieInfo Service Unavailable", message);
                });

        WireMock.verify(4, getRequestedFor(urlEqualTo("/v1/movieinfos/" + movieId)));
    }

    @Test
    void retrieveMovieById_reviews_5XX() {
        var movieId = "abc";

        stubFor(get(urlEqualTo("/v1/movieinfos/" + movieId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("movieinfo.json")));
        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .withQueryParam("movieInfoId", equalTo(movieId))
                .willReturn(aResponse()
                        .withStatus(500)
                .withBody("Review Service Unavailable")));

        webTestClient.get()
                .uri("/v1/movies/{id}", "abc")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .value(message -> {
                    assertEquals("Review Service Unavailable", message);
                });

        WireMock.verify(4, getRequestedFor(urlPathMatching("/v1/reviews*")));
    }
}
