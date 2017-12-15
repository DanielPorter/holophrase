package holophrase

import java.io.PrintWriter

import scala.util.{Random, Try}
import HolophraseFunctions._
import java.nio.file.{Files, Paths}

sealed trait HolophraseDSL {
  // Builds a droplet
  def run(ip: String=""): DigitalOceanDroplet = {
    this match {
      case s @ DigitalOceanServer(_, repo) =>
        val droplet = HolophraseFunctions.createDigitalOceanVM(s.fullName)
        repo.run(droplet.ip)
        droplet

      case ScalaRepo(url, running) =>
        HolophraseFunctions.installSBT(ip)
        HolophraseFunctions.cloneRepo(ip, url)
        running.run(ip)
      case SBTCommand(value) => HolophraseFunctions.runCommand(ip, value)
    }
  }

  // Validates parameters.
  def validate: List[String] = {
    this match {
      case DigitalOceanServer(name, repo) =>
        val res = if(name != "") Nil
        else List("Name cannot be empty")
        res ++ repo.validate

      case ScalaRepo(url, running) =>
        running.validate ++ (
          if(url.startsWith("https://github.com")) Nil
          else List("Invalid URL")
        )

      case SBTCommand(value) =>
        if(List("run", "compile", "test").contains(value)) Nil
        else List("Not a valid SBT command.")
    }
  }


  // destroys a droplet by name. Might have splash damage.
  def destroy: Unit = {
    this match {
      case droplet @ DigitalOceanServer(name, _) =>
        println(getAllDroplets
          .filter{x => println(x.name); x.name == droplet.fullName}
          .map {destroyDroplet})
      case _ => Nil
    }
  }

  def compile: List[String] = {
    this match {
      case DigitalOceanServer(name, repo) =>

        val create = s"bash createDOServer.sh $name $$keyId"
        val createAndCaptureIP =
          s"""|$$id="$$($create | python -c "import sys, json; println(json.load(sys.stdin))['droplet']['id'])")"2
             |sleep 30
             |""".stripMargin
        List(create, createAndCaptureIP) ++ repo.compile
      case ScalaRepo(url, runWith) =>
        Nil
    }
  }
}
object HolophraseDSL {

  def stopAllServers = {
    HolophraseFunctions.getAllDroplets.filter(_.name.startsWith(DigitalOceanServer.getName(""))) foreach {HolophraseFunctions.destroyDroplet}
  }
  def shell = {
    while(true) {
      println("please specify a server to build.")
      val input = io.StdIn.readLine()
      if(input == "quit") sys.exit(0)

      holophraseparser.run(input).validate match {
        case Left(error) => println(error)
        case Right(server) =>
          println(server)
          val validationResult = server.validate
          if(validationResult.isEmpty) server.run()
          else println(validationResult.mkString("\n"))
      }
    }
  }
  def runOrFail(input: String): Unit = {
    holophraseparser.run(input).validate match {
      case Left(error) => println(error)
      case Right(server) =>
        println(server)
        val validationResult = server.validate
        if(validationResult.isEmpty) server.run()
        else println(validationResult.mkString("\n"))
    }
  }

  def killOrFail(input: String): Unit = {
    holophraseparser.run(input).validate match {
      case Left(error) => println(error)
      case Right(server) =>
        println(server)
        val validationResult = server.validate
        if(validationResult.isEmpty) server.destroy
        else println(validationResult.mkString("\n"))
    }
  }
  def validate(input: String): Unit = {
    holophraseparser.run(input).validate match {
      case Left(error) => println(error)
      case Right(server) =>
        println(server)
        val validationResult = server.validate
        if(validationResult.isEmpty) ()
        else println(validationResult.mkString("\n"))
    }
  }
}
object DigitalOceanServer {

  def getOrCreateUniqueId = {
    if (!Files.exists(Paths.get("holophraseid"))) {
      new PrintWriter("holophraseid") {
        write(Random.alphanumeric.take(5).mkString); close
      }
    }
    io.Source.fromFile("holophraseid").mkString
  }
  def getName(name: String) = s"${System.getProperty("user.name")}-$getOrCreateUniqueId-$name"


  def unapply(dos: DigitalOceanServer) = Some((dos.fullName, dos.repo))


}
case class DigitalOceanServer(name: String, repo: ScalaRepo) extends HolophraseDSL {
  val fullName: String = getName(name)

  def getOrCreateUniqueId = {
    if (!Files.exists(Paths.get("holophraseid"))) {
      new PrintWriter("holophraseid") {
        write(Random.alphanumeric.take(5).mkString); close
      }
    }
    io.Source.fromFile("holophraseid").mkString
  }
  def getName(name: String) = s"${System.getProperty("user.name")}-$getOrCreateUniqueId-$name"

}
case class ScalaRepo(url: String, runWith: SBTCommand) extends HolophraseDSL
case class SBTCommand(value: String) extends HolophraseDSL


object holophrase2 {
  val server = DigitalOceanServer(
    name="myserver",
    repo=ScalaRepo(
      url="https://github.com/slamdata/matryoshka",
      runWith=SBTCommand("build")
    )
  )
  server.validate
  server.run()
}
