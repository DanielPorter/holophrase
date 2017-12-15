package holophrase
import sys.process._
import play.api.libs.json._
import language.postfixOps

object holophrase1 {

  def createHolophraseKey = createHolophraseKeyCommand !
  def createHolophraseKeyCommand = "bash createHolophraseKey.sh"

  def getHolophrasePublicKey = (getHolophrasePublicKeyCommand !!).dropRight(1)
  def getHolophrasePublicKeyCommand = "bash getHolophraseKey.sh"

  def getUsername = (getUsernameCommand !!).dropRight(1)
  def getUsernameCommand = "bash getUsername.sh"

  def getDOKeys = Json.parse(getDOKeysCommand !!).as[SSHKeys].ssh_keys
  def getDOKeysCommand = s"bash getDOKeys.sh"

  def createDOKey(name: String, key: String) = Json.parse(createDOKeyCommand(name, key) !!).as[SSHKeyResult].ssh_key
  def createDOKeyCommand(name: String, key: String) = s"""bash createDOKey.sh '{\"name\":\"holophrase$name\", \"public_key\":\"$key\"}' """ !!

  def getDigitalOceanSSHKey(): SSHKey = {
    // if key doesn't already exist..
    createHolophraseKey

    // gets the text of the public key
    val holophrasePub = getHolophrasePublicKey

    val keys = getDOKeys.filter(_.public_key == holophrasePub)

    // if the key is already registered with DO, return the id. Otherwise, register it and return the id.
    val doKey = keys match {
      case x :: _ => x
      case _ =>
        createDOKey("holophrase" + getUsername, holophrasePub)
    }
    doKey
  }

  def createDigitalOceanVMCommand(name: String, keyId: Int) = {
    s"bash createDOServer.sh $name $keyId"
  }

    def createDigitalOceanVM(name: String) = {
    println("creating vm")
    val key = getDigitalOceanSSHKey()
    val res = createDigitalOceanVMCommand(name, key.id) !!

    var dropletInfo = (Json.parse(res) \ "droplet").as[DigitalOceanDroplet]

    // poll while VM spins up
    while (dropletInfo.status == "new") {
      println("polling")
      Thread.sleep(3000)
      val checkServerRes = s"bash checkDigitalOceanServer.sh ${dropletInfo.id}" !!

      dropletInfo = (Json.parse(checkServerRes) \ "droplet").as[DigitalOceanDroplet]
    }
    // wait a little more, just in case
    Thread.sleep(10000)
    println("vm probably ready")

    dropletInfo
  }

  def installSBT(serverIP: String) = {
    println("installing sbt")
    s"bash runRemoteBash.sh $serverIP install_sbt.sh".!(ProcessLogger(println(_)))
    getDropletByIp(serverIP)
  }

  def cloneRepo(serverIP: String, repo: String) = {
    println(cloneRepoCommand(serverIP, repo) !!)
    getDropletByIp(serverIP)
  }

  def cloneRepoCommand(serverIP: String, repo: String) = {
    s"bash runRemoteBash.sh $serverIP clone_repo.sh $repo"
  }

  def runCommand(serverIP: String, command: String) = {
    println("running  sbt command!")
    s"bash runRemoteBash.sh $serverIP runrepo.sh $command".!!
    getDropletByIp(serverIP)
  }

  def getAllDroplets: List[DigitalOceanDroplet] = {
    (Json.parse("bash listDroplets.sh"!!) \ "droplets").as[List[DigitalOceanDroplet]]
  }

  def getDropletByIp(ip: String): DigitalOceanDroplet = {
    getAllDroplets.filter(_.ip == ip).head
  }
  def destroyDroplet(droplet: DigitalOceanDroplet) = {
    s"bash destroyDroplet.sh ${droplet.id}" !!
  }

  def createNamedServerRunning(
      name: String,
      gitRepoOfScalaProject: String,
      command: String) = {

    val dropletInfo = createDigitalOceanVM(name)
    installSBT(dropletInfo.ip)
    cloneRepo(dropletInfo.ip, gitRepoOfScalaProject)
    runCommand(dropletInfo.ip, command)
  }
}
