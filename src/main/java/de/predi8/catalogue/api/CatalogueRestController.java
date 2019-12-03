package de.predi8.catalogue.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.predi8.catalogue.error.NotFoundException;
import de.predi8.catalogue.event.Operation;
import de.predi8.catalogue.model.Article;
import de.predi8.catalogue.repository.ArticleRepository;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/articles")
public class CatalogueRestController {

	public static final String PRICE = "price";
	public static final String NAME = "name";

	private ArticleRepository repo;
	private KafkaTemplate<String, Operation> kafka;
	final private ObjectMapper mapper;

	public CatalogueRestController(ArticleRepository repo, KafkaTemplate<String, Operation> kafka, ObjectMapper mapper) {
		this.repo = repo;
		this.kafka = kafka;
		this.mapper = mapper;
	}

	@GetMapping
	public List<Article> index() {
		return repo.findAll();
	}

	@GetMapping("/count")
	public long count() {
		return repo.count();
	}

	@GetMapping("/{id}")
	public Article get(@PathVariable String id) {
		return repo.findById(id).orElseThrow(NotFoundException::new);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity delete(@PathVariable String id) {
		Article article = get(id);
		repo.delete(article);
		return ResponseEntity.accepted().build();
	}

	@PutMapping("/{id}")
	public ResponseEntity<Article> change(@PathVariable String id,
			@RequestBody Article article,
			UriComponentsBuilder builder) {
		get(id);
		article.setUuid(id);

		return ResponseEntity
				.created
						(builder
								.path("/articles/" + id)
								.build().toUri()).body(repo.save(article));
	}

	@PostMapping
	public ResponseEntity<Article> post(@RequestBody Article article,
			UriComponentsBuilder builder) {
		String uuid = UUID.randomUUID().toString();
		article.setUuid(uuid);

		System.out.println("article = " + article);

		Article savedArticle = repo.save(article);
		return ResponseEntity
				.created
						(builder
								.path("/articles/" + uuid)
								.build().toUri()).body(savedArticle);
	}

	@PatchMapping("/{id}")
	public ResponseEntity patch(@PathVariable String id,
			@RequestBody JsonNode json,
			UriComponentsBuilder builder) {
		Article old = get(id);

		//BeanUtils.copyProperties(json,old, "uuid");//

		if(json.has(PRICE)) {
			if(json.hasNonNull(PRICE)) {
				old.setPrice(new BigDecimal(json.get(PRICE).asDouble()));
			}
		}

		if(json.has(NAME)) {
			old.setName(json.get(NAME).asText());
		}

		System.out.println("old = " + old + " json " + json);

		return ResponseEntity.created(builder
				.path("/articles/" + old.getUuid())
				.build().toUri()).body(repo.save(old));
	}
}