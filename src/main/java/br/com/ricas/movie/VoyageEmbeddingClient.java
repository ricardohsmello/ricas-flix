package br.com.ricas.movie;

import br.com.ricas.movie.config.MovieSearchProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class VoyageEmbeddingClient implements EmbeddingClient {

	private final RestClient restClient;
	private final MovieSearchProperties properties;

	public VoyageEmbeddingClient(MovieSearchProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.builder().baseUrl(properties.voyage().baseUrl()).build();
	}

	@Override
	public List<Double> embedQuery(String query) {
		if (!StringUtils.hasText(properties.voyage().apiKey())) {
			throw new IllegalStateException("Configure VOYAGE_API_KEY to run semantic search");
		}

		VoyageEmbeddingResponse response = restClient.post()
				.uri("/embeddings")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.voyage().apiKey())
				.body(new VoyageEmbeddingRequest(
						query,
						properties.voyage().model(),
						"query",
						properties.voyage().outputDimension()
				))
				.retrieve()
				.body(VoyageEmbeddingResponse.class);

		if (response == null || response.data() == null || response.data().isEmpty()) {
			throw new IllegalStateException("Voyage AI did not return an embedding");
		}
		List<Double> embedding = response.data().getFirst().embedding();
		if (embedding == null || embedding.size() != properties.voyage().outputDimension()) {
			throw new IllegalStateException(
					"Incompatible embedding dimension: expected %d, received %d"
							.formatted(properties.voyage().outputDimension(), embedding == null ? 0 : embedding.size())
			);
		}
		return embedding;
	}

	private record VoyageEmbeddingRequest(
			String input,
			String model,
			String input_type,
			int output_dimension
	) {
	}

	private record VoyageEmbeddingResponse(List<EmbeddingData> data) {
	}

	private record EmbeddingData(List<Double> embedding) {
	}
}
