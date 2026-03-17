package com.example.demo.google;

import com.example.demo.db.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GoogleBookService {
    private final RestClient restClient;

    public GoogleBookService(@Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public GoogleBook searchBooks(String query, Integer maxResults, Integer startIndex) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/volumes")
                        .queryParam("q", query)
                        .queryParam("maxResults", maxResults != null ? maxResults : 10)
                        .queryParam("startIndex", startIndex != null ? startIndex : 0)
                        .build())
                .retrieve()
                .body(GoogleBook.class);
    }

    public GoogleVolume getVolume(String id) {
        return restClient.get()
                .uri("/volumes/{id}", id)
                .retrieve()
                .body(GoogleVolume.class);

    }



    public Book mapBook(GoogleVolume volume) {
        var info = volume.volumeInfo();
        String author = info.authors() != null && !info.authors().isEmpty() ? info.authors().get(0) : null;
        return new Book(volume.id(), info.title(), author, info.pageCount());
    }
}
