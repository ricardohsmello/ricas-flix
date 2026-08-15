package br.com.ricas.movie;

import java.util.List;

public interface EmbeddingClient {

	List<Double> embedQuery(String query);
}
