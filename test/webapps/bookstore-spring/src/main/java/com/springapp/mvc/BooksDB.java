package com.springapp.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public enum BooksDB {
  INSTANCE;

  private final ConcurrentMap<String, ArrayList<BookData>> books =
      new ConcurrentHashMap<String, ArrayList<BookData>>();
  private final Map<String, BookData> isbnToBook = new HashMap<String, BookData>();

  private volatile boolean initialized = false;

  public void initialize() {
    if (!initialized) {
      synchronized (INSTANCE) {
        if (!initialized) {
          books.put("Thriller", getThrillerBooks());
          books.put("Romance", getRomanceBooks());
          books.put("Science Fiction", getScienceFictionBooks());
          books.put("Drama", getDramaBooks());
          books.put("Comedy", getComedyBooks());

          initialized = true;
        }
      }
    }
  }

  public ArrayList<BookData> getBooks(String id) {
    return books.get(id);
  }

  public void loanBook(String isbn) throws Exception {
    BookData bookData = isbnToBook.get(isbn);
    if (bookData.isLoaned()) {
      throw new Exception("Error, book already loaned");
    }

    bookData.setLoaned(true);
  }

  private ArrayList<BookData> getThrillerBooks() {
    ArrayList<BookData> b = new ArrayList<BookData>();
    b.add(new BookData(new Book("The Da Vinci Code", "Dan Brown", "0307474275")));
    b.add(new BookData(new Book("Gone Girl", "Gillian Flynn", "030758836x")));
    b.add(new BookData(new Book("The Lost Symbol", "Dan Brown", "0307950689")));
    b.add(new BookData(new Book("Digital Fortress", "Dan Brown", "0312944926")));

    for (BookData bookData : b) {
      isbnToBook.put(bookData.getBook().getIsbn10(), bookData);
    }
    return b;
  }

  private ArrayList<BookData> getRomanceBooks() {
    ArrayList<BookData> b = new ArrayList<BookData>();
    b.add(new BookData(new Book("Twilight", "Stephenie Meyer", "0316015849")));
    b.add(new BookData(new Book("Pride and Prejudice", "Jane Austen", "0486284735")));
    b.add(new BookData(new Book("Fifty Shades of Grey", "E.L. James", "0345803485")));
    b.add(new BookData(new Book("The Fault in Our Stars", "John Green", "014242417X")));

    addByIsbn(b);
    return b;
  }

  private ArrayList<BookData> getScienceFictionBooks() {
    ArrayList<BookData> b = new ArrayList<BookData>();
    b.add(new BookData(new Book("Mockingjay", "Suzanne Collins", "0545663261")));
    b.add(new BookData(new Book("Breaking Dawn", "Stephenie Meyer", "9780316067935")));
    b.add(new BookData(new Book("The Sea of Monsters", "Dan Brown", "1423103343")));
    b.add(new BookData(new Book("Stranger in a Strange Land", "Robert A. Heinlein", "0441788386")));

    addByIsbn(b);
    return b;
  }

  private ArrayList<BookData> getDramaBooks() {
    ArrayList<BookData> b = new ArrayList<BookData>();
    b.add(new BookData(new Book("Romeo and Juliet", "William Shakespeare", "0743477111")));
    b.add(new BookData(new Book("The Crucible", "Arthur Miller", "0142437336")));
    b.add(new BookData(new Book("The Tempest", "William Shakespeare", "0743482832")));
    b.add(new BookData(new Book("The Importance of Being Earnest", "Oscar Wilde", "0486264785")));

    addByIsbn(b);
    return b;
  }

  private ArrayList<BookData> getComedyBooks() {
    ArrayList<BookData> b = new ArrayList<BookData>();
    b.add(new BookData(new Book("Bossypants", "Tina Fey", "0316056898")));
    b.add(
        new BookData(
            new Book("Is Everyone Hanging Out Without Me?", "Mindy Kaling", "9780307886279")));
    b.add(new BookData(new Book("The Color of Magic", "Terry Pratchett", "0062225677")));
    b.add(new BookData(new Book("Me Talk Pretty One Day", "David Sedaris", "0316776963")));

    addByIsbn(b);
    return b;
  }

  private void addByIsbn(ArrayList<BookData> b) {
    for (BookData bookData : b) {
      isbnToBook.put(bookData.getBook().getIsbn10(), bookData);
    }
  }
}
