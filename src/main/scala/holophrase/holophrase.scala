package holophrase


import play.api.libs.json._
import sys.process._
import language.postfixOps
import scala.collection.mutable

case class Network(ip_address: String)
case class Networks(v4: List[Network])
case class DigitalOceanDroplet(id: Int, name: String, memory: Int, status: String, networks: Networks) {
  def ip = networks.v4.head.ip_address
}
case class SSHKeys(ssh_keys: List[SSHKey])
case class SSHKey(id: Int, name: String, public_key: String, fingerprint: String)

// json boiler plate
object Network {
  implicit def fmts = Json.format[Network]
}
object Networks{
  implicit def fmts = Json.format[Networks]
}
case class SSHKeyResult(ssh_key: SSHKey)

object SSHKey {
  implicit def fmts = Json.format[SSHKey]
}

object SSHKeyResult {
  implicit def fmts = Json.format[SSHKeyResult]
}


object DigitalOceanDroplet {
  implicit def fmts = Json.format[DigitalOceanDroplet]
}
object SSHKeys {
  implicit def fmts = Json.format[SSHKeys]
}

object holophrase0 {

  def main(args: Array[String]): Unit = {
    args.toList match {
      case Nil => Holophrase.shell
      case x if x=="stopall" =>
    }

  }
  def getDigitalOceanSSHKeyId(): SSHKey = {
    // if key doesn't already exist..
    "bash createHolophraseKey.sh" !

    // gets the text of the public key
    val holophrasePub = ("bash getHolophraseKey.sh" !!).dropRight(1)

    val username = ("bash getUsername.sh" !!).dropRight(1)

    val keys = Json.parse(s"""bash getDOKeys.sh""" !!).as[SSHKeys].ssh_keys.filter(_.public_key == holophrasePub)

    // if the key is already registered with DO, return the id. Otherwise, register it and return the id.
    val doKey = keys match {
      case x :: _ => println("key found!")
        x
      case _ =>
        val command = Seq("bash", "createDOKey.sh", s"""{"name":"$username","public_key":"$holophrasePub"}""")
        println(command)
        val res = command.!!
        println(res)
        val r = Json.parse(res)
        r.as[SSHKeyResult].ssh_key
    }
    doKey
  }
  def destroyAllDroplets = {
    val droplets = (Json.parse("bash listDroplets.sh"!!) \ "droplets").as[List[DigitalOceanDroplet]]
    droplets.map { x =>
      s"bash destroyDroplet.sh ${x.id}" !!
    }
  }



def createServerRunning(name: String, gitRepoOfScalaProject: String, command: String) {
  //destroyAllDroplets
  val key = getDigitalOceanSSHKeyId()
  val res = s"bash createDOServer.sh $name ${key.id}".!!
  var dropletInfo = (Json.parse(res) \ "droplet")
    .as[DigitalOceanDroplet]
  println(dropletInfo)

  while (dropletInfo.status == "new") {
    Thread.sleep(3000)
    val checkServerRes =
      s"bash checkDigitalOceanServer.sh ${dropletInfo.id}".!!
    dropletInfo = (Json.parse(checkServerRes) \ "droplet")
      .as[DigitalOceanDroplet]
  }

  // just in case..?
  Thread.sleep(15000)
  s"bash runRemoteBash.sh ${dropletInfo.ip} install_sbt.sh".!!

  s"bash runRemoteBash.sh ${dropletInfo.ip} clone_repo.sh $gitRepoOfScalaProject".!!

  s"bash runRemoteBash.sh ${dropletInfo.ip} runrepo.sh $command" !!
}
}


object Servers {
  def createDigitalOceanServer() = {

  }

  def createVM = {
    "werd"
  }

  def installSBT = {

  }
}

