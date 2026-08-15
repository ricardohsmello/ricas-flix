package br.com.ricas.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MovieSearchRequest(
		@NotBlank @Size(max = 1_000) String query,
		@Min(1888) Integer yearFrom,
		@Min(1888) Integer yearTo,
		@Size(max = 20) List<@NotBlank @Size(max = 80) String> genres,
		@DecimalMin("0.0") @DecimalMax("10.0") Double minIMDbRating,
		Boolean excludeGenres
) {

	public MovieSearchRequest {
		query = query == null ? null : query.strip();
		genres = genres == null
				? List.of()
				: genres.stream().map(String::strip).filter(genre -> !genre.isEmpty()).distinct().toList();
		excludeGenres = Boolean.TRUE.equals(excludeGenres);

		if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
			throw new IllegalArgumentException("yearFrom must be less than or equal to yearTo");
		}
	}
}
