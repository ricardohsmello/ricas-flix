package br.com.ricas.movie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.movie-search")
public record MovieSearchProperties(
		String collection,
		String vectorIndex,
		String textIndex,
		String embeddingPath,
		int limit,
		int numCandidates,
		double vectorWeight,
		double textWeight,
		Voyage voyage
) {

	public record Voyage(String baseUrl, String apiKey, String model, int outputDimension) {
	}
}
