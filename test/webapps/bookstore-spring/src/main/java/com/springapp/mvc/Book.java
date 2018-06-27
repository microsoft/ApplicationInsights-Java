package com.springapp.mvc;

public final class Book {
  private final String title;
  private final String author;
  private final String isbn10;

  public Book(String title, String author, String isbn10) {
    this.title = title;
    this.author = author;
    this.isbn10 = isbn10;
  }

  public String getTitle() {
    return title;
  }

  public String getAuthor() {
    return author;
  }

  public String getIsbn10() {
    return isbn10;
  }
}
