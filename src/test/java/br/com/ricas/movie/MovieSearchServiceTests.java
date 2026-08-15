package br.com.ricas.movie;

import br.com.ricas.movie.config.MovieSearchProperties;
import br.com.ricas.web.dto.MovieSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MovieSearchServiceTests {

	private MovieSearchService service;

	@BeforeEach
	void setUp() {
		MovieSearchProperties properties = new MovieSearchProperties(
				"embedded_movies",
				"vector_index",
				"search_index",
				"plot_embedding_voyage_3_large",
				20,
				300,
				0.7,
				0.3,
				new MovieSearchProperties.Voyage("https://api.voyageai.com/v1", "", "voyage-3-large", 2048)
		);
		service = new MovieSearchService(null, query -> List.of(0.1, 0.2), properties);
	}

	@Test
	void buildsRankFusionWithVectorAndFullTextPipelines() {
		MovieSearchRequest request = new MovieSearchRequest(
				"space travel", 2010, 2020, List.of("Sci-Fi", "Drama"), 7.5, false
		);

		String json = service.buildHybridAggregation(request, List.of(0.1, 0.2)).toString();

		assertThat(json)
				.contains("$rankFusion")
				.contains("$vectorSearch")
				.contains("$search")
				.contains("plot_embedding_voyage_3_large")
				.contains("space travel")
				.contains("Sci-Fi")
				.contains("imdb.rating");
	}

	@Test
	void appliesGenreExclusionToBothSearchPipelines() {
		MovieSearchRequest request = new MovieSearchRequest(
				"light comedy", null, null, List.of("Horror"), null, true
		);

		String json = service.buildHybridAggregation(request, List.of(0.1, 0.2)).toString();

		assertThat(json)
				.contains("$nin")
				.contains("mustNot")
				.contains("Horror");
	}

	@Test
	void searchesWithOnlyTheQueryWithoutAddingOptionalFilters() {
		MovieSearchRequest request = new MovieSearchRequest(
				"ship movie", null, null, null, null, false
		);

		String json = service.buildHybridAggregation(request, List.of(0.1, 0.2)).toString();

		assertThat(request.genres()).isEmpty();
		assertThat(json)
				.contains("ship movie")
				.contains("$vectorSearch")
				.contains("$search")
				.doesNotContain("\"filter\"")
				.doesNotContain("mustNot");
	}
}
