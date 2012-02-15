package com.ahalmeida.tubaina.parser

import util.parsing.combinator._
import java.lang.RuntimeException
import br.com.caelum.tubaina.chunk._
import br.com.caelum.tubaina.Chunk
import scala.collection.JavaConversions._
import br.com.caelum.tubaina.Chapter
import br.com.caelum.tubaina.Section
import br.com.caelum.tubaina.Book

class TubainaParser(bookName:String) extends JavaTokenParsers {

  def document:Parser[Book] = (chapter+) ^^ {
    case list => new Book(bookName, list, false)
  }

  def nonBracket:Parser[String] = "(?ms)[^\\[\\]]+".r ^^ (x => x.trim())

  def chapter:Parser[Chapter] =
    p("[chapter " ~> nonBracket <~ "]") ~ (content?) ~ (section*) ^^ {
      case name ~ Some(intro) ~ sections => new Chapter(name, new IntroductionChunk(intro), sections, Seq())
      case name ~ None ~ sections => new Chapter(name, new IntroductionChunk(Seq()), sections, Seq())
    }

  def exercises:Parser[ExerciseChunk] = "[exercises]" ~> (question+) <~ "[/exercises]" ^^ {
    case questions => new ExerciseChunk(questions)
  }
  
  def question:Parser[QuestionChunk] = "[question]" ~> content ~ (answer?) <~ "[/question]" ^^ {
    case content ~ answer => new QuestionChunk(content ++ answer)
  }
  
  def answer:Parser[AnswerChunk] = "[answer]" ~> content <~ "[/answer]" ^^ {
    case content => new AnswerChunk(content)
  }

//  def bold:Parser[Bold] = "**" ~> paragraph <~ "**" ^^ {p => Bold(p)}
//  def em:Parser[Em] = "::" ~> paragraph <~ "::" ^^ {p => Em(p)}
//  def und:Parser[Und] = "__" ~> paragraph <~ "__" ^^ {p => Und(p)}
//  def mono:Parser[Mono] = "%%" ~> paragraph <~ "%%" ^^ {p => Mono(p)}
  def text:Parser[String] = "[^\\[]+".r ^^ {x => x.trim()}

//  def textElem:Parser[TextElement] = bold | em | und | mono | text
  
  def paragraph:Parser[ParagraphChunk] = (text) ^^ { x => new ParagraphChunk(x) }


  def code:Parser[CodeChunk] = p("[code " ~> nonBracket <~"]" | "[code]" ~> "") ~ nonBracket <~ "[/code]" ^^ {
    case opts ~ content => new CodeChunk("\n" + content + "\n", opts) 
  }
  def box:Parser[BoxChunk] = p("[box " ~> nonBracket <~ "]") ~ content <~ "[/box]" ^^ {
    case title ~ chunks => new BoxChunk(title, chunks)
  }
  def java:Parser[JavaChunk] = p("[java " ~> nonBracket <~"]" | "[java]" ~> "") ~ nonBracket <~ "[/java]" ^^ {
    case opts ~ content => new JavaChunk(opts, "\n" + content + "\n")
  }
  
  def ruby:Parser[RubyChunk] = p("[ruby " ~> nonBracket <~"]" | "[ruby]" ~> "") ~ nonBracket <~ "[/ruby]" ^^ {
    case opts ~ content => new RubyChunk("\n" + content + "\n", opts)
  }

  def note:Parser[NoteChunk] = "[note]" ~> content <~ "[/note]" ^^ (x => new NoteChunk(Seq(), x))
  
  def item:Parser[ItemChunk] = "^\\s*\\* ".r ~> "(.(?!^\\s*\\*|\\[/list\\]))+.".r ^^ {
    x => new ItemChunk(parse(content, x).get)
  }
  
  def list:Parser[ListChunk] = "[list]" ~> (item+) <~ "[/list]" ^^ (x => new ListChunk("", x))

  def elem:Parser[Chunk] = list | code | java | ruby | paragraph | box | note | exercises
  
  def content:Parser[Seq[Chunk]] = elem+

  def section:Parser[Section] =
    p("[section " ~> nonBracket <~ "]") ~ (content?) ^^ {
      case name ~ Some(content) => new Section(name, content)
      case name ~ None => new Section(name, Seq())
    }

  def p(s:Parser[String]) = s

  def faz(toParse:String) = {
    parseAll(document, toParse) match {
      case Success(r, q) => r
      case NoSuccess(message, input) => message match {
        case _ => throw new RuntimeException(message)
      }
    }
  }
}
