package com.springapp.mvc;

public class BookData {
  private final Book book;
  private boolean loaned;

  public BookData(Book book) {
    this.book = book;
    this.loaned = false;
  }

  public Book getBook() {
    return book;
  }

  public boolean isLoaned() {
    return loaned;
  }

  public void setLoaned(boolean loaned) {
    this.loaned = loaned;
  }
}
