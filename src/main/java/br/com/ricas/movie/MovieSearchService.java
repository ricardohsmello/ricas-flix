package br.com.ricas.movie;

import br.com.ricas.movie.config.MovieSearchProperties;
import br.com.ricas.web.dto.MovieSearchRequest;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MovieSearchService {

	private static final Logger log = LoggerFactory.getLogger(MovieSearchService.class);

	private final MongoTemplate mongoTemplate;
	private final EmbeddingClient embeddingClient;
	private final MovieSearchProperties properties;

	public MovieSearchService(
			MongoTemplate mongoTemplate,
			EmbeddingClient embeddingClient,
			MovieSearchProperties properties
	) {
		this.mongoTemplate = mongoTemplate;
		this.embeddingClient = embeddingClient;
		this.properties = properties;
	}

	public List<MovieSearchResult> searchMovies(MovieSearchRequest request) {
		long startedAt = System.nanoTime();
		log.info(
				"Starting hybrid movie search: database={}, collection={}, vectorIndex={}, textIndex={}, query=\"{}\", "
						+ "yearFrom={}, yearTo={}, genres={}, minIMDbRating={}, excludeGenres={}",
				mongoTemplate.getDb().getName(),
				properties.collection(),
				properties.vectorIndex(),
				properties.textIndex(),
				request.query().replaceAll("\\s+", " "),
				request.yearFrom(),
				request.yearTo(),
				request.genres(),
				request.minIMDbRating(),
				request.excludeGenres()
		);

		List<Double> queryVector = embeddingClient.embedQuery(request.query());
		Aggregation aggregation = buildHybridAggregation(request, queryVector);

		log.info("MongoDB pipeline: {}", aggregation.toString().replace(queryVector.toString(),
				"<vector with " + queryVector.size() + " dimensions>"));

		try {
			List<MovieSearchResult> results = mongoTemplate
					.aggregate(aggregation, properties.collection(), MovieSearchResult.class)
					.getMappedResults();
			long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
			log.info("Hybrid movie search completed: results={}, durationMs={}", results.size(), elapsedMs);
			return results;
		} catch (RuntimeException exception) {
			long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
			log.error("Hybrid movie search failed after {} ms: {}", elapsedMs, exception.getMessage(), exception);
			throw exception;
		}
	}

	Aggregation buildHybridAggregation(MovieSearchRequest request, List<Double> queryVector) {
		AggregationOperation rankFusion = context -> new Document("$rankFusion",
				new Document("input", new Document("pipelines", new Document()
						.append("fullText", buildFullTextSearchPipeline(request))
						.append("vector", buildVectorSearchPipeline(request, queryVector))))
						.append("combination", new Document("weights", new Document()
								.append("fullText", properties.textWeight())
								.append("vector", properties.vectorWeight())))
						.append("scoreDetails", false));

		return Aggregation.newAggregation(
				rankFusion,
				context -> buildProjectionStage(),
				context -> new Document("$limit", properties.limit())
		);
	}

	private List<Document> buildVectorSearchPipeline(MovieSearchRequest request, List<Double> queryVector) {
		Document vectorSearch = new Document("index", properties.vectorIndex())
				.append("path", properties.embeddingPath())
				.append("queryVector", queryVector)
				.append("numCandidates", properties.numCandidates())
				.append("limit", properties.limit());

		Document vectorFilter = buildVectorFilter(request);
		if (!vectorFilter.isEmpty()) {
			vectorSearch.append("filter", vectorFilter);
		}

		return List.of(new Document("$vectorSearch", vectorSearch));
	}

	private List<Document> buildFullTextSearchPipeline(MovieSearchRequest request) {
		Document searchCompound = new Document("must", List.of(
				new Document("text", new Document("query", request.query())
						.append("path", List.of("title", "plot", "fullplot", "genres"))
						.append("fuzzy", new Document("maxEdits", 1)))
		));
		applySearchFilters(searchCompound, request);

		return List.of(
				new Document("$search", new Document("index", properties.textIndex())
						.append("compound", searchCompound)),
				new Document("$limit", properties.limit())
		);
	}

	private Document buildProjectionStage() {
		return new Document("$project", new Document("_id", 0)
						.append("id", new Document("$toString", "$_id"))
						.append("title", 1)
						.append("year", 1)
						.append("genres", 1)
						.append("plot", 1)
						.append("cast", 1)
						.append("poster", 1)
						.append("imdbRating", "$imdb.rating")
						.append("score", new Document("$meta", "score")));
	}

	private Document buildVectorFilter(MovieSearchRequest request) {
		List<Document> filters = new ArrayList<>();
		addRangeFilter(filters, "year", request.yearFrom(), request.yearTo());
		if (request.minIMDbRating() != null) {
			filters.add(new Document("imdb.rating", new Document("$gte", request.minIMDbRating())));
		}
		if (!request.genres().isEmpty()) {
			filters.add(new Document("genres", new Document(request.excludeGenres() ? "$nin" : "$in", request.genres())));
		}
		return filters.isEmpty() ? new Document() : new Document("$and", filters);
	}

	private void applySearchFilters(Document compound, MovieSearchRequest request) {
		List<Document> filters = new ArrayList<>();
		if (request.yearFrom() != null || request.yearTo() != null) {
			Document range = new Document("path", "year");
			if (request.yearFrom() != null) range.append("gte", request.yearFrom());
			if (request.yearTo() != null) range.append("lte", request.yearTo());
			filters.add(new Document("range", range));
		}
		if (request.minIMDbRating() != null) {
			filters.add(new Document("range", new Document("path", "imdb.rating")
					.append("gte", request.minIMDbRating())));
		}
		if (!request.genres().isEmpty()) {
			Document genreFilter = new Document("text", new Document("path", "genres")
					.append("query", request.genres()));
			if (request.excludeGenres()) {
				compound.append("mustNot", List.of(genreFilter));
			} else {
				filters.add(genreFilter);
			}
		}
		if (!filters.isEmpty()) compound.append("filter", filters);
	}

	private void addRangeFilter(List<Document> filters, String field, Integer from, Integer to) {
		if (from == null && to == null) return;
		Document range = new Document();
		if (from != null) range.append("$gte", from);
		if (to != null) range.append("$lte", to);
		filters.add(new Document(field, range));
	}

}
