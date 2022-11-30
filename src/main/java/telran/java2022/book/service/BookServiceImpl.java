package telran.java2022.book.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import telran.java2022.book.dao.AuthorRepository;
import telran.java2022.book.dao.BookRepository;
import telran.java2022.book.dao.PublisherRepository;
import telran.java2022.book.dto.AuthorDto;
import telran.java2022.book.dto.BookDto;
import telran.java2022.book.dto.exceptions.EntityNotFoundException;
import telran.java2022.book.model.Author;
import telran.java2022.book.model.Book;
import telran.java2022.book.model.Publisher;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    final BookRepository bookRepository;
    final AuthorRepository authorRepository;
    final PublisherRepository publisherRepository;
    final ModelMapper modelMapper;

    @Override
    @Transactional
    public boolean addBook(BookDto bookDto) {
	if (bookRepository.existsById(bookDto.getIsbn())) {
	    return false;
	}
	// Publisher
	Publisher publisher = publisherRepository.findById(bookDto.getPublisher())
		.orElse(publisherRepository.save(new Publisher(bookDto.getPublisher())));
	// Author
	Set<Author> authors = bookDto.getAuthors().stream()
		.map(a -> authorRepository.findById(a.getName())
			.orElse(authorRepository.save(new Author(a.getName(), a.getBirthDate()))))
		.collect(Collectors.toSet());
	Book book = new Book(bookDto.getIsbn(), bookDto.getTitle(), authors, publisher);
	bookRepository.save(book);
	return true;
    }

    @Override
    public BookDto findBookByIsbn(String isbn) {
	Book book = bookRepository.findById(isbn).orElseThrow(() -> new EntityNotFoundException());
	return new BookDto(isbn, book.getTitle(),
		book.getAuthors().stream().map(a -> modelMapper.map(a, AuthorDto.class)).collect(Collectors.toSet()),
		book.getPublisher().getPublisherName());
    }

    @Override
    public BookDto removeBook(String isbn) {
	Book book = bookRepository.findById(isbn).orElseThrow(() -> new EntityNotFoundException());
	BookDto bookDto = new BookDto(isbn, book.getTitle(),
		book.getAuthors().stream().map(a -> modelMapper.map(a, AuthorDto.class)).collect(Collectors.toSet()),
		book.getPublisher().getPublisherName());
	bookRepository.deleteById(isbn);
	return bookDto;
    }

    @Override
    public BookDto updateBook(String isbn, String title) {
	Book book = bookRepository.findById(isbn).orElseThrow(() -> new EntityNotFoundException());
	book.setTitle(title);
	bookRepository.save(book);
	return new BookDto(isbn, book.getTitle(),
		book.getAuthors().stream().map(a -> modelMapper.map(a, AuthorDto.class)).collect(Collectors.toSet()),
		book.getPublisher().getPublisherName());
    }

    @Override
    @Transactional(readOnly = true)
    public Iterable<BookDto> findBooksByAuthor(String authorName) {
	if (!authorRepository.existsById(authorName)) {
	    throw new EntityNotFoundException();
	}
	return bookRepository.findByAuthorsName(authorName)
		.map(b -> new BookDto(b.getIsbn(), b.getTitle(),
			b.getAuthors().stream().map(a -> modelMapper.map(a, AuthorDto.class))
				.collect(Collectors.toSet()),
			b.getPublisher().getPublisherName()))
		.collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Iterable<BookDto> findBooksByPublisher(String publisherName) {
	if (!publisherRepository.existsById(publisherName)) {
	    throw new EntityNotFoundException();
	}
	return bookRepository.findByPublisherPublisherName(publisherName)
		.map(b -> new BookDto(b.getIsbn(), b.getTitle(),
			b.getAuthors().stream().map(a -> modelMapper.map(a, AuthorDto.class))
				.collect(Collectors.toSet()),
			b.getPublisher().getPublisherName()))
		.collect(Collectors.toList());
    }

    @Override
    public Iterable<AuthorDto> findBookAuthors(String isbn) {
	Book book = bookRepository.findById(isbn).orElseThrow(() -> new EntityNotFoundException());
	return book.getAuthors().stream().map(a -> modelMapper.map(a, AuthorDto.class)).collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Iterable<String> findPublishersByAuthor(String authorName) {
	if (!authorRepository.existsById(authorName)) {
	    throw new EntityNotFoundException();
	}
	return bookRepository.findByAuthorsName(authorName).map(b -> b.getPublisher().getPublisherName())
		.collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public AuthorDto removeAuthor(String authorName) {
	if (!authorRepository.existsById(authorName)) {
	    throw new EntityNotFoundException();
	}
	bookRepository.findByAuthorsName(authorName).forEach(b -> bookRepository.delete(b));
	Author author = authorRepository.findById(authorName).orElseThrow(() -> new EntityNotFoundException());
	authorRepository.deleteById(authorName);
	return modelMapper.map(author, AuthorDto.class);
    }

}
