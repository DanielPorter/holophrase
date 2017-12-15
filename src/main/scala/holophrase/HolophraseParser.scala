package holophrase
import scala.util.parsing.combinator._
import scala.util.parsing.input.Positional

// invalid
// "Server(name=\"myserver\", repost=\"https://github.com/slamdata/matryoshka\", command=\"build\")"
// valid
// "Server(name=\"myserver\", repo=\"https://github.com/slamdata/matryoshka.git\", command=\"compile\")"
// Server(name="myserver", repo="https://github.com/slamdata/matryoshka.git", command="compile")

sealed trait HolophraseAST extends Positional {
  val validIdentifiers = Set(
    "name",
    "repo",
    "command")

  def validate: Either[String, DigitalOceanServer] = {
    this match {
      case Server(assignments) =>
        val invalidAssignments = assignments.filter{ assignment => !validIdentifiers.contains(assignment.id.name)}
        if(invalidAssignments.isEmpty) {
          val assignmentsMap = assignments.map { a => a.id.name -> a.value.value }.toMap
          Right(
            DigitalOceanServer(
              assignmentsMap.getOrElse("name", "mydroplet"),
              ScalaRepo(
                assignmentsMap.getOrElse("repo", "https://github.com/danielporter/holophrase"),
                SBTCommand(assignmentsMap.getOrElse("command", "run"))
              )
            )
          )
        } else {
          Left(invalidAssignments.map { x=> s"${x.id.name} at position ${x.pos} is an invalid attribute."} mkString(" "))
        }
      case _ => Left("Invalid program. Must be in the form of Server(attribute=\"stringliteral\")")
    }
  }
}

final case class Identifier(name: String) extends HolophraseAST
final case class StringLiteral(value: String) extends HolophraseAST
final case class Assignment(id: Identifier, value: StringLiteral) extends HolophraseAST
final case class Server(assignments: List[Assignment]) extends HolophraseAST


final case class UnterminatedString(value: String) extends HolophraseAST
final case class LPAREN() extends HolophraseAST
final case class RPAREN() extends HolophraseAST
final case class EQUALS() extends HolophraseAST
final case class COMMA() extends HolophraseAST
final case class UnrecognizedCharacter(char: Char) extends HolophraseAST
final case class TokensSequence(tokens: List[HolophraseAST]) extends HolophraseAST




object holophrasedemoparser extends RegexParsers {
  def equals = "="

  def stringLiteral = "\".*?\"".r ^^ {literal =>
    //lop off the captured quotes..
    literal.drop(1).dropRight(1)
  }

  def comma = ","
  def name = "name" ~> equals ~> stringLiteral
  def repo = "repo" ~> equals ~> stringLiteral
  def command = "command" ~> equals ~> stringLiteral
  def server = "Server"
  def program = server ~> "(" ~> name ~ comma ~ repo ~ comma ~ command <~ ")" ^^ { case name ~ _ ~ repo ~ _ ~ command =>
    DigitalOceanServer(name, ScalaRepo(repo, SBTCommand(command)))
  }

  def run(toParse: String) = {
    this.parse(program, toParse) match {
      case Success(res, _) => res
    }
  }
}
object holophraseparser extends RegexParsers {

  val identifier = positioned {"[a-zA-Z]+".r ^^ Identifier}
  def stringLiteral = "\".*?\"".r ^^ {literal =>
    //lop off the captured quotes..
    StringLiteral(literal.drop(1).dropRight(1))
  }
  val unterminatedString = positioned {"\".*?$".r ^^ {x => UnterminatedString(x)}}
  val equals: Parser[EQUALS] = positioned { "=" ^^ { _ => EQUALS()}}

  val lparen: Parser[LPAREN] = positioned {"(" ^^ {_ => LPAREN()}}
  val rparen: Parser[RPAREN] = positioned {")" ^^ {_ => RPAREN()}}

  val assignment: Parser[Assignment] = positioned {(identifier <~ equals) ~ stringLiteral ^^ {case id ~ literal => Assignment(id, literal)}}
  val assignmentSequence: Parser[List[Assignment]] = lparen ~> repsep(assignment, ",") <~ rparen

  val server =  positioned {"Server" ~> assignmentSequence ^^ {assignments => Server(assignments)} }

  val anything = ".".r ^^ {char: String => UnrecognizedCharacter(char.charAt(0))}

  val program: Parser[HolophraseAST] = server | (
    rep(identifier |
      stringLiteral |
      unterminatedString |
      equals |
      lparen |
      rparen |
      assignment |
      anything ) ^^ {TokensSequence})

  def run(toParse: String) = {
    this.parse(program, toParse) match {
      case Success(res, next) => res
    }
  }
}
