package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import com.example.demo.google.GoogleVolume;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@RestController
public class BookController {
    private final BookRepository bookRepository;
    private final GoogleBookService googleBookService;

    @Autowired
    public BookController(BookRepository bookRepository, GoogleBookService googleBookService) {
        this.bookRepository = bookRepository;
        this.googleBookService = googleBookService;
    }

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }



    @GetMapping("/google")
    public GoogleBook searchGoogleBooks(@RequestParam("q") String query,
                                        @RequestParam(value = "maxResults", required = false) Integer maxResults,
                                        @RequestParam(value = "startIndex", required = false) Integer startIndex) {
        return googleBookService.searchBooks(query, maxResults, startIndex);
    }



    @PostMapping("/books/{googleId}")
    public ResponseEntity<Book> addBook(@PathVariable String googleId) {
        try {
            GoogleVolume volume = googleBookService.getVolume(googleId);
            if (volume == null || volume.volumeInfo() == null) {
                return ResponseEntity.badRequest().build();
            }
            Book book = googleBookService.mapBook(volume);
            bookRepository.save(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(book);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
