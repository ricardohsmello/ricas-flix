package br.com.ricas.web;

import br.com.ricas.movie.MovieSearchResult;
import br.com.ricas.movie.MovieSearchService;
import br.com.ricas.web.dto.MovieSearchRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RicasFlixController {

	private final MovieSearchService movieSearchService;

	public RicasFlixController(MovieSearchService movieSearchService) {
		this.movieSearchService = movieSearchService;
	}

	@PostMapping("/movies/search")
	public List<MovieSearchResult> search(@Valid @RequestBody MovieSearchRequest request) {
		return movieSearchService.searchMovies(request);
	}
}
