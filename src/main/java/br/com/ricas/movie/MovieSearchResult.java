package br.com.ricas.movie;

import java.util.List;

public record MovieSearchResult(
		String id,
		String title,
		Integer year,
		List<String> genres,
		Double imdbRating,
		String plot,
		List<String> cast,
		String poster,
		Double score
) {}
