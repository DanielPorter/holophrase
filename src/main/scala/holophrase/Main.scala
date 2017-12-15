package holophrase

import java.nio.file.{Files, Paths}

object Main {
  def main(args: Array[String]): Unit = {
    println("starting holophrase...")
    args.toList match {
      case Nil => HolophraseDSL.shell
      case x::Nil if x.toLowerCase == "stopall".toLowerCase => HolophraseDSL.stopAllServers
      case x::Nil if x.toLowerCase == "killeverything" => holophrase0.destroyAllDroplets
      case x::y::Nil if x == "start" => if(Files.exists(Paths.get(y))) {
        HolophraseDSL.runOrFail(io.Source.fromFile(y).mkString)
      }
      case x::y::Nil if x == "stop" => if(Files.exists(Paths.get(y))) {
        println("killing vm??")
        HolophraseDSL.killOrFail(io.Source.fromFile(y).mkString)
      }
      case x::y::Nil if x == "validate" => if(Files.exists(Paths.get(y))) {
        HolophraseDSL.validate(io.Source.fromFile(y).mkString)
      }
    }
  }
}
