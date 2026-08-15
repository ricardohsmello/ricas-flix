package br.com.ricas.web;

import br.com.ricas.movie.MovieSearchResult;
import br.com.ricas.movie.MovieSearchService;
import br.com.ricas.web.dto.MovieSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RicasFlixControllerTests {

	@Test
	void acceptsRequestContainingOnlyQuery() throws Exception {
		MovieSearchService service = new MovieSearchService(null, null, null) {
			@Override
			public List<MovieSearchResult> searchMovies(MovieSearchRequest request) {
				return List.of();
			}
		};
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RicasFlixController(service)).build();

		mockMvc.perform(post("/api/movies/search")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"query\":\"action\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void acceptsRequestContainingOnlyYear() throws Exception {
		MovieSearchService service = new MovieSearchService(null, null, null) {
			@Override
			public List<MovieSearchResult> searchMovies(MovieSearchRequest request) {
				return List.of();
			}
		};
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RicasFlixController(service)).build();

		mockMvc.perform(post("/api/movies/search")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"yearFrom\":2020,\"yearTo\":2020}"))
				.andExpect(status().isOk());
	}
}
