# Ricas Flix

Ricas Flix is a Java movie search application that combines full-text and semantic search to understand both exact titles and natural-language descriptions.

The project is live at **[ricardohsmello.com/ricas-flix](https://www.ricardohsmello.com/ricas-flix)**.

## How it works

The application uses MongoDB Atlas Hybrid Search to combine two complementary search strategies:

- **Full-text search** with Atlas Search for titles, plots, genres, fuzzy matches, and exact keywords.
- **Semantic search** with Atlas Vector Search and Voyage AI embeddings to find movies by meaning, even when the query does not contain the same words as the movie plot.
- **Result fusion** with MongoDB `$rankFusion`, which merges and ranks the results returned by both pipelines.

For example, a title query such as `Titanic` benefits from full-text search, while a description such as `a ship that sinks after hitting an iceberg` benefits from vector search.

To learn more about the architecture and the ideas behind this project, read:

**[Beyond Keywords: Hybrid Search With Atlas and Vector Search (Part 3)](https://foojay.io/today/beyond-keywords-hybrid-search-with-atlas-and-vector-search-part-3/)**

## Tech stack

- Java 25
- Spring Boot 4.1
- Spring Web 
- Spring Data MongoDB
- MongoDB Atlas Search
- Voyage AI `voyage-3-large` embeddings with 2,048 dimensions
- Maven

## Requirements

- Java 25
- Maven, or the Maven Wrapper if added to the project
- MongoDB Atlas with support for `$rankFusion`
- `mongosh`, `curl`, and `jq` to generate the movie embeddings
- A Voyage AI API key

## MongoDB indexes

Before running hybrid search, create both Atlas indexes for the `movies` collection:

1. A Search index using [`src/main/resources/mongodb/search-index.json`](src/main/resources/mongodb/search-index.json).
2. A Vector Search index named `vector_index` using [`src/main/resources/mongodb/vector-index.json`](src/main/resources/mongodb/vector-index.json).

The vector index expects the following field:

```text
plot_embedding_voyage_3_large
```

with 2,048 dimensions and `dotProduct` similarity.

Wait until both indexes have a ready/active status in MongoDB Atlas before testing the application.

## Generating movie embeddings

Configure the MongoDB connection and Voyage AI credentials:

```shell
export MONGODB_URI='mongodb+srv://...'
export VOYAGE_API_KEY='...'
```

Then run:

```shell
./scripts/embed-movies.sh
```

The loader processes movies in batches and stores the generated vector in `plot_embedding_voyage_3_large`. It is incremental: if execution is interrupted, running it again skips documents that already contain an embedding with the expected 2,048 dimensions.

## Running locally

Configure the required environment variables:

```shell
export MONGODB_URI='mongodb+srv://...'
export VOYAGE_API_KEY='...'
```

Start the application:

```shell
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

## Search API

Send a request to `POST /api/movies/search`:

```shell
curl --request POST 'http://localhost:8080/api/movies/search' \
  --header 'Content-Type: application/json' \
  --data '{
    "query": "a ship that sinks after hitting an iceberg"
  }'
```

Optional filters include release year, genres, minimum IMDb rating, and genre exclusion:

```json
{
  "query": "space travel and survival",
  "yearFrom": 2000,
  "yearTo": 2025,
  "genres": ["Sci-Fi", "Drama"],
  "minIMDbRating": 7.0,
  "excludeGenres": false
}
```

## Configuration

The main configuration options can be overridden with environment variables:

| Variable | Default |
| --- | --- |
| `MONGODB_DATABASE` | `ricas_flix` |
| `MOVIE_COLLECTION` | `movies` |
| `MOVIE_VECTOR_INDEX` | `vector_index` |
| `MOVIE_TEXT_INDEX` | `default` |
| `MOVIE_EMBEDDING_PATH` | `plot_embedding_voyage_3_large` |
| `VOYAGE_MODEL` | `voyage-3-large` |
| `VOYAGE_OUTPUT_DIMENSION` | `2048` |
| `MOVIE_SEARCH_LIMIT` | `5` |
| `MOVIE_SEARCH_NUM_CANDIDATES` | `300` |
| `MOVIE_VECTOR_WEIGHT` | `0.7` |
| `MOVIE_TEXT_WEIGHT` | `0.3` |

## Tests

Run the automated tests with:

```shell
mvn test
```

## License

This project is licensed under the terms provided in [`LICENSE`](LICENSE).
