package holophrase

import java.io.PrintWriter

import scala.util.{Random, Try}
import holophrase1._
import java.nio.file.{Files, Paths}

sealed trait Holophrase {


  // Builds a droplet
  def run(ip: String=""): DigitalOceanDroplet = {
    this match {
      case s @ DigitalOceanServer(_, repo) =>
        val droplet = holophrase1.createDigitalOceanVM(s.fullName)
        repo.run(droplet.ip)
        droplet

      case ScalaRepo(url, running) =>
        holophrase1.installSBT(ip)
        holophrase1.cloneRepo(ip, url)
        running.run(ip)
      case SBTCommand(value) => holophrase1.runCommand(ip, value)
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
      case DigitalOceanServer(name, _) =>
        getAllDroplets
          .filter(_.name==name)
          .foreach {destroyDroplet}
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
case class DigitalOceanServer(name: String, repo: ScalaRepo) extends Holophrase {
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
case class ScalaRepo(url: String, runWith: SBTCommand) extends Holophrase
case class SBTCommand(value: String) extends Holophrase


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
