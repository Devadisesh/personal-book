package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import com.example.demo.google.GoogleBookService;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;

import java.io.IOException;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "google.books.base-url=http://localhost:8081")
class BookControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GoogleBookService googleBookService;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8081);

        bookRepository.deleteAll();
        bookRepository.save(new Book("lRtdEAAAQBAJ", "Spring in Action", "Craig Walls"));
        bookRepository.save(new Book("12muzgEACAAJ", "Effective Java", "Joshua Bloch"));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetAllBooks() throws Exception {
        mockMvc.perform(get("/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Spring in Action"))
            .andExpect(jsonPath("$[1].title").value("Effective Java"));
    }

    @Test
    void testAddBookHappyPath() throws Exception {
        String googleId = "dOz-UK8Fl_UC";
        String mockResponse = """
                {
                  "kind": "books#volume",
                  "id": "dOz-UK8Fl_UC",
                  "etag": "test",
                  "selfLink": "https://www.googleapis.com/books/v1/volumes/dOz-UK8Fl_UC",
                  "volumeInfo": {
                    "title": "Test Title",
                    "authors": [
                      "Test Author"
                    ],
                    "publishedDate": "2023",
                    "publisher": "Publisher",
                    "pageCount": 300,
                    "printType": "BOOK",
                    "maturityRating": "NOT_MATURE",
                    "categories": [
                      "Fiction"
                    ],
                    "language": "en",
                    "previewLink": "preview",
                    "infoLink": "info"
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/books/{googleId}", googleId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(googleId))
            .andExpect(jsonPath("$.title").value("Test Title"))
            .andExpect(jsonPath("$.author").value("Test Author"))
            .andExpect(jsonPath("$.pageCount").value(300));

        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(3);
        assertThat(books.stream().anyMatch(b -> b.getId().equals(googleId))).isTrue();
    }

    @Test
    void testAddBookErrorPath() throws Exception {
        String googleId = "invalidId";
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody("Not Found"));

        mockMvc.perform(post("/books/{googleId}", googleId))
            .andExpect(status().isBadRequest());

        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(2);
    }
}
